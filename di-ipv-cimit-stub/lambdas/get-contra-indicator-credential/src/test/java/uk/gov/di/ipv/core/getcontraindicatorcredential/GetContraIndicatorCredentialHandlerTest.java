package uk.gov.di.ipv.core.getcontraindicatorcredential;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.GetCiCredentialRequest;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.GetCiCredentialResponse;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential.ContraIndicator;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential.MitigatingCredential;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential.Mitigation;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential.VcClaim;
import uk.gov.di.ipv.core.getcontraindicatorcredential.factory.ECDSASignerFactory;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.service.CimitStubItemService;
import uk.gov.di.ipv.core.library.service.ConfigService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.core.library.vc.VerifiableCredentialConstants.VC_CLAIM;

@ExtendWith(MockitoExtension.class)
class GetContraIndicatorCredentialHandlerTest {

    private static final String USER_ID = "user_id";
    private static final String CI_V03 = "V03";
    private static final String CI_D02 = "D02";
    private static final String MITIGATION_M01 = "M01";
    private static final String CIMIT_PRIVATE_KEY =
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgOXt0P05ZsQcK7eYusgIPsqZdaBCIJiW4imwUtnaAthWhRANCAAQT1nO46ipxVTilUH2umZPN7OPI49GU6Y8YkcqLxFKUgypUzGbYR2VJGM+QJXk0PI339EyYkt6tjgfS+RcOMQNO";
    private static final String CIMIT_PUBLIC_JWK =
            "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"E9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIM\",\"y\":\"KlTMZthHZUkYz5AleTQ8jff0TJiS3q2OB9L5Fw4xA04\"}";
    private static final String ISSUERS_TEST = "https://review-d.account.gov.uk";
    private static final String CIMIT_COMPONENT_ID = "https://cimit.stubs.account.gov.uk";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private Context mockContext;
    @Mock private ConfigService mockConfigService;
    @Mock private CimitStubItemService mockCimitStubItemService;
    @Spy private ECDSASignerFactory spyEcdsaSignerFactory = new ECDSASignerFactory();
    @InjectMocks private GetContraIndicatorCredentialHandler getContraIndicatorCredentialHandler;

    @Test
    void shouldReturnSignedJwtWhenProvidedValidRequest() throws Exception {
        when(mockConfigService.getCimitSigningKey()).thenReturn(CIMIT_PRIVATE_KEY);
        when(mockConfigService.getCimitComponentId()).thenReturn(CIMIT_COMPONENT_ID);
        List<CimitStubItem> cimitStubItems = new ArrayList<>();
        Instant issuanceDate = Instant.now();
        cimitStubItems.add(
                CimitStubItem.builder()
                        .userId(USER_ID)
                        .contraIndicatorCode(CI_V03)
                        .issuer(ISSUERS_TEST)
                        .issuanceDate(issuanceDate)
                        .mitigations(List.of(MITIGATION_M01))
                        .build());
        when(mockCimitStubItemService.getCIsForUserId(USER_ID)).thenReturn(cimitStubItems);

        GetCiCredentialRequest getCiCredentialRequest =
                GetCiCredentialRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .userId(USER_ID)
                        .build();

        var response = makeRequest(getCiCredentialRequest);

        verify(mockConfigService).getCimitSigningKey();
        verify(mockConfigService).getCimitComponentId();

        var signedJWT = SignedJWT.parse(response.getVc());
        var claimsSet = signedJWT.getJWTClaimsSet();

        assertEquals(USER_ID, signedJWT.getJWTClaimsSet().getClaim(JWTClaimNames.SUBJECT));
        assertEquals(
                CIMIT_COMPONENT_ID, signedJWT.getJWTClaimsSet().getClaim(JWTClaimNames.ISSUER));

        var vcClaim =
                objectMapper.convertValue(claimsSet.getJSONObjectClaim(VC_CLAIM), VcClaim.class);

        assertEquals(List.of("VerifiableCredential", "SecurityCheckCredential"), vcClaim.type());

        var evidenceTxn = vcClaim.evidence().get(0).txn();

        var contraIndicators = vcClaim.evidence().get(0).contraIndicator();
        assertEquals(1, contraIndicators.size());

        var firstContraIndicator = contraIndicators.get(0);
        assertEquals(evidenceTxn, firstContraIndicator.getTxn());

        ContraIndicator expectedCi =
                ContraIndicator.builder()
                        .code(CI_V03)
                        .issuers(new TreeSet<>(List.of(ISSUERS_TEST)))
                        .issuanceDate(issuanceDate.toString())
                        .mitigation(
                                List.of(
                                        new Mitigation(
                                                MITIGATION_M01,
                                                List.of(MitigatingCredential.EMPTY))))
                        .incompleteMitigation(List.of())
                        .document(new TreeSet<>())
                        .txn(evidenceTxn)
                        .build();
        assertEquals(expectedCi, firstContraIndicator);

        ECDSAVerifier verifier = new ECDSAVerifier(ECKey.parse(CIMIT_PUBLIC_JWK));
        assertTrue(signedJWT.verify(verifier));
    }

    @Test
    void shouldDeduplicateUnmitigatedCi() throws Exception {
        when(mockConfigService.getCimitSigningKey()).thenReturn(CIMIT_PRIVATE_KEY);
        when(mockConfigService.getCimitComponentId()).thenReturn(CIMIT_COMPONENT_ID);
        List<CimitStubItem> cimitStubItems = new ArrayList<>();
        Instant issuanceDate = Instant.now();
        cimitStubItems.add(
                CimitStubItem.builder()
                        .userId(USER_ID)
                        .contraIndicatorCode(CI_V03)
                        .issuer("issuer1")
                        .issuanceDate(issuanceDate.minusSeconds(100L))
                        .mitigations(new ArrayList<>(List.of()))
                        .documentIdentifier(null)
                        .build());
        cimitStubItems.add(
                CimitStubItem.builder()
                        .userId(USER_ID)
                        .contraIndicatorCode(CI_V03)
                        .issuer("issuer1")
                        .issuanceDate(issuanceDate)
                        .mitigations(new ArrayList<>(List.of()))
                        .documentIdentifier(null)
                        .build());
        cimitStubItems.add(
                CimitStubItem.builder()
                        .userId(USER_ID)
                        .contraIndicatorCode(CI_D02)
                        .issuer("issuer2")
                        .issuanceDate(issuanceDate.minusSeconds(100L))
                        .mitigations(new ArrayList<>(List.of()))
                        .documentIdentifier("docId/1")
                        .build());
        cimitStubItems.add(
                CimitStubItem.builder()
                        .userId(USER_ID)
                        .contraIndicatorCode(CI_D02)
                        .issuer("issuer3")
                        .issuanceDate(issuanceDate)
                        .mitigations(new ArrayList<>(List.of()))
                        .documentIdentifier("docId/2")
                        .build());
        when(mockCimitStubItemService.getCIsForUserId(USER_ID)).thenReturn(cimitStubItems);

        GetCiCredentialRequest getCiCredentialRequest =
                GetCiCredentialRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .userId(USER_ID)
                        .build();

        var response = makeRequest(getCiCredentialRequest);

        verify(mockConfigService).getCimitSigningKey();
        verify(mockConfigService).getCimitComponentId();

        var signedJWT = SignedJWT.parse(response.getVc());
        var claimsSet = signedJWT.getJWTClaimsSet();
        var vcClaim =
                objectMapper.convertValue(claimsSet.getJSONObjectClaim(VC_CLAIM), VcClaim.class);

        var evidenceTxn = vcClaim.evidence().get(0).txn();

        var contraIndicators = vcClaim.evidence().get(0).contraIndicator();
        assertEquals(2, contraIndicators.size());

        ContraIndicator expectedFirstCi =
                ContraIndicator.builder()
                        .code(CI_V03)
                        .issuers(new TreeSet<>(List.of("issuer1")))
                        .issuanceDate(issuanceDate.toString())
                        .mitigation(List.of())
                        .incompleteMitigation(List.of())
                        .document(new TreeSet<>())
                        .txn(evidenceTxn)
                        .build();
        assertEquals(expectedFirstCi, contraIndicators.get(0));

        ContraIndicator expectedSecondCi =
                ContraIndicator.builder()
                        .code(CI_D02)
                        .issuers(new TreeSet<>(List.of("issuer2", "issuer3")))
                        .issuanceDate(issuanceDate.toString())
                        .mitigation(List.of())
                        .incompleteMitigation(List.of())
                        .document(new TreeSet<>(List.of("docId/1", "docId/2")))
                        .txn(evidenceTxn)
                        .build();
        assertEquals(expectedSecondCi, contraIndicators.get(1));

        ECDSAVerifier verifier = new ECDSAVerifier(ECKey.parse(CIMIT_PUBLIC_JWK));
        assertTrue(signedJWT.verify(verifier));
    }

    @Test
    void shouldReturnSeparateCiForMitigatedCiWithUnmitigatedVersion() throws Exception {
        when(mockConfigService.getCimitSigningKey()).thenReturn(CIMIT_PRIVATE_KEY);
        when(mockConfigService.getCimitComponentId()).thenReturn(CIMIT_COMPONENT_ID);
        List<CimitStubItem> cimitStubItems = new ArrayList<>();
        Instant issuanceDate = Instant.now();
        cimitStubItems.add(
                CimitStubItem.builder()
                        .userId(USER_ID)
                        .contraIndicatorCode(CI_D02)
                        .issuer("issuer1")
                        .issuanceDate(issuanceDate.minusSeconds(100L))
                        .mitigations(List.of())
                        .documentIdentifier("docId/1")
                        .build());
        cimitStubItems.add(
                CimitStubItem.builder()
                        .userId(USER_ID)
                        .contraIndicatorCode(CI_D02)
                        .issuer("issuer2")
                        .issuanceDate(issuanceDate.minusSeconds(200L))
                        .mitigations(List.of())
                        .documentIdentifier("docId/2")
                        .build());
        cimitStubItems.add(
                CimitStubItem.builder()
                        .userId(USER_ID)
                        .contraIndicatorCode(CI_D02)
                        .issuer("issuer3")
                        .issuanceDate(issuanceDate)
                        .mitigations(List.of(MITIGATION_M01))
                        .documentIdentifier("docId/3")
                        .build());
        when(mockCimitStubItemService.getCIsForUserId(USER_ID)).thenReturn(cimitStubItems);

        GetCiCredentialRequest getCiCredentialRequest =
                GetCiCredentialRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .userId(USER_ID)
                        .build();

        var response = makeRequest(getCiCredentialRequest);

        var signedJWT = SignedJWT.parse(response.getVc());
        var claimsSet = signedJWT.getJWTClaimsSet();
        var vcClaim =
                objectMapper.convertValue(claimsSet.getJSONObjectClaim(VC_CLAIM), VcClaim.class);

        var evidenceTxn = vcClaim.evidence().get(0).txn();

        var contraIndicators = vcClaim.evidence().get(0).contraIndicator();
        assertEquals(2, contraIndicators.size());

        ContraIndicator expectedFirstCi =
                ContraIndicator.builder()
                        .code(CI_D02)
                        .issuers(new TreeSet<>(List.of("issuer1", "issuer2")))
                        .issuanceDate(issuanceDate.minusSeconds(100L).toString())
                        .mitigation(List.of())
                        .incompleteMitigation(List.of())
                        .document(new TreeSet<>(List.of("docId/1", "docId/2")))
                        .txn(evidenceTxn)
                        .build();
        assertEquals(expectedFirstCi, contraIndicators.get(0));

        ContraIndicator expectedSecondCi =
                ContraIndicator.builder()
                        .code(CI_D02)
                        .issuers(new TreeSet<>(List.of("issuer3")))
                        .issuanceDate(issuanceDate.toString())
                        .mitigation(
                                List.of(
                                        new Mitigation(
                                                MITIGATION_M01,
                                                List.of(MitigatingCredential.EMPTY))))
                        .incompleteMitigation(List.of())
                        .document(new TreeSet<>(List.of("docId/3")))
                        .txn(evidenceTxn)
                        .build();
        assertEquals(expectedSecondCi, contraIndicators.get(1));

        ECDSAVerifier verifier = new ECDSAVerifier(ECKey.parse(CIMIT_PUBLIC_JWK));
        assertTrue(signedJWT.verify(verifier));
    }

    @Test
    void shouldFailForWrongCimitKey() throws IOException {
        when(mockConfigService.getCimitSigningKey()).thenReturn("Invalid_cimit_key");

        GetCiCredentialRequest getCiCredentialRequest =
                GetCiCredentialRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .userId(USER_ID)
                        .build();

        var response = makeRequest(getCiCredentialRequest);

        verify(mockConfigService).getCimitSigningKey();

        assertEquals("Failure", response.getVc());
    }

    private GetCiCredentialResponse makeRequest(GetCiCredentialRequest request) throws IOException {
        try (var inputStream = new ByteArrayInputStream(objectMapper.writeValueAsBytes(request));
                var outputStream = new ByteArrayOutputStream()) {
            getContraIndicatorCredentialHandler.handleRequest(
                    inputStream, outputStream, mockContext);
            return objectMapper.readValue(outputStream.toString(), GetCiCredentialResponse.class);
        }
    }
}
