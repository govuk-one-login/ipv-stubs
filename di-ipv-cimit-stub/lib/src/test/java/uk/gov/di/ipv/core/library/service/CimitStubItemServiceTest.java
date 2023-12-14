package uk.gov.di.ipv.core.library.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.library.persistence.DataStore;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.core.library.config.ConfigurationVariable.CIMIT_STUB_TTL;

@ExtendWith(MockitoExtension.class)
class CimitStubItemServiceTest {

    private static final String USER_ID = "user-id-1";
    private static final String DB_TTL = "1800";
    @Mock private ConfigService mockConfigService;

    @Mock private DataStore<CimitStubItem> mockDataStore;

    private CimitStubItemService classToTest;

    @BeforeEach
    void setUp() {
        classToTest = new CimitStubItemService(mockDataStore, mockConfigService);
    }

    @Test
    void shouldReturnCredentialIssuersFromDataStoreForSpecificUserId() {
        String ciCode = "V03";
        List<CimitStubItem> cimitStubItems =
                List.of(
                        CimitStubItem.builder()
                                .userId(USER_ID)
                                .contraIndicatorCode(ciCode)
                                .build());

        when(mockDataStore.getItems(USER_ID)).thenReturn(cimitStubItems);

        var result = classToTest.getCIsForUserId(USER_ID);

        assertTrue(
                result.stream()
                        .map(CimitStubItem::getContraIndicatorCode)
                        .anyMatch(item -> ciCode.equals(item)));
    }

    @Test
    void shouldCreateCimitStubItem() {
        String ciCode = "V03";
        List<String> mitigations = List.of("V01", "V03");
        Instant issuanceDate = Instant.now();
        List<String> issuer = List.of("https://address-cri.stubs.account.gov.uk");

        CimitStubItem cimitStubItem =
                classToTest.persistCimitStub(USER_ID, ciCode, issuer, issuanceDate, mitigations);

        ArgumentCaptor<CimitStubItem> cimitStubItemArgumentCaptor =
                ArgumentCaptor.forClass(CimitStubItem.class);
        verify(mockDataStore).create(cimitStubItemArgumentCaptor.capture(), any());

        assertEquals(USER_ID, cimitStubItem.getUserId());
        assertEquals(ciCode, cimitStubItem.getContraIndicatorCode());
        assertEquals(issuanceDate, cimitStubItem.getIssuanceDate());
        assertEquals(mitigations, cimitStubItem.getMitigations());
    }

    @Test
    void shouldUpdateCimitStubItem() {
        CimitStubItem cimitStubItem =
                CimitStubItem.builder()
                        .userId(USER_ID)
                        .contraIndicatorCode("D01")
                        .mitigations(List.of("V01", "V03"))
                        .issuanceDate(Instant.now())
                        .build();
        when(mockConfigService.getSsmParameter(CIMIT_STUB_TTL)).thenReturn(DB_TTL);
        classToTest.updateCimitStubItem(cimitStubItem);
        verify(mockDataStore, times(1)).update(any());
    }

    @Test
    void shouldDeleteCimitStubItem() {
        String ciCode = "D02";
        classToTest.deleteCimitStubItem(USER_ID, ciCode);
        verify(mockDataStore, times(1)).delete(any(), any());
    }
}
