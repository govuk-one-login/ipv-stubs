package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import uk.gov.di.ipv.stub.cred.service.AuthCodeService;
import uk.gov.di.ipv.stub.cred.service.TokenService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.nimbusds.oauth2.sdk.OAuth2Error.INVALID_CLIENT_CODE;
import static com.nimbusds.oauth2.sdk.OAuth2Error.INVALID_GRANT_CODE;
import static com.nimbusds.oauth2.sdk.OAuth2Error.INVALID_REQUEST_CODE;
import static com.nimbusds.oauth2.sdk.OAuth2Error.UNSUPPORTED_GRANT_TYPE_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TokenHandlerTest {
    private static final String TEST_REDIRECT_URI = "https://example.com";
    private static final String TEST_AUTH_CODE = "e2Ln9Vs6bwZ1mDM8gfl256hg8I88i8LLenVfqxKuDEg";

    private Response mockResponse;
    private Request mockRequest;
    private TokenService mockTokenService;
    private AuthCodeService mockAuthCodeService;
    private TokenHandler tokenHandler;

    @BeforeEach
    void setup() {
        mockResponse = mock(Response.class);
        mockRequest = mock(Request.class);
        mockAuthCodeService = mock(AuthCodeService.class);
        mockTokenService = mock(TokenService.class);

        tokenHandler = new TokenHandler(mockAuthCodeService, mockTokenService);
    }

    @Test
    void shouldIssueAccessTokenWhenValidRequestReceived() throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);

        String resourceId = UUID.randomUUID().toString();

        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.GRANT_TYPE, new String[]{GrantType.AUTHORIZATION_CODE.getValue()});
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[]{"test-client-id"});
        queryParams.put(RequestParamConstants.REDIRECT_URI, new String[]{TEST_REDIRECT_URI});
        queryParams.put(RequestParamConstants.AUTH_CODE, new String[]{TEST_AUTH_CODE});
        when(mockHttpRequest.getParameterMap()).thenReturn(queryParams);

        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        when(mockAuthCodeService.getPayload(TEST_AUTH_CODE)).thenReturn(resourceId);
        when(mockTokenService.createBearerAccessToken()).thenReturn(new BearerAccessToken());

        String result = (String) tokenHandler.issueAccessToken.handle(mockRequest, mockResponse);

        assertNotNull(result);
        verify(mockResponse).status(HttpServletResponse.SC_OK);
        verify(mockResponse).type("application/json;charset=UTF-8");

        verify(mockAuthCodeService, times(2)).getPayload(TEST_AUTH_CODE);
        verify(mockAuthCodeService).revoke(TEST_AUTH_CODE);
        verify(mockTokenService).persist(any(AccessToken.class), eq(resourceId));

        HTTPResponse response = new HTTPResponse(HttpServletResponse.SC_OK);
        response.setContent(result);
        response.setContentType("application/json;charset=UTF-8");
        AccessTokenResponse accessTokenResponse = AccessTokenResponse.parse(response);

        assertNotNull(accessTokenResponse.getTokens().getAccessToken());
        assertNotNull(accessTokenResponse.getTokens().getRefreshToken());
        assertEquals(AccessTokenType.BEARER, accessTokenResponse.getTokens().getAccessToken().getType());
    }

    @Test
    void shouldReturn400ResponseWhenGrantTypeNotProvided() throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
        int responseStatusCode = HttpServletResponse.SC_BAD_REQUEST;

        Map<String, String[]> queryParams = new HashMap<>();
        when(mockHttpRequest.getParameterMap()).thenReturn(queryParams);

        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String result = (String) tokenHandler.issueAccessToken.handle(mockRequest, mockResponse);

        verify(mockResponse).type("application/json;charset=UTF-8");
        verify(mockResponse).status(responseStatusCode);

        ErrorObject resultantErrorObject = createErrorFromResult(responseStatusCode, result);

        assertEquals(UNSUPPORTED_GRANT_TYPE_CODE, resultantErrorObject.getCode());
        assertEquals("Unsupported grant type", resultantErrorObject.getDescription());
    }

    @Test
    void shouldReturn400ResponseWhenNoAuthCodeProvided() throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
        int responseStatusCode = HttpServletResponse.SC_BAD_REQUEST;

        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.GRANT_TYPE, new String[]{GrantType.AUTHORIZATION_CODE.getValue()});
        when(mockHttpRequest.getParameterMap()).thenReturn(queryParams);

        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String result = (String) tokenHandler.issueAccessToken.handle(mockRequest, mockResponse);

        verify(mockResponse).type("application/json;charset=UTF-8");
        verify(mockResponse).status(responseStatusCode);

        ErrorObject resultantErrorObject = createErrorFromResult(responseStatusCode, result);

        assertEquals(INVALID_GRANT_CODE, resultantErrorObject.getCode());
        assertEquals("Invalid grant", resultantErrorObject.getDescription());
    }

    @Test
    void shouldReturn400ResponseWhenInvalidAuthCodeProvided() throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);

        int responseStatusCode = HttpServletResponse.SC_BAD_REQUEST;

        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.GRANT_TYPE, new String[]{GrantType.AUTHORIZATION_CODE.getValue()});
        queryParams.put(RequestParamConstants.AUTH_CODE, new String[]{TEST_AUTH_CODE});
        when(mockHttpRequest.getParameterMap()).thenReturn(queryParams);

        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        when(mockAuthCodeService.getPayload(TEST_AUTH_CODE)).thenReturn(null);

        String result = (String) tokenHandler.issueAccessToken.handle(mockRequest, mockResponse);

        verify(mockResponse).type("application/json;charset=UTF-8");
        verify(mockResponse).status(responseStatusCode);
        verify(mockAuthCodeService).getPayload(TEST_AUTH_CODE);

        ErrorObject resultantErrorObject = createErrorFromResult(responseStatusCode, result);

        assertEquals(INVALID_GRANT_CODE, resultantErrorObject.getCode());
        assertEquals("Invalid grant", resultantErrorObject.getDescription());
    }

    @Test
    void shouldReturn400ResponseWhenRedirectUriNotProvided() throws Exception {
        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.GRANT_TYPE, new String[]{GrantType.AUTHORIZATION_CODE.getValue()});
        queryParams.put(RequestParamConstants.AUTH_CODE, new String[]{TEST_AUTH_CODE});

        invokeIssueAccessTokenAndMakeAssertions(
                queryParams,
                INVALID_REQUEST_CODE,
                "Invalid request",
                HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    void shouldReturn400ResponseWhenClientIdNotProvided() throws Exception {
        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.GRANT_TYPE, new String[]{GrantType.AUTHORIZATION_CODE.getValue()});
        queryParams.put(RequestParamConstants.AUTH_CODE, new String[]{TEST_AUTH_CODE});
        queryParams.put(RequestParamConstants.REDIRECT_URI, new String[]{TEST_REDIRECT_URI});

        invokeIssueAccessTokenAndMakeAssertions(
                queryParams,
                INVALID_CLIENT_CODE,
                "Client authentication failed",
                HttpServletResponse.SC_UNAUTHORIZED);
    }

    private void invokeIssueAccessTokenAndMakeAssertions(
            Map<String, String[]> queryParams,
            String errorCode,
            String errorDesc,
            int responseStatusCode) throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);

        when(mockHttpRequest.getParameterMap()).thenReturn(queryParams);

        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        when(mockAuthCodeService.getPayload(TEST_AUTH_CODE)).thenReturn(UUID.randomUUID().toString());

        String result = (String) tokenHandler.issueAccessToken.handle(mockRequest, mockResponse);

        verify(mockResponse).type("application/json;charset=UTF-8");
        verify(mockResponse).status(responseStatusCode);
        verify(mockAuthCodeService).getPayload(TEST_AUTH_CODE);

        ErrorObject resultantErrorObject = createErrorFromResult(responseStatusCode, result);

        assertEquals(errorCode, resultantErrorObject.getCode());
        assertEquals(errorDesc, resultantErrorObject.getDescription());
    }

    private ErrorObject createErrorFromResult(int responseStatusCode, String result) throws ParseException {
        HTTPResponse response = new HTTPResponse(responseStatusCode);
        response.setContent(result);
        response.setContentType("application/json;charset=UTF-8");
        return ErrorObject.parse(response);
    }
}
