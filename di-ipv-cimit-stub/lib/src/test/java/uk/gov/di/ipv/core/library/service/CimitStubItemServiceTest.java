package uk.gov.di.ipv.core.library.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.library.persistence.DataStore;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.core.library.config.ConfigurationVariable.CIMIT_STUB_TTL;

@ExtendWith(MockitoExtension.class)
class CimitStubItemServiceTest {

    private static final String USER_ID = "user-id-1";
    private static final String DB_TTL = "1800";
    @Captor private ArgumentCaptor<CimitStubItem> cimitStubItemArgumentCaptor;
    @Mock private ConfigService mockConfigService;
    @Mock private DataStore<CimitStubItem> mockDataStore;
    @InjectMocks private CimitStubItemService cimitStubItemService;

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

        var result = cimitStubItemService.getCIsForUserId(USER_ID);

        assertTrue(
                result.stream()
                        .map(CimitStubItem::getContraIndicatorCode)
                        .anyMatch(ciCode::equals));
    }

    @Test
    void shouldCreateCimitStubItem() {
        String ciCode = "V03";
        List<String> mitigations = List.of("V01", "V03");
        Instant issuanceDate = Instant.now();
        List<String> issuers = List.of("https://address-cri.stubs.account.gov.uk");
        String docId = "some/document/id";

        cimitStubItemService.persistCimitStubItem(
                CimitStubItem.builder()
                        .userId(USER_ID)
                        .contraIndicatorCode(ciCode)
                        .issuers(issuers)
                        .issuanceDate(issuanceDate)
                        .mitigations(mitigations)
                        .documentIdentifier(docId)
                        .build());

        verify(mockDataStore).create(cimitStubItemArgumentCaptor.capture(), any());

        CimitStubItem capturedItem = cimitStubItemArgumentCaptor.getValue();

        assertEquals(USER_ID, capturedItem.getUserId());
        assertEquals(ciCode, capturedItem.getContraIndicatorCode());
        assertEquals(issuers, capturedItem.getIssuers());
        assertEquals(issuanceDate, capturedItem.getIssuanceDate());
        assertEquals(mitigations, capturedItem.getMitigations());
        assertEquals(docId, capturedItem.getDocumentIdentifier());
    }

    @Test
    void persistCimitStubItemShouldCreateAnItem() {
        CimitStubItem itemToPersist = CimitStubItem.builder().userId("some-user").build();

        cimitStubItemService.persistCimitStubItem(itemToPersist);

        verify(mockDataStore).create(cimitStubItemArgumentCaptor.capture(), eq(CIMIT_STUB_TTL));
        assertEquals(itemToPersist, cimitStubItemArgumentCaptor.getValue());
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
        cimitStubItemService.updateCimitStubItem(cimitStubItem);
        verify(mockDataStore, times(1)).update(any());
    }

    @Test
    void shouldDeleteCimitStubItem() {
        String ciCode = "D02";
        cimitStubItemService.deleteCimitStubItem(USER_ID, ciCode);
        verify(mockDataStore, times(1)).delete(any(), any());
    }
}
