package uk.gov.di.ipv.core.putcontraindicators.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.library.persistence.DataStore;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.service.CimitStubItemService;
import uk.gov.di.ipv.core.library.service.ConfigService;
import uk.gov.di.ipv.core.putcontraindicators.domain.PutContraIndicatorsRequest;
import uk.gov.di.ipv.core.putcontraindicators.exceptions.CiPutException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.core.library.config.ConfigurationVariable.CIMIT_STUB_TTL;

@ExtendWith(MockitoExtension.class)
public class ContraIndicatorsServiceTest {
    @Mock private DataStore<CimitStubItem> mockDataStore;

    @Mock private ConfigService mockConfigService;

    @InjectMocks private CimitStubItemService cimitStubItemService;

    public static final String USER_ID = "a-user-id";
    private static final String SIGNED_CONTRA_INDICATOR_VC =
            "eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL2lkZW50aXR5LnN0YWdpbmcuYWNjb3VudC5nb3YudWsiLCJpYXQiOjE2ODk5NDMxNjksIm5iZiI6MTY4OTk0MzE2OCwiZXhwIjoyMDA1MzAzMTY4LCJzdWIiOiJhLXVzZXItaWQiLCJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiU2VjdXJpdHlDaGVja0NyZWRlbnRpYWwiXSwiZXZpZGVuY2UiOlt7InR5cGUiOiJTZWN1cml0eUNoZWNrIiwiY2kiOlt7ImNvZGUiOiJEMDEiLCJpc3N1YW5jZURhdGUiOiIyMDIyLTA5LTIwVDE1OjU0OjUwLjAwMFoiLCJkb2N1bWVudCI6InBhc3Nwb3J0L0dCUi84MjQxNTkxMjEiLCJ0eG4iOlsiYWJjZGVmIl0sIm1pdGlnYXRpb24iOlt7ImNvZGUiOiJNMDEiLCJtaXRpZ2F0aW5nQ3JlZGVudGlhbCI6W3siaXNzdWVyIjoiaHR0cHM6Ly9jcmVkZW50aWFsLWlzc3Vlci5leGFtcGxlLyIsInZhbGlkRnJvbSI6IjIwMjItMDktMjFUMTU6NTQ6NTAuMDAwWiIsInR4biI6ImdoaWoiLCJpZCI6InVybjp1dWlkOmY4MWQ0ZmFlLTdkZWMtMTFkMC1hNzY1LTAwYTBjOTFlNmJmNiJ9XX1dLCJpbmNvbXBsZXRlTWl0aWdhdGlvbiI6W3siY29kZSI6Ik0wMiIsIm1pdGlnYXRpbmdDcmVkZW50aWFsIjpbeyJpc3N1ZXIiOiJodHRwczovL2Fub3RoZXItY3JlZGVudGlhbC1pc3N1ZXIuZXhhbXBsZS8iLCJ2YWxpZEZyb20iOiIyMDIyLTA5LTIyVDE1OjU0OjUwLjAwMFoiLCJ0eG4iOiJjZGVlZiIsImlkIjoidXJuOnV1aWQ6ZjVjOWZmNDAtMWRjZC00YThiLWJmOTItOTQ1NjA0N2MxMzJmIn1dfV19XSwidHhuIjpbImZrZmtkIl19XX19.r7uH3C8wrRf3GVcJaHkBQWZGjFzvB3Yx8d85wNFkM7Q_k30fkLj4Si3viByAYObo36LkPc_NZH5OXa_5vYjFxw";
    private static final String SIGNED_CONTRA_INDICATOR_VC_NO_EVIDENCE =
            "eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL2lkZW50aXR5LnN0YWdpbmcuYWNjb3VudC5nb3YudWsiLCJpYXQiOjE2ODgxMjM0NjYsIm5iZiI6MTY4ODEyMzQ2NiwiZXhwIjoyMDAzNDgzNDY2LCJzdWIiOiJhLXVzZXItaWQiLCJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiU2VjdXJpdHlDaGVja0NyZWRlbnRpYWwiXSwiZXZpZGVuY2UiOltdfX0.licS4NM0EWKQm6fYT1plBQV6Bk4e9qrdXQ1NOo-GIvmTUhPbRSXHdUvGHUNbnVFxFZMyxdtBM_lkEUfqTpY64A";

    private static final String SIGNED_CONTRA_INDICATOR_VC_INVALID_EVIDENCE =
            "eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL2lkZW50aXR5LnN0YWdpbmcuYWNjb3VudC5nb3YudWsiLCJpYXQiOjE2ODgxMjUwNDAsIm5iZiI6MTY4ODEyNTAzOSwiZXhwIjoyMDAzNDg1MDM5LCJzdWIiOiJhLXVzZXItaWQiLCJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiU2VjdXJpdHlDaGVja0NyZWRlbnRpYWwiXSwiZXZpZGVuY2UiOlt7InR5cGUiOiJTZWN1cml0eUNoZWNrIiwibm90QUNvbnRyYUluZGljYXRvciI6W3siY29kZSI6IkQwMSIsImlzc3VhbmNlRGF0ZSI6IjIwMjItMDktMjBUMTU6NTQ6NTAuMDAwWiIsImRvY3VtZW50IjoicGFzc3BvcnQvR0JSLzgyNDE1OTEyMSIsInR4biI6WyJhYmNkZWYiXSwibWl0aWdhdGlvbiI6W3siY29kZSI6Ik0wMSIsIm1pdGlnYXRpbmdDcmVkZW50aWFsIjpbeyJpc3N1ZXIiOiJodHRwczovL2NyZWRlbnRpYWwtaXNzdWVyLmV4YW1wbGUvIiwidmFsaWRGcm9tIjoiMjAyMi0wOS0yMVQxNTo1NDo1MC4wMDBaIiwidHhuIjoiZ2hpaiIsImlkIjoidXJuOnV1aWQ6ZjgxZDRmYWUtN2RlYy0xMWQwLWE3NjUtMDBhMGM5MWU2YmY2In1dfV0sImluY29tcGxldGVNaXRpZ2F0aW9uIjpbeyJjb2RlIjoiTTAyIiwibWl0aWdhdGluZ0NyZWRlbnRpYWwiOlt7Imlzc3VlciI6Imh0dHBzOi8vYW5vdGhlci1jcmVkZW50aWFsLWlzc3Vlci5leGFtcGxlLyIsInZhbGlkRnJvbSI6IjIwMjItMDktMjJUMTU6NTQ6NTAuMDAwWiIsInR4biI6ImNkZWVmIiwiaWQiOiJ1cm46dXVpZDpmNWM5ZmY0MC0xZGNkLTRhOGItYmY5Mi05NDU2MDQ3YzEzMmYifV19XX1dfV19fQ._2dCakEuFyF861YIxn7XJvBs03vbmPfX3H51YuUyn53sFDKJPZZzgAN_qMIphEfTlUMxclKtCu0b_ycseW3bFQ";

    private static final String DB_TTL = "1800";

    @Test
    public void addUserCisShouldInsert() {
        String ci = "C01";
        PutContraIndicatorsRequest putContraIndicatorsRequest =
                PutContraIndicatorsRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .signedJwt(SIGNED_CONTRA_INDICATOR_VC)
                        .build();

        CimitStubItem existingCimitStubItem =
                CimitStubItem.builder()
                        .userId(USER_ID)
                        .contraIndicatorCode(ci)
                        .issuanceDate(Instant.now())
                        .mitigations(Collections.emptyList())
                        .build();

        when(mockDataStore.getItems(USER_ID))
                .thenReturn(Collections.singletonList(existingCimitStubItem));

        ContraIndicatorsService cimitService =
                new ContraIndicatorsService(mockConfigService, cimitStubItemService);
        cimitService.addUserCis(putContraIndicatorsRequest);

        verify(mockDataStore, times(1)).getItems(any());
        verify(mockDataStore, times(1)).create(any(), eq(CIMIT_STUB_TTL));
    }

    @Test
    public void addUserCisShouldInsertAndUpdated() {
        PutContraIndicatorsRequest putContraIndicatorsRequest =
                PutContraIndicatorsRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .signedJwt(SIGNED_CONTRA_INDICATOR_VC)
                        .build();

        CimitStubItem existingCimitStubItem =
                CimitStubItem.builder()
                        .userId(USER_ID)
                        .contraIndicatorCode("D01")
                        .issuanceDate(Instant.now())
                        .mitigations(List.of("V01", "V02"))
                        .ttl(1800)
                        .build();

        when(mockConfigService.getSsmParameter(CIMIT_STUB_TTL)).thenReturn(DB_TTL);
        when(mockDataStore.getItems(USER_ID))
                .thenReturn(Collections.singletonList(existingCimitStubItem));

        ContraIndicatorsService cimitService =
                new ContraIndicatorsService(mockConfigService, cimitStubItemService);
        cimitService.addUserCis(putContraIndicatorsRequest);

        verify(mockDataStore, times(1)).getItems(any());
        verify(mockDataStore, never()).create(any(), eq(CIMIT_STUB_TTL));
        verify(mockDataStore, times(1)).update(any());
    }

    @Test
    public void addUserCisShouldFailedIfNoEvidence() {
        ContraIndicatorsService cimitService =
                new ContraIndicatorsService(mockConfigService, cimitStubItemService);
        PutContraIndicatorsRequest putContraIndicatorsRequest =
                PutContraIndicatorsRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .signedJwt(SIGNED_CONTRA_INDICATOR_VC_NO_EVIDENCE)
                        .build();

        assertThrows(
                CiPutException.class, () -> cimitService.addUserCis(putContraIndicatorsRequest));

        verify(mockDataStore, never()).getItems(any());
        verify(mockDataStore, never()).create(any(), eq(CIMIT_STUB_TTL));
    }

    @Test
    public void addUserCisShouldFailedIfJWTPArsingFails() {
        ContraIndicatorsService cimitService =
                new ContraIndicatorsService(mockConfigService, cimitStubItemService);
        PutContraIndicatorsRequest putContraIndicatorsRequest =
                PutContraIndicatorsRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .signedJwt("invalid_jwt")
                        .build();

        assertThrows(
                CiPutException.class, () -> cimitService.addUserCis(putContraIndicatorsRequest));

        verify(mockDataStore, never()).getItems(any());
        verify(mockDataStore, never()).create(any(), eq(CIMIT_STUB_TTL));
    }

    @Test
    public void addUserCisShouldFailedIfInvalidEvidence() {
        ContraIndicatorsService cimitService =
                new ContraIndicatorsService(mockConfigService, cimitStubItemService);
        PutContraIndicatorsRequest putContraIndicatorsRequest =
                PutContraIndicatorsRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .signedJwt(SIGNED_CONTRA_INDICATOR_VC_INVALID_EVIDENCE)
                        .build();

        assertThrows(
                CiPutException.class, () -> cimitService.addUserCis(putContraIndicatorsRequest));

        verify(mockDataStore, never()).getItems(any());
        verify(mockDataStore, never()).create(any(), eq(CIMIT_STUB_TTL));
    }
}
