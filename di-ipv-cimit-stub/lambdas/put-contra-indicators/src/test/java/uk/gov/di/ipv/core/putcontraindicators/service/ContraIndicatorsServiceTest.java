package uk.gov.di.ipv.core.putcontraindicators.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.di.ipv.core.library.config.ConfigurationVariable.CIMIT_STUB_TTL;

@ExtendWith(MockitoExtension.class)
public class ContraIndicatorsServiceTest {
    @Captor ArgumentCaptor<CimitStubItem> cimitStubItemArgumentCaptor;
    @Mock private DataStore<CimitStubItem> mockDataStore;
    @Mock private ConfigService mockConfigService;
    @InjectMocks private CimitStubItemService cimitStubItemService;

    private static final String SIGNED_CRI_VC =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ1cm46dXVpZDpjMjNlYzE2Ni0yYzMyLTRmMDAtYmRmZS1iMjkzOThlMzY4MDEiLCJhdWQiOiJodHRwczpcL1wvaWRlbnRpdHkuYnVpbGQuYWNjb3VudC5nb3YudWsiLCJuYmYiOjE2OTIyNjc2NTMsImlzcyI6Imh0dHBzOlwvXC9rYnYtY3JpLnN0dWJzLmFjY291bnQuZ292LnVrIiwidmMiOnsidHlwZSI6WyJWZXJpZmlhYmxlQ3JlZGVudGlhbCIsIklkZW50aXR5Q2hlY2tDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidHlwZSI6IkdpdmVuTmFtZSIsInZhbHVlIjoiTWFyeSJ9LHsidHlwZSI6IkZhbWlseU5hbWUiLCJ2YWx1ZSI6IldhdHNvbiJ9XX1dLCJiaXJ0aERhdGUiOlt7InZhbHVlIjoiMTkzMi0wMi0yNSJ9XSwiYWRkcmVzcyI6W3siYnVpbGRpbmdOYW1lIjoiMjIxQiIsInN0cmVldE5hbWUiOiJCQUtFUiBTVFJFRVQiLCJwb3N0YWxDb2RlIjoiTlcxIDZYRSIsImFkZHJlc3NMb2NhbGl0eSI6IkxPTkRPTiIsInZhbGlkRnJvbSI6IjE4ODctMDEtMDEifV19LCJldmlkZW5jZSI6W3sidmVyaWZpY2F0aW9uU2NvcmUiOjAsImNpIjpbIlYwMyJdLCJ0eG4iOiIxOGZiZmU5My0yZTcxLTQ0YmItODhjNS0wZjdkZTYwZmJlODAiLCJ0eXBlIjoiSWRlbnRpdHlDaGVjayJ9XX0sImp0aSI6Ijg2ZTc3NmQxLTVjNmMtNDIzYy05OWNmLWUyZjYxOTQyYzY0YiJ9.TO4mRYGbD9QPxI3W8_gKmB87qTcIehhWXQ2RQgPvWrVbYynai0JDuphYRclXraLIBOAh_XK2mtBCpFnK9Rj0OQ";
    private static final String SIGNED_CRI_VC_NO_EVIDENCE =
            "eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL2lkZW50aXR5LnN0YWdpbmcuYWNjb3VudC5nb3YudWsiLCJpYXQiOjE2ODgxMjM0NjYsIm5iZiI6MTY4ODEyMzQ2NiwiZXhwIjoyMDAzNDgzNDY2LCJzdWIiOiJhLXVzZXItaWQiLCJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiU2VjdXJpdHlDaGVja0NyZWRlbnRpYWwiXSwiZXZpZGVuY2UiOltdfX0.licS4NM0EWKQm6fYT1plBQV6Bk4e9qrdXQ1NOo-GIvmTUhPbRSXHdUvGHUNbnVFxFZMyxdtBM_lkEUfqTpY64A";
    private static final String SIGNED_CRI_VC_INVALID_EVIDENCE =
            "eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL2lkZW50aXR5LnN0YWdpbmcuYWNjb3VudC5nb3YudWsiLCJpYXQiOjE2ODgxMjUwNDAsIm5iZiI6MTY4ODEyNTAzOSwiZXhwIjoyMDAzNDg1MDM5LCJzdWIiOiJhLXVzZXItaWQiLCJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiU2VjdXJpdHlDaGVja0NyZWRlbnRpYWwiXSwiZXZpZGVuY2UiOlt7InR5cGUiOiJTZWN1cml0eUNoZWNrIiwibm90QUNvbnRyYUluZGljYXRvciI6W3siY29kZSI6IkQwMSIsImlzc3VhbmNlRGF0ZSI6IjIwMjItMDktMjBUMTU6NTQ6NTAuMDAwWiIsImRvY3VtZW50IjoicGFzc3BvcnQvR0JSLzgyNDE1OTEyMSIsInR4biI6WyJhYmNkZWYiXSwibWl0aWdhdGlvbiI6W3siY29kZSI6Ik0wMSIsIm1pdGlnYXRpbmdDcmVkZW50aWFsIjpbeyJpc3N1ZXIiOiJodHRwczovL2NyZWRlbnRpYWwtaXNzdWVyLmV4YW1wbGUvIiwidmFsaWRGcm9tIjoiMjAyMi0wOS0yMVQxNTo1NDo1MC4wMDBaIiwidHhuIjoiZ2hpaiIsImlkIjoidXJuOnV1aWQ6ZjgxZDRmYWUtN2RlYy0xMWQwLWE3NjUtMDBhMGM5MWU2YmY2In1dfV0sImluY29tcGxldGVNaXRpZ2F0aW9uIjpbeyJjb2RlIjoiTTAyIiwibWl0aWdhdGluZ0NyZWRlbnRpYWwiOlt7Imlzc3VlciI6Imh0dHBzOi8vYW5vdGhlci1jcmVkZW50aWFsLWlzc3Vlci5leGFtcGxlLyIsInZhbGlkRnJvbSI6IjIwMjItMDktMjJUMTU6NTQ6NTAuMDAwWiIsInR4biI6ImNkZWVmIiwiaWQiOiJ1cm46dXVpZDpmNWM5ZmY0MC0xZGNkLTRhOGItYmY5Mi05NDU2MDQ3YzEzMmYifV19XX1dfV19fQ._2dCakEuFyF861YIxn7XJvBs03vbmPfX3H51YuUyn53sFDKJPZZzgAN_qMIphEfTlUMxclKtCu0b_ycseW3bFQ";
    private static final String PASSPORT_VC =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Jldmlldy1wLnN0YWdpbmcuYWNjb3VudC5nb3YudWsiLCJzdWIiOiJ1cm46dXVpZDpiZmQzM2QyYi0yYTAzLTQ0MWMtOTBkYi00NGFlZDdhM2E3ZWYiLCJuYmYiOjE3MDg1MDY3OTMsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJJZGVudGl0eUNoZWNrQ3JlZGVudGlhbCJdLCJjcmVkZW50aWFsU3ViamVjdCI6eyJiaXJ0aERhdGUiOlt7InZhbHVlIjoiMTk2NS0wNy0wOCJ9XSwicGFzc3BvcnQiOlt7ImRvY3VtZW50TnVtYmVyIjoiMzIxNjU0OTg3IiwiaWNhb0lzc3VlckNvZGUiOiJHQlIiLCJleHBpcnlEYXRlIjoiMjAzMC0wMS0wMSJ9XSwibmFtZSI6W3sibmFtZVBhcnRzIjpbeyJ0eXBlIjoiR2l2ZW5OYW1lIiwidmFsdWUiOiJLRU5ORVRISEhIIn0seyJ0eXBlIjoiRmFtaWx5TmFtZSIsInZhbHVlIjoiREVDRVJRVUVJUkEifV19XX0sImV2aWRlbmNlIjpbeyJ0eXBlIjoiSWRlbnRpdHlDaGVjayIsInR4biI6IjAzMmY4YjA3LWYxYWYtNDVhZC1hZjE4LWMyNjU2NTkxY2QyOCIsInN0cmVuZ3RoU2NvcmUiOjQsInZhbGlkaXR5U2NvcmUiOjAsImNpIjpbIkQwMiJdLCJmYWlsZWRDaGVja0RldGFpbHMiOlt7ImNoZWNrTWV0aG9kIjoiZGF0YSIsImRhdGFDaGVjayI6InJlY29yZF9jaGVjayJ9XSwiY2lSZWFzb25zIjpbeyJjaSI6IkQwMiIsInJlYXNvbiI6Ik5vTWF0Y2hpbmdSZWNvcmQifV19XX0sImp0aSI6InVybjp1dWlkOjM4ODBlYzg3LWIwZGMtNDYzNy04ZTRkLTA2NjU0YjFkNzY2ZiJ9.XeggA5BpYrxGxwyDKSZaZG4_7YiTR62m4tgLhncsNrT9V0NT_RBAYeIRY-dIuccnPLbn1rEYGt9XRwcP9NYVbg";
    private static final String DRIVING_PERMIT_VC =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Jldmlldy1kLnN0YWdpbmcuYWNjb3VudC5nb3YudWsiLCJzdWIiOiJ1cm46dXVpZDo0NTdkODgwMS0wN2VmLTRmNmQtOWI0Ny04MDY5YjI2YjFjOTYiLCJuYmYiOjE3MDg1MDgyNzcsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJJZGVudGl0eUNoZWNrQ3JlZGVudGlhbCJdLCJjcmVkZW50aWFsU3ViamVjdCI6eyJkcml2aW5nUGVybWl0IjpbeyJwZXJzb25hbE51bWJlciI6IkRFQ0VSNjA3MDg1S0U5TE4iLCJleHBpcnlEYXRlIjoiMjA0Mi0xMC0wMSIsImlzc3VlTnVtYmVyIjoiMjMiLCJpc3N1ZWRCeSI6IkRWTEEiLCJpc3N1ZURhdGUiOiIyMDE4LTA0LTE5In1dLCJuYW1lIjpbeyJuYW1lUGFydHMiOlt7InR5cGUiOiJHaXZlbk5hbWUiLCJ2YWx1ZSI6IktFTk5FVEgifSx7InR5cGUiOiJGYW1pbHlOYW1lIiwidmFsdWUiOiJERUNFUlFVRUlSQSJ9XX1dLCJhZGRyZXNzIjpbeyJpZCI6bnVsbCwicG9Cb3hOdW1iZXIiOm51bGwsInN1YkJ1aWxkaW5nTmFtZSI6bnVsbCwiYnVpbGRpbmdOdW1iZXIiOm51bGwsImJ1aWxkaW5nTmFtZSI6bnVsbCwic3RyZWV0TmFtZSI6bnVsbCwiYWRkcmVzc0xvY2FsaXR5IjpudWxsLCJwb3N0YWxDb2RlIjoiQkEyIDVBQSIsImFkZHJlc3NDb3VudHJ5IjoiR0IifV0sImJpcnRoRGF0ZSI6W3sidmFsdWUiOiIxOTY1LTA3LTA4In1dfSwiZXZpZGVuY2UiOlt7InR5cGUiOiJJZGVudGl0eUNoZWNrIiwidHhuIjoiNTNkZGU2ODEtYzZhZC00MzYzLWFkZWYtZmM0MDAxZmI2MTZiIiwiYWN0aXZpdHlIaXN0b3J5U2NvcmUiOjAsInN0cmVuZ3RoU2NvcmUiOjMsInZhbGlkaXR5U2NvcmUiOjAsImNpIjpbIkQwMiJdLCJmYWlsZWRDaGVja0RldGFpbHMiOlt7ImNoZWNrTWV0aG9kIjoiZGF0YSIsImlkZW50aXR5Q2hlY2tQb2xpY3kiOiJwdWJsaXNoZWQifV19XX0sImp0aSI6InVybjp1dWlkOjYxMDY1YTk2LTA4NDgtNDAxNy05YjE0LWFhODllOGI3ZDAzOCJ9.9M862Le368uXMxzUFpBKg13tBDCZErFXRcyjAuspthTho7qubpvuBkPNqiXL-rmi8ZzvRzY1o3St8iE8uvQV8A";

    @Test
    void addUserCisShouldInsert() {
        PutContraIndicatorsRequest putContraIndicatorsRequest =
                PutContraIndicatorsRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .signedJwt(SIGNED_CRI_VC)
                        .build();

        ContraIndicatorsService cimitService =
                new ContraIndicatorsService(mockConfigService, cimitStubItemService);
        cimitService.addUserCis(putContraIndicatorsRequest);

        verify(mockDataStore, times(1))
                .create(cimitStubItemArgumentCaptor.capture(), eq(CIMIT_STUB_TTL));
        verify(mockDataStore, never()).update(any());

        CimitStubItem expectedItem =
                CimitStubItem.builder()
                        .userId("urn:uuid:c23ec166-2c32-4f00-bdfe-b29398e36801")
                        .contraIndicatorCode("V03")
                        .issuer("https://kbv-cri.stubs.account.gov.uk")
                        .issuanceDate(Instant.parse("2023-08-17T10:20:53Z"))
                        .mitigations(List.of())
                        .document(null)
                        .sortKey("V03#2023-08-17T10:20:53Z")
                        .txn("18fbfe93-2e71-44bb-88c5-0f7de60fbe80")
                        .build();

        assertEquals(expectedItem, cimitStubItemArgumentCaptor.getValue());
    }

    @Test
    void addUserCisShouldFailedIfNoEvidence() {
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
    void addUserCisShouldFailedIfJWTParsingFails() {
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
    void addUserCisShouldFailedIfInvalidEvidence() {
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

    @Test
    void addUserCisShouldStorePassportIdentifierIfPresent() {
        PutContraIndicatorsRequest putContraIndicatorsRequest =
                PutContraIndicatorsRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .signedJwt(PASSPORT_VC)
                        .build();

        ContraIndicatorsService cimitService =
                new ContraIndicatorsService(mockConfigService, cimitStubItemService);
        cimitService.addUserCis(putContraIndicatorsRequest);

        verify(mockDataStore, times(1))
                .create(cimitStubItemArgumentCaptor.capture(), eq(CIMIT_STUB_TTL));
        verify(mockDataStore, never()).update(any());

        assertEquals(
                "passport/GBR/321654987", cimitStubItemArgumentCaptor.getValue().getDocument());
    }

    @Test
    void addUserCisShouldStoreDrivingPermitIdentifierIfPresent() {
        PutContraIndicatorsRequest putContraIndicatorsRequest =
                PutContraIndicatorsRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .signedJwt(DRIVING_PERMIT_VC)
                        .build();

        ContraIndicatorsService cimitService =
                new ContraIndicatorsService(mockConfigService, cimitStubItemService);
        cimitService.addUserCis(putContraIndicatorsRequest);

        verify(mockDataStore, times(1))
                .create(cimitStubItemArgumentCaptor.capture(), eq(CIMIT_STUB_TTL));
        verify(mockDataStore, never()).update(any());

        assertEquals(
                "drivingPermit/GB/DVLA/DECER607085KE9LN/2018-04-19",
                cimitStubItemArgumentCaptor.getValue().getDocument());
    }
}
