package uk.gov.di.ipv.stub.cred.auth;

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
import uk.gov.di.ipv.stub.cred.auth.ClientJwtVerifier;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.error.ClientAuthenticationException;
import uk.gov.di.ipv.stub.cred.fixtures.TestFixtures;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import javax.servlet.http.HttpServletRequest;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
public class ClientJwtVerifierTest {
    @SystemStub
    private final EnvironmentVariables environmentVariables = new EnvironmentVariables(
            "CLIENT_CONFIG", TestFixtures.JWT_AUTH_SERVICE_TEST_CLIENT_CONFIG,
            "CLIENT_AUDIENCE", "https://test-server.example.com/token"
    );

    @Mock private HttpServletRequest mockHttpRequest;

    private ClientJwtVerifier jwtAuthenticationService;

    @BeforeEach
    public void setUp() {
        CredentialIssuerConfig.resetClientConfigs();
        jwtAuthenticationService = new ClientJwtVerifier();
    }

    @Test
    void itShouldNotThrowForValidJwt() throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        var validQueryParams = getValidQueryParams(generateClientAssertion(getValidClaimsSetValues()));

        assertDoesNotThrow(() -> {
            jwtAuthenticationService.authenticateClient(queryMapToString(validQueryParams));
        });
    }

    @Test
    void itShouldThrowIfInvalidSignature() throws Exception {
        var invalidSignatureQueryParams = new HashMap<>(getValidQueryParams(generateClientAssertion(getValidClaimsSetValues())));
        invalidSignatureQueryParams.put("client_assertion", invalidSignatureQueryParams.get("client_assertion") + "BREAKING_THE_SIGNATURE");

        ClientAuthenticationException exception = assertThrows(ClientAuthenticationException.class, () -> {
            jwtAuthenticationService.authenticateClient(queryMapToString(invalidSignatureQueryParams));
        });

        assertTrue(exception.getMessage().contains("InvalidClientException: Bad JWT signature"));
    }

    @Test
    void itShouldThrowIfClaimsSetIssuerAndSubjectAreNotTheSame() throws Exception {
        var differentIssuerAndSubjectClaimsSetValues = new HashMap<>(getValidClaimsSetValues());
        differentIssuerAndSubjectClaimsSetValues.put(JWTClaimNames.ISSUER, "NOT_THE_SAME_AS_SUBJECT");
        var differentIssuerAndSubjectQueryParams = getValidQueryParams(generateClientAssertion(differentIssuerAndSubjectClaimsSetValues));

        ClientAuthenticationException exception = assertThrows(ClientAuthenticationException.class, () -> {
            jwtAuthenticationService.authenticateClient(queryMapToString(differentIssuerAndSubjectQueryParams));
        });

        assertTrue(exception.getMessage().contains("Issuer and subject in client JWT assertion must designate the same client identifier"));
    }

    @Test
    void itShouldThrowIfClientIdParseFromJwtDoesNotMatchConfig() throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        var wrongIssuerAndSubjectClaimsSetValues = new HashMap<>(getValidClaimsSetValues());
        wrongIssuerAndSubjectClaimsSetValues.put(JWTClaimNames.ISSUER, "THIS_BECOMES_THE_CLIENT_ID");
        wrongIssuerAndSubjectClaimsSetValues.put(JWTClaimNames.SUBJECT, "THIS_BECOMES_THE_CLIENT_ID");
        var wrongIssuerAndSubjectQueryParams = getValidQueryParams(generateClientAssertion(wrongIssuerAndSubjectClaimsSetValues));

        ClientAuthenticationException exception = assertThrows(ClientAuthenticationException.class, () -> {
            jwtAuthenticationService.authenticateClient(queryMapToString(wrongIssuerAndSubjectQueryParams));
        });

        assertEquals("Config for client ID 'THIS_BECOMES_THE_CLIENT_ID' not found", exception.getMessage());
    }

    @Test
    void itShouldThrowIfWrongAudience() throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        var wrongAudienceClaimsSetValues = new HashMap<>(getValidClaimsSetValues());
        wrongAudienceClaimsSetValues.put(JWTClaimNames.AUDIENCE, "NOT_THE_AUDIENCE_YOU_ARE_LOOKING_FOR");
        var wrongAudienceQueryParams = getValidQueryParams(generateClientAssertion(wrongAudienceClaimsSetValues));

        ClientAuthenticationException exception = assertThrows(ClientAuthenticationException.class, () -> {
            jwtAuthenticationService.authenticateClient(queryMapToString(wrongAudienceQueryParams));
        });

        assertTrue(exception.getMessage().contains("Invalid JWT audience claim, expected [https://test-server.example.com/token]"));
    }

    @Test
    void itShouldThrowIfClaimsSetHasExpired() throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        var expiredClaimsSetValues = new HashMap<>(getValidClaimsSetValues());
        expiredClaimsSetValues.put(JWTClaimNames.EXPIRATION_TIME, new Date(new Date().getTime() - 61000).getTime() / 1000);
        var expiredQueryParams = getValidQueryParams(generateClientAssertion(expiredClaimsSetValues));

        ClientAuthenticationException exception = assertThrows(ClientAuthenticationException.class, () -> {
            jwtAuthenticationService.authenticateClient(queryMapToString(expiredQueryParams));
        });

        assertTrue(exception.getMessage().contains("Expired JWT"));
    }

    private Map<String, String> getValidQueryParams(String clientAssertion) {
        return Map.of(
                "client_assertion", clientAssertion,
                "client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
                "code", ResponseType.Value.CODE.getValue(),
                "grant_type", "authorization_code",
                "redirect_uri", "https://test-client.example.com/callback"
        );
    }

    private String queryMapToString(Map<String, String> queryParams) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, String> param: queryParams.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(String.format("%s=%s",
                    URLEncoder.encode(param.getKey(), "UTF-8"),
                    URLEncoder.encode(param.getValue(), "UTF-8")
            ));
        }
        return sb.toString();
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
}
