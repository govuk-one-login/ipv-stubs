package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import uk.gov.di.ipv.stub.cred.auth.ClientJwtVerifier;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.error.ClientAuthenticationException;
import uk.gov.di.ipv.stub.cred.fixtures.TestFixtures;
import uk.gov.di.ipv.stub.cred.service.AuthCodeService;
import uk.gov.di.ipv.stub.cred.service.TokenService;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;
import uk.gov.di.ipv.stub.cred.validation.Validator;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.nimbusds.oauth2.sdk.OAuth2Error.INVALID_CLIENT_CODE;
import static com.nimbusds.oauth2.sdk.OAuth2Error.INVALID_GRANT_CODE;
import static com.nimbusds.oauth2.sdk.OAuth2Error.INVALID_REQUEST_CODE;
import static com.nimbusds.oauth2.sdk.auth.JWTAuthentication.CLIENT_ASSERTION_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SystemStubsExtension.class)
public class TokenHandlerTest {
    private static final String TEST_REDIRECT_URI = "https://example.com";
    private static final String TEST_AUTH_CODE = "e2Ln9Vs6bwZ1mDM8gfl256hg8I88i8LLenVfqxKuDEg";

    @Mock private Response mockResponse;
    @Mock private Request mockRequest;
    @Mock private TokenService mockTokenService;
    @Mock private AuthCodeService mockAuthCodeService;
    @Mock private Validator mockValidator;
    @Mock private ClientJwtVerifier mockJwtAuthenticationService;
    @InjectMocks private TokenHandler tokenHandler;

    @SystemStub
    private final EnvironmentVariables environmentVariables =
            new EnvironmentVariables(
                    "CLIENT_CONFIG",
                    TestFixtures.NO_AUTHENTICATION_CLIENT_CONFIG,
                    "CLIENT_AUDIENCE",
                    "https://test-server.example.com/token");

    @BeforeEach
    public void setUp() {
        CredentialIssuerConfig.resetClientConfigs();
    }

    @Test
    void shouldIssueAccessTokenWhenValidRequestReceivedUsingJwtClientAuthentication()
            throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
        String resourceId = UUID.randomUUID().toString();

        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(
                RequestParamConstants.GRANT_TYPE,
                new String[] {GrantType.AUTHORIZATION_CODE.getValue()});
        queryParams.put(
                RequestParamConstants.CLIENT_ASSERTION, new String[] {"a-client-assertion"});
        queryParams.put(
                RequestParamConstants.CLIENT_ASSERTION_TYPE, new String[] {CLIENT_ASSERTION_TYPE});
        queryParams.put(RequestParamConstants.REDIRECT_URI, new String[] {TEST_REDIRECT_URI});
        queryParams.put(RequestParamConstants.AUTH_CODE, new String[] {TEST_AUTH_CODE});
        when(mockHttpRequest.getParameterMap()).thenReturn(queryParams);

        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);
        when(mockValidator.validateTokenRequest(any()))
                .thenReturn(ValidationResult.createValidResult());
        when(mockValidator.validateRedirectUrlsMatch(anyString(), anyString()))
                .thenReturn(ValidationResult.createValidResult());
        when(mockAuthCodeService.getPayload(TEST_AUTH_CODE)).thenReturn(resourceId);
        when(mockAuthCodeService.getRedirectUrl(TEST_AUTH_CODE)).thenReturn(TEST_REDIRECT_URI);
        when(mockTokenService.createBearerAccessToken()).thenReturn(new BearerAccessToken());

        String result = (String) tokenHandler.issueAccessToken.handle(mockRequest, mockResponse);

        assertNotNull(result);
        verify(mockResponse).status(HttpServletResponse.SC_OK);
        verify(mockResponse).type("application/json;charset=UTF-8");

        verify(mockJwtAuthenticationService).authenticateClient(any());
        verify(mockAuthCodeService).getPayload(TEST_AUTH_CODE);
        verify(mockAuthCodeService).revoke(TEST_AUTH_CODE);
        verify(mockTokenService).persist(any(AccessToken.class), eq(resourceId));

        HTTPResponse response = new HTTPResponse(HttpServletResponse.SC_OK);
        response.setContent(result);
        response.setContentType("application/json;charset=UTF-8");
        AccessTokenResponse accessTokenResponse = AccessTokenResponse.parse(response);

        assertNotNull(accessTokenResponse.getTokens().getAccessToken());
        assertNotNull(accessTokenResponse.getTokens().getRefreshToken());
        assertEquals(
                AccessTokenType.BEARER, accessTokenResponse.getTokens().getAccessToken().getType());
    }

    @Test
    void shouldIssueAccessTokenWhenValidRequestReceivedWithClientId() throws Exception {
        // Having the client_id provided in the params implies the client is not using JWT
        // authentication
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);

        String resourceId = UUID.randomUUID().toString();

        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(
                RequestParamConstants.GRANT_TYPE,
                new String[] {GrantType.AUTHORIZATION_CODE.getValue()});
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[] {"noAuthenticationClient"});
        queryParams.put(RequestParamConstants.REDIRECT_URI, new String[] {TEST_REDIRECT_URI});
        queryParams.put(RequestParamConstants.AUTH_CODE, new String[] {TEST_AUTH_CODE});
        when(mockHttpRequest.getParameterMap()).thenReturn(queryParams);

        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);
        when(mockValidator.validateTokenRequest(any()))
                .thenReturn(ValidationResult.createValidResult());
        when(mockValidator.validateRedirectUrlsMatch(anyString(), anyString()))
                .thenReturn(ValidationResult.createValidResult());
        when(mockAuthCodeService.getPayload(TEST_AUTH_CODE)).thenReturn(resourceId);
        when(mockAuthCodeService.getRedirectUrl(TEST_AUTH_CODE)).thenReturn(TEST_REDIRECT_URI);
        when(mockTokenService.createBearerAccessToken()).thenReturn(new BearerAccessToken());

        String result = (String) tokenHandler.issueAccessToken.handle(mockRequest, mockResponse);

        assertNotNull(result);
        verify(mockResponse).status(HttpServletResponse.SC_OK);
        verify(mockResponse).type("application/json;charset=UTF-8");

        verify(mockAuthCodeService).getPayload(TEST_AUTH_CODE);
        verify(mockAuthCodeService).revoke(TEST_AUTH_CODE);
        verify(mockTokenService).persist(any(AccessToken.class), eq(resourceId));

        HTTPResponse response = new HTTPResponse(HttpServletResponse.SC_OK);
        response.setContent(result);
        response.setContentType("application/json;charset=UTF-8");
        AccessTokenResponse accessTokenResponse = AccessTokenResponse.parse(response);

        assertNotNull(accessTokenResponse.getTokens().getAccessToken());
        assertNotNull(accessTokenResponse.getTokens().getRefreshToken());
        assertEquals(
                AccessTokenType.BEARER, accessTokenResponse.getTokens().getAccessToken().getType());
    }

    @Test
    void shouldReturnCorrectErrorResponseIfTokenRequestFailsValidation() throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
        when(mockHttpRequest.getParameterMap()).thenReturn(new HashMap<>());

        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        when(mockValidator.validateTokenRequest(any()))
                .thenReturn(new ValidationResult(false, OAuth2Error.INVALID_CLIENT));

        String result = (String) tokenHandler.issueAccessToken.handle(mockRequest, mockResponse);

        verify(mockResponse).type("application/json;charset=UTF-8");
        verify(mockResponse).status(HTTPResponse.SC_UNAUTHORIZED);

        ErrorObject resultantErrorObject =
                createErrorFromResult(HTTPResponse.SC_UNAUTHORIZED, result);

        assertEquals(INVALID_CLIENT_CODE, resultantErrorObject.getCode());
        assertEquals("Client authentication failed", resultantErrorObject.getDescription());
    }

    @Test
    void shouldReturn401IfJwtAuthenticationFails() throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
        when(mockHttpRequest.getParameterMap()).thenReturn(new HashMap<>());
        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);
        when(mockValidator.validateTokenRequest(any()))
                .thenReturn(ValidationResult.createValidResult());
        doThrow(new ClientAuthenticationException("Fail."))
                .when(mockJwtAuthenticationService)
                .authenticateClient(any());

        String result = (String) tokenHandler.issueAccessToken.handle(mockRequest, mockResponse);

        verify(mockResponse).status(OAuth2Error.INVALID_CLIENT.getHTTPStatusCode());

        ErrorObject resultantErrorObject =
                createErrorFromResult(OAuth2Error.INVALID_CLIENT.getHTTPStatusCode(), result);

        assertEquals(INVALID_CLIENT_CODE, resultantErrorObject.getCode());
        assertEquals("Client authentication failed", resultantErrorObject.getDescription());
    }

    @Test
    void shouldReturn400IfClientConfigureForAuthenticationProvidesClientId() throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
        when(mockHttpRequest.getParameterMap())
                .thenReturn(
                        Map.of(RequestParamConstants.CLIENT_ID, new String[] {"clientIdValid"}));

        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);
        when(mockValidator.validateTokenRequest(any()))
                .thenReturn(ValidationResult.createValidResult());
        new EnvironmentVariables("CLIENT_CONFIG", TestFixtures.CLIENT_CONFIG)
                .execute(
                        () -> {
                            CredentialIssuerConfig.resetClientConfigs();

                            String result =
                                    (String)
                                            tokenHandler.issueAccessToken.handle(
                                                    mockRequest, mockResponse);

                            verify(mockResponse).status(HTTPResponse.SC_BAD_REQUEST);

                            ErrorObject resultantErrorObject =
                                    createErrorFromResult(HTTPResponse.SC_BAD_REQUEST, result);

                            assertEquals(INVALID_REQUEST_CODE, resultantErrorObject.getCode());
                            assertEquals("Invalid request", resultantErrorObject.getDescription());
                        });
    }

    @Test
    void shouldReturn400ResponseWhenRedirectUrlsDoNotMatch() throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
        when(mockHttpRequest.getParameterMap())
                .thenReturn(
                        Map.of(
                                RequestParamConstants.CLIENT_ID,
                                new String[] {"noAuthenticationClient"}));

        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        when(mockValidator.validateTokenRequest(any()))
                .thenReturn(ValidationResult.createValidResult());
        when(mockValidator.validateRedirectUrlsMatch(any(), any()))
                .thenReturn(new ValidationResult(false, OAuth2Error.INVALID_GRANT));

        String result = (String) tokenHandler.issueAccessToken.handle(mockRequest, mockResponse);

        verify(mockResponse).type("application/json;charset=UTF-8");
        verify(mockResponse).status(HTTPResponse.SC_BAD_REQUEST);

        ErrorObject resultantErrorObject =
                createErrorFromResult(HTTPResponse.SC_BAD_REQUEST, result);

        assertEquals(INVALID_GRANT_CODE, resultantErrorObject.getCode());
        assertEquals("Invalid grant", resultantErrorObject.getDescription());
    }

    private ErrorObject createErrorFromResult(int responseStatusCode, String result)
            throws ParseException {
        HTTPResponse response = new HTTPResponse(responseStatusCode);
        response.setContent(result);
        response.setContentType("application/json;charset=UTF-8");
        return ErrorObject.parse(response);
    }
}
