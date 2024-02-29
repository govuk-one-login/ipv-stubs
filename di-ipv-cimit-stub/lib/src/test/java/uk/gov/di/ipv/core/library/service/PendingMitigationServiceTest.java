package uk.gov.di.ipv.core.library.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.library.model.UserMitigationRequest;
import uk.gov.di.ipv.core.library.persistence.DataStore;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.persistence.items.PendingMitigationItem;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.core.library.config.ConfigurationVariable.CIMIT_STUB_TTL;

@ExtendWith(MockitoExtension.class)
class PendingMitigationServiceTest {
    @Mock private DataStore<PendingMitigationItem> mockDataStore;
    @Mock private CimitStubItemService mockCimitStubItemService;
    @Captor private ArgumentCaptor<CimitStubItem> cimitItemCaptor;

    private PendingMitigationItem pendingMitigationItem;
    private CimitStubItem cimitStubItem;
    private PendingMitigationService service;

    @BeforeEach
    void setUp() {
        pendingMitigationItem = new PendingMitigationItem();
        pendingMitigationItem.setVcJti("aJwtId");
        pendingMitigationItem.setMitigatedCi("CI");
        pendingMitigationItem.setMitigationCodes(List.of("M01", "M02"));

        cimitStubItem =
                CimitStubItem.builder()
                        .userId("aUserId")
                        .contraIndicatorCode("CI")
                        .issuanceDate(Instant.now())
                        .build();

        service = new PendingMitigationService(mockDataStore);
    }

    @Test
    void persistPendingMitigationShouldCreateNewItem() {
        UserMitigationRequest request =
                UserMitigationRequest.builder()
                        .mitigations(List.of("M01", "M02"))
                        .vcJti("someRandomId")
                        .build();

        service.persistPendingMitigation(request, "CI", "POST");

        var expectedItem = new PendingMitigationItem();
        expectedItem.setVcJti("someRandomId");
        expectedItem.setMitigatedCi("CI");
        expectedItem.setMitigationCodes(List.of("M01", "M02"));
        expectedItem.setRequestMethod("POST");

        verify(mockDataStore).create(expectedItem, CIMIT_STUB_TTL);
    }

    @Test
    void completePendingMitigationShouldUpdateCimitItemPost() {
        pendingMitigationItem.setRequestMethod("POST");

        when(mockDataStore.getItem("aJwtId", false)).thenReturn(pendingMitigationItem);
        when(mockCimitStubItemService.getCiForUserId("aUserId", "CI"))
                .thenReturn(List.of(cimitStubItem));

        service.completePendingMitigation("aJwtId", "aUserId", mockCimitStubItemService);

        verify(mockCimitStubItemService).updateCimitStubItem(cimitItemCaptor.capture());
        assertEquals(List.of("M01", "M02"), cimitItemCaptor.getValue().getMitigations());
    }

    @Test
    void completePendingMitigationShouldUpdateCimitItemWithExistingMitigationsPost() {
        pendingMitigationItem.setRequestMethod("POST");
        cimitStubItem.setMitigations(List.of("M03"));

        when(mockDataStore.getItem("aJwtId", false)).thenReturn(pendingMitigationItem);
        when(mockCimitStubItemService.getCiForUserId("aUserId", "CI"))
                .thenReturn(List.of(cimitStubItem));

        service.completePendingMitigation("aJwtId", "aUserId", mockCimitStubItemService);

        verify(mockCimitStubItemService).updateCimitStubItem(cimitItemCaptor.capture());
        assertEquals(List.of("M01", "M02", "M03"), cimitItemCaptor.getValue().getMitigations());
    }

    @Test
    void completePendingMitigationShouldUpdateCimitItemPut() {
        pendingMitigationItem.setRequestMethod("PUT");

        when(mockDataStore.getItem("aJwtId", false)).thenReturn(pendingMitigationItem);
        when(mockCimitStubItemService.getCiForUserId("aUserId", "CI"))
                .thenReturn(List.of(cimitStubItem));

        service.completePendingMitigation("aJwtId", "aUserId", mockCimitStubItemService);

        verify(mockCimitStubItemService).updateCimitStubItem(cimitItemCaptor.capture());
        assertEquals(List.of("M01", "M02"), cimitItemCaptor.getValue().getMitigations());
    }

    @Test
    void completePendingMitigationShouldUpdateCimitItemWithExistingMitigationsPut() {
        pendingMitigationItem.setRequestMethod("PUT");
        cimitStubItem.setMitigations(List.of("M03"));

        when(mockDataStore.getItem("aJwtId", false)).thenReturn(pendingMitigationItem);
        when(mockCimitStubItemService.getCiForUserId("aUserId", "CI"))
                .thenReturn(List.of(cimitStubItem));

        service.completePendingMitigation("aJwtId", "aUserId", mockCimitStubItemService);

        verify(mockCimitStubItemService).updateCimitStubItem(cimitItemCaptor.capture());
        assertEquals(List.of("M01", "M02"), cimitItemCaptor.getValue().getMitigations());
    }

    @Test
    void completePendingMitigationShouldDoNothingIfNoPendingMitigationFound() {
        when(mockDataStore.getItem("aJwtId", false)).thenReturn(null);

        service.completePendingMitigation("aJwtId", "aUserId", mockCimitStubItemService);

        verifyNoInteractions(mockCimitStubItemService);
    }

    @Test
    void completePendingMitigationShouldDoNothingIfNoCiFound() {
        when(mockDataStore.getItem("aJwtId", false)).thenReturn(pendingMitigationItem);
        when(mockCimitStubItemService.getCiForUserId("aUserId", "CI")).thenReturn(null);

        service.completePendingMitigation("aJwtId", "aUserId", mockCimitStubItemService);

        verify(mockCimitStubItemService, never()).updateCimitStubItem(any());
    }

    @Test
    void completePendingMitigationShouldThrowIfUnsupportedMethod() {
        pendingMitigationItem.setRequestMethod("DELETE");

        when(mockDataStore.getItem("aJwtId", false)).thenReturn(pendingMitigationItem);
        when(mockCimitStubItemService.getCiForUserId("aUserId", "CI"))
                .thenReturn(List.of(cimitStubItem));

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    service.completePendingMitigation(
                            "aJwtId", "aUserId", mockCimitStubItemService);
                });
    }
}
