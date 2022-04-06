package uk.gov.di.ipv.stub.cred.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.impl.ECDSA;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ResponseType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spark.QueryParamsMap;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.error.ClientAuthenticationException;
import uk.gov.di.ipv.stub.cred.fixtures.TestFixtures;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import javax.servlet.http.HttpServletRequest;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(SystemStubsExtension.class)
@ExtendWith(MockitoExtension.class)
public class ClientEs256SignatureVerifierTest {
    @SystemStub
    private final EnvironmentVariables environmentVariables =
            new EnvironmentVariables(
                    "CLIENT_CONFIG",
                    TestFixtures.CLIENT_CONFIG_WITH_PUBLIC_JWK,
                    "CLIENT_AUDIENCE",
                    "https://test-server.example.com/token");

    @Mock private HttpServletRequest mockHttpRequest;

    private ClientJwtVerifier jwtAuthenticationService;

    @BeforeEach
    public void setUp() {
        CredentialIssuerConfig.resetClientConfigs();
        jwtAuthenticationService = new ClientJwtVerifier();
    }

    @Test
    void itShouldNotThrowForValidJwt()
            throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        var validQueryParams =
                getValidQueryParams(generateClientAssertion(getValidClaimsSetValues()));

        when(mockHttpRequest.getParameterMap()).thenReturn(validQueryParams);

        assertDoesNotThrow(
                () -> {
                    jwtAuthenticationService.authenticateClient(
                            new QueryParamsMap(mockHttpRequest));
                });
    }

    @Test
    void itShouldNotThrowForJwtWithDerEncodedSignature() throws Exception {
        SignedJWT signedJwt = SignedJWT.parse(generateClientAssertion(getValidClaimsSetValues()));
        String[] jwtParts = signedJwt.serialize().split("\\.");
        Base64URL derSignature =
                Base64URL.encode(ECDSA.transcodeSignatureToDER(signedJwt.getSignature().decode()));
        SignedJWT derSignatureJwt =
                SignedJWT.parse(String.format("%s.%s.%s", jwtParts[0], jwtParts[1], derSignature));
        var validQueryParams = getValidQueryParams(derSignatureJwt.serialize());

        when(mockHttpRequest.getParameterMap()).thenReturn(validQueryParams);

        assertDoesNotThrow(
                () -> {
                    jwtAuthenticationService.authenticateClient(
                            new QueryParamsMap(mockHttpRequest));
                });
    }

    @Test
    void itShouldThrowIfInvalidSignature() throws Exception {
        var invalidSignatureQueryParams =
                new HashMap<>(
                        getValidQueryParams(generateClientAssertion(getValidClaimsSetValues())));
        String client_assertion = invalidSignatureQueryParams.get("client_assertion")[0];
        String badSignatureAssertion =
                client_assertion.substring(0, client_assertion.length() - 4) + "nope";
        invalidSignatureQueryParams.get("client_assertion")[0] = badSignatureAssertion;
        when(mockHttpRequest.getParameterMap()).thenReturn(invalidSignatureQueryParams);

        ClientAuthenticationException exception =
                assertThrows(
                        ClientAuthenticationException.class,
                        () -> {
                            jwtAuthenticationService.authenticateClient(
                                    new QueryParamsMap(mockHttpRequest));
                        });

        assertTrue(exception.getMessage().contains("InvalidClientException: Bad JWT signature"));
    }

    @Test
    void itShouldThrowIfClaimsSetIssuerAndSubjectAreNotTheSame() throws Exception {
        var differentIssuerAndSubjectClaimsSetValues = new HashMap<>(getValidClaimsSetValues());
        differentIssuerAndSubjectClaimsSetValues.put(
                JWTClaimNames.ISSUER, "NOT_THE_SAME_AS_SUBJECT");
        var differentIssuerAndSubjectQueryParams =
                getValidQueryParams(
                        generateClientAssertion(differentIssuerAndSubjectClaimsSetValues));
        when(mockHttpRequest.getParameterMap()).thenReturn(differentIssuerAndSubjectQueryParams);

        ClientAuthenticationException exception =
                assertThrows(
                        ClientAuthenticationException.class,
                        () -> {
                            jwtAuthenticationService.authenticateClient(
                                    new QueryParamsMap(mockHttpRequest));
                        });

        assertTrue(
                exception
                        .getMessage()
                        .contains(
                                "Issuer and subject in client JWT assertion must designate the same client identifier"));
    }

    @Test
    void itShouldThrowIfClientIdParseFromJwtDoesNotMatchConfig()
            throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        var wrongIssuerAndSubjectClaimsSetValues = new HashMap<>(getValidClaimsSetValues());
        wrongIssuerAndSubjectClaimsSetValues.put(
                JWTClaimNames.ISSUER, "THIS_BECOMES_THE_CLIENT_ID");
        wrongIssuerAndSubjectClaimsSetValues.put(
                JWTClaimNames.SUBJECT, "THIS_BECOMES_THE_CLIENT_ID");
        var wrongIssuerAndSubjectQueryParams =
                getValidQueryParams(generateClientAssertion(wrongIssuerAndSubjectClaimsSetValues));

        when(mockHttpRequest.getParameterMap()).thenReturn(wrongIssuerAndSubjectQueryParams);

        ClientAuthenticationException exception =
                assertThrows(
                        ClientAuthenticationException.class,
                        () -> {
                            jwtAuthenticationService.authenticateClient(
                                    new QueryParamsMap(mockHttpRequest));
                        });

        assertEquals(
                "Config for client ID 'THIS_BECOMES_THE_CLIENT_ID' not found",
                exception.getMessage());
    }

    @Test
    void itShouldThrowIfWrongAudience()
            throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        var wrongAudienceClaimsSetValues = new HashMap<>(getValidClaimsSetValues());
        wrongAudienceClaimsSetValues.put(
                JWTClaimNames.AUDIENCE, "NOT_THE_AUDIENCE_YOU_ARE_LOOKING_FOR");
        var wrongAudienceQueryParams =
                getValidQueryParams(generateClientAssertion(wrongAudienceClaimsSetValues));

        when(mockHttpRequest.getParameterMap()).thenReturn(wrongAudienceQueryParams);

        ClientAuthenticationException exception =
                assertThrows(
                        ClientAuthenticationException.class,
                        () -> {
                            jwtAuthenticationService.authenticateClient(
                                    new QueryParamsMap(mockHttpRequest));
                        });

        assertTrue(
                exception
                        .getMessage()
                        .contains(
                                "Invalid JWT audience claim, expected [https://test-server.example.com/token]"));
    }

    @Test
    void itShouldThrowIfClaimsSetHasExpired()
            throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        var expiredClaimsSetValues = new HashMap<>(getValidClaimsSetValues());
        expiredClaimsSetValues.put(
                JWTClaimNames.EXPIRATION_TIME,
                new Date(new Date().getTime() - 61000).getTime() / 1000);
        var expiredQueryParams =
                getValidQueryParams(generateClientAssertion(expiredClaimsSetValues));

        when(mockHttpRequest.getParameterMap()).thenReturn(expiredQueryParams);

        ClientAuthenticationException exception =
                assertThrows(
                        ClientAuthenticationException.class,
                        () -> {
                            jwtAuthenticationService.authenticateClient(
                                    new QueryParamsMap(mockHttpRequest));
                        });

        assertTrue(exception.getMessage().contains("Expired JWT"));
    }

    private Map<String, String[]> getValidQueryParams(String clientAssertion) {
        return Map.of(
                "client_assertion", new String[] {clientAssertion},
                "client_assertion_type",
                        new String[] {"urn:ietf:params:oauth:client-assertion-type:jwt-bearer"},
                "code", new String[] {ResponseType.Value.CODE.getValue()},
                "grant_type", new String[] {"authorization_code"},
                "redirect_uri", new String[] {"https://test-client.example.com/callback"});
    }

    private Map<String, Object> getValidClaimsSetValues() {
        return Map.of(
                JWTClaimNames.ISSUER, "aTestClient",
                JWTClaimNames.SUBJECT, "aTestClient",
                JWTClaimNames.AUDIENCE, "https://test-server.example.com/token",
                JWTClaimNames.EXPIRATION_TIME, fifteenMinutesFromNow());
    }

    private static long fifteenMinutesFromNow() {
        return new Date(new Date().getTime() + (15 * 60 * 1000)).getTime();
    }

    private String generateClientAssertion(Map<String, Object> claimsSetValues)
            throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        ECDSASigner signer =
                new ECDSASigner(
                        (KeyFactory.getInstance("EC")
                                .generatePrivate(
                                        new PKCS8EncodedKeySpec(
                                                Base64.getDecoder()
                                                        .decode(TestFixtures.EC_PRIVATE_KEY_1)))),
                        Curve.P_256);

        SignedJWT signedJWT =
                new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),
                        generateClaimsSet(claimsSetValues));
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

    private JWTClaimsSet generateClaimsSet(Map<String, Object> claimsSetValues) {
        return new JWTClaimsSet.Builder()
                .claim(JWTClaimNames.ISSUER, claimsSetValues.get(JWTClaimNames.ISSUER))
                .claim(JWTClaimNames.SUBJECT, claimsSetValues.get(JWTClaimNames.SUBJECT))
                .claim(JWTClaimNames.AUDIENCE, claimsSetValues.get(JWTClaimNames.AUDIENCE))
                .claim(
                        JWTClaimNames.EXPIRATION_TIME,
                        claimsSetValues.get(JWTClaimNames.EXPIRATION_TIME))
                .build();
    }
}
