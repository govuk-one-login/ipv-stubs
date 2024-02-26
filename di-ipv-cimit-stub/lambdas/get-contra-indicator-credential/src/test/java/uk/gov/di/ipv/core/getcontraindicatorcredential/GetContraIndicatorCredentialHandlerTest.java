package uk.gov.di.ipv.core.getcontraindicatorcredential;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.GetCiCredentialRequest;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.GetCiCredentialResponse;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.service.CimitStubItemService;
import uk.gov.di.ipv.core.library.service.ConfigService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.core.getcontraindicatorcredential.GetContraIndicatorCredentialHandler.CODE;
import static uk.gov.di.ipv.core.getcontraindicatorcredential.GetContraIndicatorCredentialHandler.CONTRA_INDICATORS;
import static uk.gov.di.ipv.core.getcontraindicatorcredential.GetContraIndicatorCredentialHandler.DOCUMENT;
import static uk.gov.di.ipv.core.getcontraindicatorcredential.GetContraIndicatorCredentialHandler.ISSUANCE_DATE;
import static uk.gov.di.ipv.core.getcontraindicatorcredential.GetContraIndicatorCredentialHandler.ISSUERS;
import static uk.gov.di.ipv.core.getcontraindicatorcredential.GetContraIndicatorCredentialHandler.MITIGATION;
import static uk.gov.di.ipv.core.getcontraindicatorcredential.GetContraIndicatorCredentialHandler.MITIGATION_CREDENTIAL;
import static uk.gov.di.ipv.core.getcontraindicatorcredential.GetContraIndicatorCredentialHandler.SECURITY_CHECK_CREDENTIAL_VC_TYPE;
import static uk.gov.di.ipv.core.getcontraindicatorcredential.GetContraIndicatorCredentialHandler.TYPE;
import static uk.gov.di.ipv.core.library.vc.VerifiableCredentialConstants.VC_CLAIM;
import static uk.gov.di.ipv.core.library.vc.VerifiableCredentialConstants.VC_EVIDENCE;

@ExtendWith(MockitoExtension.class)
class GetContraIndicatorCredentialHandlerTest {

    public static final String USER_ID = "user_id";
    public static final String CI_V_03 = "V03";
    public static final String MITIGATION_M_01 = "M01";
    private static final String CIMIT_PRIVATE_KEY =
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgOXt0P05ZsQcK7eYusgIPsqZdaBCIJiW4imwUtnaAthWhRANCAAQT1nO46ipxVTilUH2umZPN7OPI49GU6Y8YkcqLxFKUgypUzGbYR2VJGM+QJXk0PI339EyYkt6tjgfS+RcOMQNO";
    private static final String CIMIT_PUBLIC_JWK =
            "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"E9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIM\",\"y\":\"KlTMZthHZUkYz5AleTQ8jff0TJiS3q2OB9L5Fw4xA04\"}";
    public static final String ISSUERS_TEST = "https://review-d.account.gov.uk";

    private static final String CIMIT_COMPONENT_ID = "https://cimit.stubs.account.gov.uk";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static final String CI_D02 = "D02";

    @Mock private Context mockContext;
    @Mock private ConfigService mockConfigService;
    @Mock private CimitStubItemService mockCimitStubItemService;
    @InjectMocks private GetContraIndicatorCredentialHandler getContraIndicatorCredentialHandler;

    @Test
    void shouldReturnSignedJwtWhenProvidedValidRequest()
            throws IOException, ParseException, JOSEException {
        when(mockConfigService.getCimitSigningKey()).thenReturn(CIMIT_PRIVATE_KEY);
        when(mockConfigService.getCimitComponentId()).thenReturn(CIMIT_COMPONENT_ID);
        List<CimitStubItem> cimitStubItems = new ArrayList<>();
        Instant issuanceDate = Instant.now();
        cimitStubItems.add(
                CimitStubItem.builder()
                        .userId(USER_ID)
                        .contraIndicatorCode(CI_V_03)
                        .issuer(ISSUERS_TEST)
                        .issuanceDate(issuanceDate)
                        .mitigations(List.of(MITIGATION_M_01))
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
        assertClaimsJWTIsValid(response.getVc(), issuanceDate);
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
                        .contraIndicatorCode(CI_V_03)
                        .issuer("issuer1")
                        .issuanceDate(issuanceDate.minusSeconds(100L))
                        .mitigations(new ArrayList<>(List.of()))
                        .documentIdentifier(null)
                        .build());
        cimitStubItems.add(
                CimitStubItem.builder()
                        .userId(USER_ID)
                        .contraIndicatorCode(CI_V_03)
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

        assertEquals(USER_ID, claimsSet.getClaim(JWTClaimNames.SUBJECT));
        assertEquals(CIMIT_COMPONENT_ID, claimsSet.getClaim(JWTClaimNames.ISSUER));

        JsonNode vc = objectMapper.readTree(claimsSet.toString()).get(VC_CLAIM);
        assertEquals(2, vc.size());
        assertEquals(SECURITY_CHECK_CREDENTIAL_VC_TYPE, vc.get(TYPE).get(0).asText());

        JsonNode contraIndicators = vc.get(VC_EVIDENCE).get(0).get(CONTRA_INDICATORS);
        assertEquals(2, contraIndicators.size());

        JsonNode firstCINode = contraIndicators.get(0);
        assertEquals(CI_V_03, firstCINode.get(CODE).asText());

        JsonNode firstCiIssuers = firstCINode.get(ISSUERS);
        assertEquals("[\"issuer1\"]", firstCiIssuers.toString());

        assertEquals(issuanceDate.toString(), firstCINode.get(ISSUANCE_DATE).asText());

        assertEquals(0, firstCINode.get(MITIGATION).size());
        assertEquals(0, firstCINode.get(DOCUMENT).size());

        JsonNode secondCiNode = contraIndicators.get(1);
        assertEquals(CI_D02, secondCiNode.get(CODE).asText());

        JsonNode issuers = secondCiNode.get(ISSUERS);
        assertEquals("[\"issuer2\",\"issuer3\"]", issuers.toString());

        assertEquals(issuanceDate.toString(), secondCiNode.get(ISSUANCE_DATE).asText());

        assertEquals(0, secondCiNode.get(MITIGATION).size());
        assertEquals("[\"docId/1\",\"docId/2\"]", secondCiNode.get(DOCUMENT).toString());

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
                        .mitigations(List.of("M01"))
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

        JsonNode vc = objectMapper.readTree(claimsSet.toString()).get(VC_CLAIM);

        JsonNode contraIndicators = vc.get(VC_EVIDENCE).get(0).get(CONTRA_INDICATORS);
        assertEquals(2, contraIndicators.size());

        JsonNode firstCINode = contraIndicators.get(0);
        assertEquals(CI_D02, firstCINode.get(CODE).asText());

        assertEquals("[\"issuer1\",\"issuer2\"]", firstCINode.get(ISSUERS).toString());
        assertEquals(
                issuanceDate.minusSeconds(100L).toString(),
                firstCINode.get(ISSUANCE_DATE).asText());
        assertEquals(0, firstCINode.get(MITIGATION).size());
        assertEquals("[\"docId/1\",\"docId/2\"]", firstCINode.get(DOCUMENT).toString());

        JsonNode secondCiNode = contraIndicators.get(1);
        assertEquals(CI_D02, secondCiNode.get(CODE).asText());

        JsonNode issuers = secondCiNode.get(ISSUERS);
        assertEquals("[\"issuer3\"]", issuers.toString());

        assertEquals(issuanceDate.toString(), secondCiNode.get(ISSUANCE_DATE).asText());

        assertEquals(
                "[{\"mitigatingCredential\":[],\"code\":\"M01\"}]",
                secondCiNode.get(MITIGATION).toString());
        assertEquals("[\"docId/3\"]", secondCiNode.get(DOCUMENT).toString());

        ECDSAVerifier verifier = new ECDSAVerifier(ECKey.parse(CIMIT_PUBLIC_JWK));
        assertTrue(signedJWT.verify(verifier));
    }

    @Test
    void shouldFailForWrongCimitKey() throws IOException {
        when(mockConfigService.getCimitSigningKey()).thenReturn("Invalid_cimit_key");
        when(mockConfigService.getCimitComponentId()).thenReturn(CIMIT_COMPONENT_ID);

        GetCiCredentialRequest getCiCredentialRequest =
                GetCiCredentialRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .userId(USER_ID)
                        .build();

        var response = makeRequest(getCiCredentialRequest);

        verify(mockConfigService).getCimitSigningKey();
        verify(mockConfigService).getCimitComponentId();

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

    private void assertClaimsJWTIsValid(String credential, Instant issuanceDate)
            throws ParseException, JsonProcessingException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(credential);
        JsonNode claimsSet = objectMapper.readTree(signedJWT.getJWTClaimsSet().toString());

        assertEquals(USER_ID, signedJWT.getJWTClaimsSet().getClaim(JWTClaimNames.SUBJECT));
        assertEquals(
                CIMIT_COMPONENT_ID, signedJWT.getJWTClaimsSet().getClaim(JWTClaimNames.ISSUER));

        JsonNode vc = claimsSet.get(VC_CLAIM);
        assertEquals(2, vc.size());
        assertEquals(SECURITY_CHECK_CREDENTIAL_VC_TYPE, vc.get(TYPE).get(0).asText());
        JsonNode contraIndicators = vc.get(VC_EVIDENCE).get(0).get(CONTRA_INDICATORS);
        assertEquals(1, contraIndicators.size());
        JsonNode firstCINode = contraIndicators.get(0);
        assertEquals(CI_V_03, firstCINode.get(CODE).asText());
        JsonNode issuers = firstCINode.get(ISSUERS);
        assertEquals(1, issuers.size());
        assertEquals(ISSUERS_TEST, issuers.get(0).asText());
        assertEquals(issuanceDate.toString(), firstCINode.get(ISSUANCE_DATE).asText());
        JsonNode mitigations = firstCINode.get(MITIGATION);
        assertEquals(1, mitigations.size());
        assertEquals(MITIGATION_M_01, mitigations.get(0).get(CODE).asText());
        JsonNode mitigationCredentials = mitigations.get(0).get(MITIGATION_CREDENTIAL);
        assertEquals(0, mitigationCredentials.size());

        ECDSAVerifier verifier = new ECDSAVerifier(ECKey.parse(CIMIT_PUBLIC_JWK));
        assertTrue(signedJWT.verify(verifier));
    }
}
