package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.di.ipv.stub.cred.auth.ClientJwtVerifier;
import uk.gov.di.ipv.stub.cred.domain.RequestedError;
import uk.gov.di.ipv.stub.cred.error.ClientAuthenticationException;
import uk.gov.di.ipv.stub.cred.service.AuthCodeService;
import uk.gov.di.ipv.stub.cred.service.RequestedErrorResponseService;
import uk.gov.di.ipv.stub.cred.service.TokenService;
import uk.gov.di.ipv.stub.cred.utils.StubSsmClient;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;
import uk.gov.di.ipv.stub.cred.validation.Validator;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.stub.cred.fixtures.TestFixtures.CLIENT_CONFIG;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SystemStubsExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TokenHandlerTest {
    private static final String TEST_REDIRECT_URI = "https://example.com";
    private static final String TEST_AUTH_CODE =
            "e2Ln9Vs6bwZ1mDM8gfl256hg8I88i8LLenVfqxKuDEg"; // pragma: allowlist secret

    @Mock private Context mockContext;
    @Mock private TokenService mockTokenService;
    @Mock private AuthCodeService mockAuthCodeService;
    @Mock private Validator mockValidator;
    @Mock private ClientJwtVerifier mockJwtAuthenticationService;
    @Captor private ArgumentCaptor<JSONObject> resultCaptor;

    @Spy
    private RequestedErrorResponseService requestedErrorResponseService =
            new RequestedErrorResponseService();

    @InjectMocks private TokenHandler tokenHandler;

    @SystemStub
    private final EnvironmentVariables environmentVariables =
            new EnvironmentVariables(
                    "CLIENT_AUDIENCE",
                    "https://test-server.example.com/token",
                    "ENVIRONMENT",
                    "TEST");

    @BeforeAll
    public static void setUp() {
        StubSsmClient.setClientConfigParams(CLIENT_CONFIG);
    }

    @Test
    void shouldIssueAccessTokenWhenValidRequestReceivedUsingJwtClientAuthentication()
            throws Exception {
        String resourceId = UUID.randomUUID().toString();

        setupMockFormParams(
                Map.of(
                        RequestParamConstants.GRANT_TYPE, GrantType.AUTHORIZATION_CODE.getValue(),
                        RequestParamConstants.CLIENT_ASSERTION, "a-client-assertion",
                        RequestParamConstants.CLIENT_ASSERTION_TYPE, CLIENT_ASSERTION_TYPE,
                        RequestParamConstants.REDIRECT_URI, TEST_REDIRECT_URI,
                        RequestParamConstants.AUTH_CODE, TEST_AUTH_CODE));

        when(mockValidator.validateTokenRequest(any()))
                .thenReturn(ValidationResult.createValidResult());
        when(mockValidator.validateRedirectUrlsMatch(anyString(), anyString()))
                .thenReturn(ValidationResult.createValidResult());
        when(mockAuthCodeService.getPayload(TEST_AUTH_CODE)).thenReturn(resourceId);
        when(mockAuthCodeService.getRedirectUrl(TEST_AUTH_CODE)).thenReturn(TEST_REDIRECT_URI);
        when(mockTokenService.createBearerAccessToken()).thenReturn(new BearerAccessToken());

        tokenHandler.issueAccessToken(mockContext);

        verify(mockJwtAuthenticationService).authenticateClient(any());
        verify(mockAuthCodeService).getPayload(TEST_AUTH_CODE);
        verify(mockAuthCodeService).revoke(TEST_AUTH_CODE);
        verify(mockTokenService).persist(any(AccessToken.class), eq(resourceId));

        verify(mockContext).json(resultCaptor.capture());
        AccessTokenResponse accessTokenResponse =
                AccessTokenResponse.parse(resultCaptor.getValue());

        assertNotNull(accessTokenResponse.getTokens().getAccessToken());
        assertNotNull(accessTokenResponse.getTokens().getRefreshToken());
        assertEquals(
                AccessTokenType.BEARER, accessTokenResponse.getTokens().getAccessToken().getType());
    }

    @Test
    void shouldIssueAccessTokenWhenValidRequestReceivedWithClientId() throws Exception {
        // Having the client_id provided in the params implies the client is not using JWT
        // authentication
        String resourceId = UUID.randomUUID().toString();

        setupMockFormParams(
                Map.of(
                        RequestParamConstants.GRANT_TYPE,
                        GrantType.AUTHORIZATION_CODE.getValue(),
                        RequestParamConstants.CLIENT_ID,
                        "noAuthenticationClient",
                        RequestParamConstants.REDIRECT_URI,
                        TEST_REDIRECT_URI,
                        RequestParamConstants.AUTH_CODE,
                        TEST_AUTH_CODE));

        when(mockValidator.validateTokenRequest(any()))
                .thenReturn(ValidationResult.createValidResult());
        when(mockValidator.validateRedirectUrlsMatch(anyString(), anyString()))
                .thenReturn(ValidationResult.createValidResult());
        when(mockAuthCodeService.getPayload(TEST_AUTH_CODE)).thenReturn(resourceId);
        when(mockAuthCodeService.getRedirectUrl(TEST_AUTH_CODE)).thenReturn(TEST_REDIRECT_URI);
        when(mockTokenService.createBearerAccessToken()).thenReturn(new BearerAccessToken());

        tokenHandler.issueAccessToken(mockContext);

        verify(mockAuthCodeService).getPayload(TEST_AUTH_CODE);
        verify(mockAuthCodeService).revoke(TEST_AUTH_CODE);
        verify(mockTokenService).persist(any(AccessToken.class), eq(resourceId));

        verify(mockContext).json(resultCaptor.capture());
        AccessTokenResponse accessTokenResponse =
                AccessTokenResponse.parse(resultCaptor.getValue());

        assertNotNull(accessTokenResponse.getTokens().getAccessToken());
        assertNotNull(accessTokenResponse.getTokens().getRefreshToken());
        assertEquals(
                AccessTokenType.BEARER, accessTokenResponse.getTokens().getAccessToken().getType());
    }

    @Test
    void shouldReturnCorrectErrorResponseIfTokenRequestFailsValidation() throws Exception {
        when(mockValidator.validateTokenRequest(any()))
                .thenReturn(new ValidationResult(false, OAuth2Error.INVALID_CLIENT));

        tokenHandler.issueAccessToken(mockContext);

        verify(mockContext).status(HttpStatus.UNAUTHORIZED.getCode());
        verify(mockContext).json(resultCaptor.capture());

        var resultantErrorObject = ErrorObject.parse(resultCaptor.getValue());

        assertEquals(INVALID_CLIENT_CODE, resultantErrorObject.getCode());
        assertEquals("Client authentication failed", resultantErrorObject.getDescription());
    }

    @Test
    void shouldReturn401IfJwtAuthenticationFails() throws Exception {
        when(mockValidator.validateTokenRequest(any()))
                .thenReturn(ValidationResult.createValidResult());
        doThrow(new ClientAuthenticationException("Fail."))
                .when(mockJwtAuthenticationService)
                .authenticateClient(any());

        tokenHandler.issueAccessToken(mockContext);

        verify(mockContext).status(OAuth2Error.INVALID_CLIENT.getHTTPStatusCode());
        verify(mockContext).json(resultCaptor.capture());

        var resultantErrorObject = ErrorObject.parse(resultCaptor.getValue());

        assertEquals(INVALID_CLIENT_CODE, resultantErrorObject.getCode());
        assertEquals("Client authentication failed", resultantErrorObject.getDescription());
    }

    @Test
    void shouldReturn400IfClientConfigureForAuthenticationProvidesClientId() throws Exception {
        setupMockFormParams(Map.of(RequestParamConstants.CLIENT_ID, "clientIdValid"));
        when(mockValidator.validateTokenRequest(any()))
                .thenReturn(ValidationResult.createValidResult());

        tokenHandler.issueAccessToken(mockContext);

        verify(mockContext).status(HttpStatus.BAD_REQUEST.getCode());
        verify(mockContext).json(resultCaptor.capture());

        var resultantErrorObject = ErrorObject.parse(resultCaptor.getValue());

        assertEquals(INVALID_REQUEST_CODE, resultantErrorObject.getCode());
        assertEquals("Invalid request", resultantErrorObject.getDescription());
    }

    @Test
    void shouldReturn400ResponseWhenRedirectUrlsDoNotMatch() throws Exception {
        setupMockFormParams(Map.of(RequestParamConstants.CLIENT_ID, "noAuthenticationClient"));

        when(mockValidator.validateTokenRequest(any()))
                .thenReturn(ValidationResult.createValidResult());
        when(mockValidator.validateRedirectUrlsMatch(any(), any()))
                .thenReturn(new ValidationResult(false, OAuth2Error.INVALID_GRANT));

        tokenHandler.issueAccessToken(mockContext);

        verify(mockContext).status(HttpStatus.BAD_REQUEST.getCode());
        verify(mockContext).json(resultCaptor.capture());

        var resultantErrorObject = ErrorObject.parse(resultCaptor.getValue());

        assertEquals(INVALID_GRANT_CODE, resultantErrorObject.getCode());
        assertEquals("Invalid grant", resultantErrorObject.getDescription());
    }

    @Test
    void shouldReturn400WithRequestedOAuthError() throws Exception {
        setupMockFormParams(
                Map.of(
                        RequestParamConstants.AUTH_CODE, "anAuthCode",
                        RequestParamConstants.REQUESTED_OAUTH_ERROR, "access_denied",
                        RequestParamConstants.REQUESTED_OAUTH_ERROR_ENDPOINT, "token",
                        RequestParamConstants.REQUESTED_OAUTH_ERROR_DESCRIPTION,
                                "an requestedError description"));

        requestedErrorResponseService.persist(
                "anAuthCode",
                new RequestedError("access_denied", "an error description", "token", null));

        tokenHandler.issueAccessToken(mockContext);

        verify(mockContext).status(HttpStatus.BAD_REQUEST.getCode());
        verify(mockContext).json(resultCaptor.capture());

        assertEquals("access_denied", resultCaptor.getValue().get("error"));
        assertEquals("an error description", resultCaptor.getValue().get("error_description"));
    }

    private void setupMockFormParams(Map<String, String> params) {
        params.forEach((key, value) -> when(mockContext.formParam(key)).thenReturn(value));
    }
}
