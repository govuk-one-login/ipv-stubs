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
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.stub.cred.error.ClientAuthenticationException;
import uk.gov.di.ipv.stub.cred.fixtures.TestFixtures;
import uk.gov.di.ipv.stub.cred.utils.StubSsmClient;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.stub.cred.fixtures.TestFixtures.CLIENT_CONFIG;

@ExtendWith(SystemStubsExtension.class)
@ExtendWith(MockitoExtension.class)
public class ClientJwtVerifierTest {
    @SystemStub
    private final EnvironmentVariables environmentVariables =
            new EnvironmentVariables(
                    "CLIENT_AUDIENCE",
                    "https://test-server.example.com/token",
                    "ENVIRONMENT",
                    "TEST");

    @Mock private Context mockContext;

    private ClientJwtVerifier jwtAuthenticationService;

    @BeforeAll
    public static void beforeAllSetUp() {
        StubSsmClient.setClientConfigParams(CLIENT_CONFIG);
    }

    @BeforeEach
    public void setUp() {
        jwtAuthenticationService = new ClientJwtVerifier();
    }

    @Test
    void itShouldNotThrowForValidJwt()
            throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        var validQueryParams =
                getValidQueryParams(generateClientAssertion(getValidClaimsSetValues()));

        when(mockContext.queryParamMap()).thenReturn(validQueryParams);

        assertDoesNotThrow(() -> jwtAuthenticationService.authenticateClient(mockContext));
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

        when(mockContext.queryParamMap()).thenReturn(validQueryParams);

        assertDoesNotThrow(() -> jwtAuthenticationService.authenticateClient(mockContext));
    }

    @Test
    void itShouldThrowIfInvalidSignature() throws Exception {
        var invalidSignatureQueryParams =
                new HashMap<>(
                        getValidQueryParams(generateClientAssertion(getValidClaimsSetValues())));
        String client_assertion = invalidSignatureQueryParams.get("client_assertion").get(0);
        String badSignatureAssertion =
                client_assertion.substring(0, client_assertion.length() - 4) + "nope";
        invalidSignatureQueryParams.put("client_assertion", List.of(badSignatureAssertion));
        when(mockContext.queryParamMap()).thenReturn(invalidSignatureQueryParams);

        ClientAuthenticationException exception =
                assertThrows(
                        ClientAuthenticationException.class,
                        () -> jwtAuthenticationService.authenticateClient(mockContext));

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
        when(mockContext.queryParamMap()).thenReturn(differentIssuerAndSubjectQueryParams);

        ClientAuthenticationException exception =
                assertThrows(
                        ClientAuthenticationException.class,
                        () -> jwtAuthenticationService.authenticateClient(mockContext));

        assertTrue(
                exception
                        .getMessage()
                        .contains(
                                "Bad / expired JWT claims: Issuer and subject JWT claims don't match"));
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

        when(mockContext.queryParamMap()).thenReturn(wrongIssuerAndSubjectQueryParams);

        ClientAuthenticationException exception =
                assertThrows(
                        ClientAuthenticationException.class,
                        () -> jwtAuthenticationService.authenticateClient(mockContext));

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

        when(mockContext.queryParamMap()).thenReturn(wrongAudienceQueryParams);

        ClientAuthenticationException exception =
                assertThrows(
                        ClientAuthenticationException.class,
                        () -> jwtAuthenticationService.authenticateClient(mockContext));

        assertTrue(
                exception
                        .getMessage()
                        .contains(
                                "Bad / expired JWT claims: JWT audience rejected: [NOT_THE_AUDIENCE_YOU_ARE_LOOKING_FOR]"));
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

        when(mockContext.queryParamMap()).thenReturn(expiredQueryParams);

        ClientAuthenticationException exception =
                assertThrows(
                        ClientAuthenticationException.class,
                        () -> jwtAuthenticationService.authenticateClient(mockContext));

        assertTrue(exception.getMessage().contains("Expired JWT"));
    }

    private Map<String, List<String>> getValidQueryParams(String clientAssertion) {
        return new HashMap<>(
                Map.of(
                        "client_assertion",
                        List.of(clientAssertion),
                        "client_assertion_type",
                        List.of("urn:ietf:params:oauth:client-assertion-type:jwt-bearer"),
                        "code",
                        List.of(ResponseType.Value.CODE.getValue()),
                        "grant_type",
                        List.of("authorization_code"),
                        "redirect_uri",
                        List.of("https://test-client.example.com/callback")));
    }

    private Map<String, Object> getValidClaimsSetValues() {
        return Map.of(
                JWTClaimNames.ISSUER, "clientIdValid",
                JWTClaimNames.SUBJECT, "clientIdValid",
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
