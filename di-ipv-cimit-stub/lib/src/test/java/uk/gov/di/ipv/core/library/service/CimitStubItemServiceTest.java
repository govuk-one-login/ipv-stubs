package uk.gov.di.ipv.core.library.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.library.persistence.DataStore;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CimitStubItemServiceTest {

    private static final String USER_ID = "user-id-1";
    private static final ObjectMapper objectMapper = new ObjectMapper();

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
}
