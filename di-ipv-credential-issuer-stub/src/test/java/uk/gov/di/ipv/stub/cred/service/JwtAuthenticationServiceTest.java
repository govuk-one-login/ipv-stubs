package uk.gov.di.ipv.stub.cred.service;

import com.google.gson.Gson;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ResponseType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import spark.QueryParamsMap;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.error.ClientAuthenticationException;
import uk.gov.di.ipv.stub.cred.fixtures.TestFixtures;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import javax.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(SystemStubsExtension.class)
@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationServiceTest {
    @SystemStub
    private final EnvironmentVariables environmentVariables = new EnvironmentVariables(
            "CLIENT_CONFIG", TestFixtures.JWT_AUTH_SERVICE_TEST_CLIENT_CONFIG,
            "CLIENT_AUDIENCE", "https://test-server.example.com/token"
    );

    @Mock private HttpServletRequest mockHttpRequest;

    private final JwtAuthenticationService jwtAuthenticationService = new JwtAuthenticationService();

    @BeforeEach
    public void setUp() {
        CredentialIssuerConfig.resetClientConfigs();
    }

    @Test
    void itShouldNotThrowForValidJwt() throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        var validQueryParams = getValidQueryParams(generateClientAssertion(getValidClaimsSetValues()));

        when(mockHttpRequest.getParameterMap()).thenReturn(validQueryParams);

        assertDoesNotThrow(() -> {
            jwtAuthenticationService.authenticateClient(new QueryParamsMap(mockHttpRequest));
        });
    }

    @Test
    void itShouldThrowIfInvalidSignature() throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        var invalidSignatureQueryParams = getValidQueryParams(generateClientAssertion(getValidClaimsSetValues()));
        invalidSignatureQueryParams.get("client_assertion")[0] += "BREAKING_THE_SIGNATURE";
        when(mockHttpRequest.getParameterMap()).thenReturn(invalidSignatureQueryParams);

        ClientAuthenticationException exception = assertThrows(ClientAuthenticationException.class, () -> {
            jwtAuthenticationService.authenticateClient(new QueryParamsMap(mockHttpRequest));
        });

        assertEquals("Failed to verify client authentication JWT signature", exception.getMessage());
    }

    @Test
    void itShouldThrowIfClaimsSetIssuerAndSubjectAreNotTheSame() throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        var differentIssuerAndSubjectClaimsSetValues = new HashMap<>(getValidClaimsSetValues());
        differentIssuerAndSubjectClaimsSetValues.put(JWTClaimNames.ISSUER, "NOT_THE_SAME_AS_SUBJECT");
        var differentIssuerAndSubjectQueryParams = getValidQueryParams(generateClientAssertion(differentIssuerAndSubjectClaimsSetValues));

        when(mockHttpRequest.getParameterMap()).thenReturn(differentIssuerAndSubjectQueryParams);

        ClientAuthenticationException exception = assertThrows(ClientAuthenticationException.class, () -> {
            jwtAuthenticationService.authenticateClient(new QueryParamsMap(mockHttpRequest));
        });

        assertTrue(exception.getMessage().contains("Issuer and subject in client JWT assertion must designate the same client identifier"));
    }

    @Test
    void itShouldThrowIfClientIdParseFromJwtDoesNotMatchConfig() throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        var wrongIssuerAndSubjectClaimsSetValues = new HashMap<>(getValidClaimsSetValues());
        wrongIssuerAndSubjectClaimsSetValues.put(JWTClaimNames.ISSUER, "THIS_BECOMES_THE_CLIENT_ID");
        wrongIssuerAndSubjectClaimsSetValues.put(JWTClaimNames.SUBJECT, "THIS_BECOMES_THE_CLIENT_ID");
        var wrongIssuerAndSubjectQueryParams = getValidQueryParams(generateClientAssertion(wrongIssuerAndSubjectClaimsSetValues));

        when(mockHttpRequest.getParameterMap()).thenReturn(wrongIssuerAndSubjectQueryParams);

        ClientAuthenticationException exception = assertThrows(ClientAuthenticationException.class, () -> {
            jwtAuthenticationService.authenticateClient(new QueryParamsMap(mockHttpRequest));
        });

        assertEquals("Config for client ID 'THIS_BECOMES_THE_CLIENT_ID' not found", exception.getMessage());
    }

    @Test
    void itShouldThrowIfIssuerInConfigDoesNotMatchClaimsSet() throws Exception {
        var validQueryParams = getValidQueryParams(generateClientAssertion(getValidClaimsSetValues()));
        when(mockHttpRequest.getParameterMap()).thenReturn(validQueryParams);

        new EnvironmentVariables("CLIENT_CONFIG", getModifiedJwtAuthenticationConfigValue("issuer", "NON_MATCHING_ISSUER"))
                .execute(() -> {
                    CredentialIssuerConfig.resetClientConfigs();
                    ClientAuthenticationException exception = assertThrows(ClientAuthenticationException.class, () -> {
                        jwtAuthenticationService.authenticateClient(new QueryParamsMap(mockHttpRequest));
                    });
                    assertEquals("Client auth claims set failed validation for 'issuer'. Expected: 'NON_MATCHING_ISSUER'. Received: 'aTestClient'", exception.getMessage());
                });
        CredentialIssuerConfig.resetClientConfigs();
    }

    @Test
    void itShouldThrowIfSubjectInConfigDoesNotMatchClaimsSet() throws Exception {
        var validQueryParams = getValidQueryParams(generateClientAssertion(getValidClaimsSetValues()));
        when(mockHttpRequest.getParameterMap()).thenReturn(validQueryParams);

        new EnvironmentVariables("CLIENT_CONFIG", getModifiedJwtAuthenticationConfigValue("subject", "NON_MATCHING_SUBJECT"))
                .execute(() -> {
                    CredentialIssuerConfig.resetClientConfigs();
                    ClientAuthenticationException exception = assertThrows(ClientAuthenticationException.class, () -> {
                        jwtAuthenticationService.authenticateClient(new QueryParamsMap(mockHttpRequest));
                    });

                    assertEquals("Client auth claims set failed validation for 'subject'. Expected: 'NON_MATCHING_SUBJECT'. Received: 'aTestClient'", exception.getMessage());
                });
        CredentialIssuerConfig.resetClientConfigs();
    }

    @Test
    void itShouldThrowIfWrongAudience() throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        var wrongAudienceClaimsSetValues = new HashMap<>(getValidClaimsSetValues());
        wrongAudienceClaimsSetValues.put(JWTClaimNames.AUDIENCE, "NOT_THE_AUDIENCE_YOU_ARE_LOOKING_FOR");
        var wrongAudienceQueryParams = getValidQueryParams(generateClientAssertion(wrongAudienceClaimsSetValues));
        when(mockHttpRequest.getParameterMap()).thenReturn(wrongAudienceQueryParams);

        ClientAuthenticationException exception = assertThrows(ClientAuthenticationException.class, () -> {
            jwtAuthenticationService.authenticateClient(new QueryParamsMap(mockHttpRequest));
        });

        assertEquals(
                "Invalid audience in claims set. Expected: 'https://test-server.example.com/token'. Received '[NOT_THE_AUDIENCE_YOU_ARE_LOOKING_FOR]'",
                exception.getMessage()
        );
    }

    @Test
    void itShouldThrowIfClaimsSetHasExpired() throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        var expiredClaimsSetValues = new HashMap<>(getValidClaimsSetValues());
        expiredClaimsSetValues.put(JWTClaimNames.EXPIRATION_TIME, new Date(new Date().getTime() - 1000).getTime() / 1000);
        var expiredQueryParams = getValidQueryParams(generateClientAssertion(expiredClaimsSetValues));
        when(mockHttpRequest.getParameterMap()).thenReturn(expiredQueryParams);

        ClientAuthenticationException exception = assertThrows(ClientAuthenticationException.class, () -> {
            jwtAuthenticationService.authenticateClient(new QueryParamsMap(mockHttpRequest));
        });

        assertEquals(
                "Expiration date in client auth claims set has passed",
                exception.getMessage()
        );
    }

    private Map<String, String[]> getValidQueryParams(String clientAssertion) {
        return Map.of(
                "client_assertion", new String[]{clientAssertion},
                "client_assertion_type", new String[]{"urn:ietf:params:oauth:client-assertion-type:jwt-bearer"},
                "code", new String[]{ResponseType.Value.CODE.getValue()},
                "grant_type", new String[]{"authorization_code"},
                "redirect_uri", new String[]{"https://test-client.example.com/callback"}
        );
    }

    private Map<String, Object> getValidClaimsSetValues() {
        return Map.of(
                JWTClaimNames.ISSUER, "aTestClient",
                JWTClaimNames.SUBJECT, "aTestClient",
                JWTClaimNames.AUDIENCE, "https://test-server.example.com/token",
                JWTClaimNames.EXPIRATION_TIME, fifteenMinutesFromNow()
        );
    }

    private static long fifteenMinutesFromNow() {
        return new Date(new Date().getTime() + (15 * 60 * 1000)).getTime();
    }

    private String generateClientAssertion(Map<String, Object> claimsSetValues) throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        RSASSASigner signer = new RSASSASigner((KeyFactory.getInstance("RSA")
                .generatePrivate(
                        new PKCS8EncodedKeySpec(Base64.getDecoder().decode(TestFixtures.JWT_AUTH_SERVICE_TEST_CLIENT_CONFIG_PRIVATE_KEY)))));

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(),
                generateClaimsSet(claimsSetValues)
        );
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

    private JWTClaimsSet generateClaimsSet(Map<String, Object> claimsSetValues) {
        return new JWTClaimsSet.Builder()
                .claim(JWTClaimNames.ISSUER, claimsSetValues.get(JWTClaimNames.ISSUER))
                .claim(JWTClaimNames.SUBJECT, claimsSetValues.get(JWTClaimNames.SUBJECT))
                .claim(JWTClaimNames.AUDIENCE, claimsSetValues.get(JWTClaimNames.AUDIENCE))
                .claim(JWTClaimNames.EXPIRATION_TIME, claimsSetValues.get(JWTClaimNames.EXPIRATION_TIME))
                .build();
    }

    private String getModifiedJwtAuthenticationConfigValue(String key, String value) {
        String validConfig = TestFixtures.JWT_AUTH_SERVICE_TEST_CLIENT_CONFIG;
        String jsonString = new String(Base64.getDecoder().decode(validConfig));
        Gson gson = new Gson();
        Map<String, Object> configMap = gson.fromJson(jsonString, Map.class);
        Map<String, Object> aTestClientConfig = (Map<String, Object>) configMap.get("aTestClient");
        Map<String, String> jwtAuthenticationConfig = (Map<String, String>) aTestClientConfig.get("jwtAuthentication");
        jwtAuthenticationConfig.put(key, value);
        return Base64.getEncoder().encodeToString(gson.toJson(configMap).getBytes(StandardCharsets.UTF_8));
    }
}
