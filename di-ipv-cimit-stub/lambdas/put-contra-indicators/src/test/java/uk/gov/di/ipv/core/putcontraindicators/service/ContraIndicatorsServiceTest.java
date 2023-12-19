package uk.gov.di.ipv.core.putcontraindicators.service;

import com.nimbusds.jwt.SignedJWT;
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

import java.text.ParseException;
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

    public static final String USER_ID = "urn:uuid:c23ec166-2c32-4f00-bdfe-b29398e36801";
    private static final String SIGNED_CRI_VC =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ1cm46dXVpZDpjMjNlYzE2Ni0yYzMyLTRmMDAtYmRmZS1iMjkzOThlMzY4MDEiLCJhdWQiOiJodHRwczpcL1wvaWRlbnRpdHkuYnVpbGQuYWNjb3VudC5nb3YudWsiLCJuYmYiOjE2OTIyNjc2NTMsImlzcyI6Imh0dHBzOlwvXC9rYnYtY3JpLnN0dWJzLmFjY291bnQuZ292LnVrIiwidmMiOnsidHlwZSI6WyJWZXJpZmlhYmxlQ3JlZGVudGlhbCIsIklkZW50aXR5Q2hlY2tDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidHlwZSI6IkdpdmVuTmFtZSIsInZhbHVlIjoiTWFyeSJ9LHsidHlwZSI6IkZhbWlseU5hbWUiLCJ2YWx1ZSI6IldhdHNvbiJ9XX1dLCJiaXJ0aERhdGUiOlt7InZhbHVlIjoiMTkzMi0wMi0yNSJ9XSwiYWRkcmVzcyI6W3siYnVpbGRpbmdOYW1lIjoiMjIxQiIsInN0cmVldE5hbWUiOiJCQUtFUiBTVFJFRVQiLCJwb3N0YWxDb2RlIjoiTlcxIDZYRSIsImFkZHJlc3NMb2NhbGl0eSI6IkxPTkRPTiIsInZhbGlkRnJvbSI6IjE4ODctMDEtMDEifV19LCJldmlkZW5jZSI6W3sidmVyaWZpY2F0aW9uU2NvcmUiOjAsImNpIjpbIlYwMyJdLCJ0eG4iOiIxOGZiZmU5My0yZTcxLTQ0YmItODhjNS0wZjdkZTYwZmJlODAiLCJ0eXBlIjoiSWRlbnRpdHlDaGVjayJ9XX0sImp0aSI6Ijg2ZTc3NmQxLTVjNmMtNDIzYy05OWNmLWUyZjYxOTQyYzY0YiJ9.TO4mRYGbD9QPxI3W8_gKmB87qTcIehhWXQ2RQgPvWrVbYynai0JDuphYRclXraLIBOAh_XK2mtBCpFnK9Rj0OQ";
    private static final String SIGNED_CRI_VC_NO_EVIDENCE =
            "eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL2lkZW50aXR5LnN0YWdpbmcuYWNjb3VudC5nb3YudWsiLCJpYXQiOjE2ODgxMjM0NjYsIm5iZiI6MTY4ODEyMzQ2NiwiZXhwIjoyMDAzNDgzNDY2LCJzdWIiOiJhLXVzZXItaWQiLCJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiU2VjdXJpdHlDaGVja0NyZWRlbnRpYWwiXSwiZXZpZGVuY2UiOltdfX0.licS4NM0EWKQm6fYT1plBQV6Bk4e9qrdXQ1NOo-GIvmTUhPbRSXHdUvGHUNbnVFxFZMyxdtBM_lkEUfqTpY64A";

    private static final String SIGNED_CRI_VC_INVALID_EVIDENCE =
            "eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL2lkZW50aXR5LnN0YWdpbmcuYWNjb3VudC5nb3YudWsiLCJpYXQiOjE2ODgxMjUwNDAsIm5iZiI6MTY4ODEyNTAzOSwiZXhwIjoyMDAzNDg1MDM5LCJzdWIiOiJhLXVzZXItaWQiLCJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiU2VjdXJpdHlDaGVja0NyZWRlbnRpYWwiXSwiZXZpZGVuY2UiOlt7InR5cGUiOiJTZWN1cml0eUNoZWNrIiwibm90QUNvbnRyYUluZGljYXRvciI6W3siY29kZSI6IkQwMSIsImlzc3VhbmNlRGF0ZSI6IjIwMjItMDktMjBUMTU6NTQ6NTAuMDAwWiIsImRvY3VtZW50IjoicGFzc3BvcnQvR0JSLzgyNDE1OTEyMSIsInR4biI6WyJhYmNkZWYiXSwibWl0aWdhdGlvbiI6W3siY29kZSI6Ik0wMSIsIm1pdGlnYXRpbmdDcmVkZW50aWFsIjpbeyJpc3N1ZXIiOiJodHRwczovL2NyZWRlbnRpYWwtaXNzdWVyLmV4YW1wbGUvIiwidmFsaWRGcm9tIjoiMjAyMi0wOS0yMVQxNTo1NDo1MC4wMDBaIiwidHhuIjoiZ2hpaiIsImlkIjoidXJuOnV1aWQ6ZjgxZDRmYWUtN2RlYy0xMWQwLWE3NjUtMDBhMGM5MWU2YmY2In1dfV0sImluY29tcGxldGVNaXRpZ2F0aW9uIjpbeyJjb2RlIjoiTTAyIiwibWl0aWdhdGluZ0NyZWRlbnRpYWwiOlt7Imlzc3VlciI6Imh0dHBzOi8vYW5vdGhlci1jcmVkZW50aWFsLWlzc3Vlci5leGFtcGxlLyIsInZhbGlkRnJvbSI6IjIwMjItMDktMjJUMTU6NTQ6NTAuMDAwWiIsInR4biI6ImNkZWVmIiwiaWQiOiJ1cm46dXVpZDpmNWM5ZmY0MC0xZGNkLTRhOGItYmY5Mi05NDU2MDQ3YzEzMmYifV19XX1dfV19fQ._2dCakEuFyF861YIxn7XJvBs03vbmPfX3H51YuUyn53sFDKJPZZzgAN_qMIphEfTlUMxclKtCu0b_ycseW3bFQ";

    private static final String DB_TTL = "1800";

    @Test
    public void addUserCisShouldInsert() {
        PutContraIndicatorsRequest putContraIndicatorsRequest =
                PutContraIndicatorsRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .signedJwt(SIGNED_CRI_VC)
                        .build();
        when(mockDataStore.getItems(USER_ID)).thenReturn(Collections.emptyList());

        ContraIndicatorsService cimitService =
                new ContraIndicatorsService(mockConfigService, cimitStubItemService);
        cimitService.addUserCis(putContraIndicatorsRequest);

        verify(mockDataStore, times(1)).getItems(any());
        verify(mockDataStore, times(1)).create(any(), eq(CIMIT_STUB_TTL));
        verify(mockDataStore, never()).update(any());
    }

    @Test
    public void addUserCisShouldUpdated() throws ParseException {
        String ci = "V03";
        PutContraIndicatorsRequest putContraIndicatorsRequest =
                PutContraIndicatorsRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .signedJwt(SIGNED_CRI_VC)
                        .build();
        String iss = SignedJWT.parse(SIGNED_CRI_VC).getJWTClaimsSet().getIssuer();
        CimitStubItem existingCimitStubItem =
                CimitStubItem.builder()
                        .userId(USER_ID)
                        .contraIndicatorCode(ci)
                        .issuers(List.of(iss))
                        .issuanceDate(Instant.now())
                        .mitigations(Collections.emptyList())
                        .build();

        when(mockDataStore.getItems(USER_ID))
                .thenReturn(Collections.singletonList(existingCimitStubItem));
        when(mockConfigService.getSsmParameter(CIMIT_STUB_TTL)).thenReturn(DB_TTL);

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
                        .signedJwt(SIGNED_CRI_VC_NO_EVIDENCE)
                        .build();

        cimitService.addUserCis(putContraIndicatorsRequest);

        verify(mockDataStore, never()).getItems(any());
        verify(mockDataStore, never()).create(any(), eq(CIMIT_STUB_TTL));
    }

    @Test
    public void addUserCisShouldFailedIfJWTParsingFails() {
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
                        .signedJwt(SIGNED_CRI_VC_INVALID_EVIDENCE)
                        .build();

        cimitService.addUserCis(putContraIndicatorsRequest);

        verify(mockDataStore, never()).getItems(any());
        verify(mockDataStore, never()).create(any(), eq(CIMIT_STUB_TTL));
    }
}
