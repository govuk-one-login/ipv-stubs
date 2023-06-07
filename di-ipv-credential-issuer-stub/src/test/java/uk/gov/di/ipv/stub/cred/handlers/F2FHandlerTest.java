package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.fixtures.TestFixtures;
import uk.gov.di.ipv.stub.cred.service.TokenService;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static uk.gov.di.ipv.stub.cred.handlers.AuthorizeHandler.SHARED_CLAIMS;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SystemStubsExtension.class)
public class F2FHandlerTest {
    private static final String JSON_RESPONSE_TYPE = "application/json;charset=UTF-8";
    private static final String VALID_REDIRECT_URI = "https://valid.example.com";
    public static final String VALID_RESPONSE_TYPE = "code";

    @Mock private Response mockResponse;
    @Mock private Request mockRequest;
    @Mock private TokenService mockTokenService;
    private F2FHandler resourceHandler;
    private AccessToken accessToken;

    @SystemStub
    private final EnvironmentVariables environmentVariables =
            new EnvironmentVariables("CLIENT_CONFIG", TestFixtures.CLIENT_CONFIG);

    @BeforeEach
    void setup() {
        CredentialIssuerConfig.resetClientConfigs();
        accessToken = new BearerAccessToken();
        resourceHandler = new F2FHandler(mockTokenService);
    }

    @Test
    public void shouldReturn200AndUserInfoWhenValidRequestReceived() throws Exception {
        QueryParamsMap queryParamsMap = toQueryParamsMap(testGetResourceQueryParams());
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);
        when(mockTokenService.getPayload(accessToken.toAuthorizationHeader()))
                .thenReturn(UUID.randomUUID().toString());
        when(mockRequest.headers("Authorization")).thenReturn(accessToken.toAuthorizationHeader());

        JSONObject jsonResponse =
                JSONObjectUtils.parse(
                        resourceHandler.getResource.handle(mockRequest, mockResponse).toString());

        UserInfo docAppUserInfo = new UserInfo(jsonResponse);

        assertEquals("subject", docAppUserInfo.getSubject().getValue());
        assertEquals(
                "pending",
                docAppUserInfo.getClaim("https://vocab.account.gov.uk/v1/credentialStatus"));

        verify(mockResponse).type(JSON_RESPONSE_TYPE);
        verify(mockResponse).status(HttpServletResponse.SC_ACCEPTED);
        verify(mockTokenService, times(1)).getPayload(accessToken.toAuthorizationHeader());
        verify(mockTokenService).revoke(accessToken.toAuthorizationHeader());
    }

    @Test
    public void shouldReturn400WhenAccessTokenIsNotProvided() throws Exception {
        String result = (String) resourceHandler.getResource.handle(mockRequest, mockResponse);

        assertEquals("Invalid request", result);
        verify(mockResponse).status(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void shouldReturn400WhenIssuedAccessTokenDoesNotMatchRequestAccessToken()
            throws Exception {
        when(mockRequest.headers("Authorization")).thenReturn(accessToken.toAuthorizationHeader());

        when(mockTokenService.getPayload(accessToken.toAuthorizationHeader())).thenReturn(null);

        String result = (String) resourceHandler.getResource.handle(mockRequest, mockResponse);

        assertEquals("Client authentication failed", result);
        verify(mockResponse).status(HttpServletResponse.SC_UNAUTHORIZED);
        verify(mockTokenService).getPayload(accessToken.toAuthorizationHeader());
    }

    @Test
    public void shouldReturn400WhenRequestAccessTokenIsNotValid() throws Exception {
        when(mockRequest.headers("Authorization")).thenReturn("invalid-token");

        String result = (String) resourceHandler.getResource.handle(mockRequest, mockResponse);

        assertEquals("Client authentication failed", result);
        verify(mockResponse).status(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private QueryParamsMap toQueryParamsMap(Map<String, String[]> queryParams) {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
        when(mockHttpRequest.getParameterMap()).thenReturn(queryParams);
        return new QueryParamsMap(mockHttpRequest);
    }

    private Map<String, String[]> testGetResourceQueryParams() throws Exception {
        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.REQUESTED_OAUTH_ERROR, new String[] {"none"});
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[] {"clientIdValid"});
        queryParams.put(
                RequestParamConstants.REQUEST,
                new String[] {
                    signedRequestJwt(validRequestJWT(VALID_RESPONSE_TYPE, VALID_REDIRECT_URI))
                            .serialize()
                });
        return queryParams;
    }

    private SignedJWT signedRequestJwt(JWTClaimsSet claimsSet) throws Exception {
        ECDSASigner ecdsaSigner = new ECDSASigner(getPrivateKey());
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.ES256), claimsSet);
        signedJWT.sign(ecdsaSigner);

        return signedJWT;
    }

    private ECPrivateKey getPrivateKey() throws InvalidKeySpecException, NoSuchAlgorithmException {
        return (ECPrivateKey)
                KeyFactory.getInstance("EC")
                        .generatePrivate(
                                new PKCS8EncodedKeySpec(
                                        Base64.getDecoder().decode(TestFixtures.EC_PRIVATE_KEY_1)));
    }

    private JWTClaimsSet validRequestJWT(String responseType, String redirectUri) {
        Instant instant = Instant.now();

        Map<String, Object> sharedClaims = new LinkedHashMap<>();
        sharedClaims.put("addresses", Collections.singletonList("123 random street, M13 7GE"));
        sharedClaims.put(
                "names",
                List.of(
                        Map.of(
                                "nameParts",
                                Arrays.asList(
                                        Map.of("value", "Test"),
                                        Map.of("value", "Testing"),
                                        Map.of("value", "Tested")))));
        sharedClaims.put("birthDate", List.of(Map.of("value", "01/01/1980")));

        return new JWTClaimsSet.Builder()
                .issuer("issuer")
                .audience("audience")
                .subject("subject")
                .claim("redirect_uri", redirectUri)
                .claim("response_type", responseType)
                .claim("state", "test-state")
                .expirationTime(Date.from(instant.plus(1L, ChronoUnit.HOURS)))
                .notBeforeTime(Date.from(instant))
                .issueTime(Date.from(instant))
                .claim(SHARED_CLAIMS, sharedClaims)
                .build();
    }
}
