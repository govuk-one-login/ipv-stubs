package uk.gov.di.ipv.core.getcontraindicatorcredential;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
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
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.core.getcontraindicatorcredential.GetContraIndicatorCredentialHandler.CODE;
import static uk.gov.di.ipv.core.getcontraindicatorcredential.GetContraIndicatorCredentialHandler.CONTRA_INDICATORS;
import static uk.gov.di.ipv.core.getcontraindicatorcredential.GetContraIndicatorCredentialHandler.ISSUANCE_DATE;
import static uk.gov.di.ipv.core.getcontraindicatorcredential.GetContraIndicatorCredentialHandler.MITIGATION;
import static uk.gov.di.ipv.core.getcontraindicatorcredential.GetContraIndicatorCredentialHandler.MITIGATION_CREDENTIAL;
import static uk.gov.di.ipv.core.getcontraindicatorcredential.GetContraIndicatorCredentialHandler.SECURITY_CHECK_CREDENTIAL_VC_TYPE;
import static uk.gov.di.ipv.core.getcontraindicatorcredential.GetContraIndicatorCredentialHandler.TYPE;
import static uk.gov.di.ipv.core.library.vc.VerifiableCredentialConstants.VC_CLAIM;
import static uk.gov.di.ipv.core.library.vc.VerifiableCredentialConstants.VC_EVIDENCE;

@ExtendWith({SystemStubsExtension.class, MockitoExtension.class})
class GetContraIndicatorCredentialHandlerTest {

    public static final String USER_ID = "user_id";
    public static final String CI_V_03 = "V03";
    public static final String MITIGATION_M_01 = "M01";
    private static final String CIMIT_PRIVATE_KEY =
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgOXt0P05ZsQcK7eYusgIPsqZdaBCIJiW4imwUtnaAthWhRANCAAQT1nO46ipxVTilUH2umZPN7OPI49GU6Y8YkcqLxFKUgypUzGbYR2VJGM+QJXk0PI339EyYkt6tjgfS+RcOMQNO";
    private static final String CIMIT_PUBLIC_JWK =
            "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"E9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIM\",\"y\":\"KlTMZthHZUkYz5AleTQ8jff0TJiS3q2OB9L5Fw4xA04\"}";

    private static final String CIMIT_COMPONENT_ID = "https://cimit.stubs.account.gov.uk";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private Context mockContext;
    @Mock private ConfigService mockConfigService;
    @Mock private CimitStubItemService mockCimitStubItemService;
    @InjectMocks private GetContraIndicatorCredentialHandler classToTest;

    @SystemStub
    private final EnvironmentVariables environmentVariables =
            new EnvironmentVariables("CIMIT_COMPONENT_ID", "https://cimit.stubs.account.gov.uk");

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

        var response =
                makeRequest(
                        classToTest,
                        objectMapper.writeValueAsString(getCiCredentialRequest),
                        mockContext,
                        GetCiCredentialResponse.class);

        verify(mockConfigService).getCimitSigningKey();
        verify(mockConfigService).getCimitComponentId();

        assertNotNull(response);
        assertTrue(!response.equals("Failure"));

        final String contraIndicatorsVC =
                new String(response.getVc().getBytes(), StandardCharsets.UTF_8);

        assertClaimsJWTIsValid(contraIndicatorsVC, issuanceDate);
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

        var response =
                makeRequest(
                        classToTest,
                        objectMapper.writeValueAsString(getCiCredentialRequest),
                        mockContext,
                        GetCiCredentialResponse.class);

        verify(mockConfigService).getCimitSigningKey();
        verify(mockConfigService).getCimitComponentId();

        assertNotNull(response);
        assertTrue(response.getVc().equals("Failure"));
    }

    private <T extends GetCiCredentialResponse> T makeRequest(
            RequestStreamHandler handler, String request, Context context, Class<T> classType)
            throws IOException {
        try (var inputStream = new ByteArrayInputStream(request.getBytes());
                var outputStream = new ByteArrayOutputStream()) {
            handler.handleRequest(inputStream, outputStream, context);
            return objectMapper.readValue(outputStream.toString(), classType);
        }
    }

    private void assertClaimsJWTIsValid(String request, Instant issuanceDate)
            throws ParseException, JsonProcessingException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(request);
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
