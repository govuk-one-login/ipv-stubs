package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ResponseType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import uk.gov.di.ipv.stub.cred.service.AuthCodeService;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.utils.ViewHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuthorizeHandlerTest {

    private Response mockResponse;
    private Request mockRequest;
    private ViewHelper mockViewHelper;
    private AuthorizeHandler authorizeHandler;
    private AuthCodeService mockAuthCodeService;
    private CredentialService mockCredentialService;

    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String TEST_REDIRECT_URI = "https://example.com";

    @BeforeEach
    void setup() {
        mockResponse = mock(Response.class);
        mockRequest = mock(Request.class);
        mockViewHelper = mock(ViewHelper.class);
        mockAuthCodeService = mock(AuthCodeService.class);
        mockCredentialService = mock(CredentialService.class);

        authorizeHandler = new AuthorizeHandler(mockViewHelper, mockAuthCodeService, mockCredentialService);
    }

    @Test
    void shouldRenderMustacheTemplateWhenValidRequestReceived() throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);

        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[]{"test-client-id"});
        queryParams.put(RequestParamConstants.REDIRECT_URI, new String[]{TEST_REDIRECT_URI});
        queryParams.put(RequestParamConstants.RESPONSE_TYPE, new String[]{ResponseType.Value.CODE.getValue()});
        queryParams.put(RequestParamConstants.STATE, new String[]{"test-state"});
        when(mockHttpRequest.getParameterMap()).thenReturn(queryParams);

        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String renderOutput = "rendered output";
        when(mockViewHelper.render(anyMap(), eq("authorize.mustache"))).thenReturn(renderOutput);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertEquals(renderOutput, result);
        verify(mockViewHelper).render(anyMap(), eq("authorize.mustache"));
    }

    @Test
    void shouldReturn400WhenRedirectUriParamNotProvided() throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);

        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertEquals("redirect_uri param must be provided", result);
        verify(mockViewHelper, never()).render(Collections.emptyMap(), "authorize.mustache");
        verify(mockResponse).status(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    void shouldReturn302WithErrorQueryParamsWhenResponseTypeParamNotProvided() throws Exception {
        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.REDIRECT_URI, new String[]{TEST_REDIRECT_URI});

        invokeDoAuthorizeAndMakeAssertions(queryParams, createExpectedErrorQueryStringParams(OAuth2Error.UNSUPPORTED_RESPONSE_TYPE));
    }

    @Test
    void shouldReturn302WithErrorQueryParamsWhenResponseTypeParamNotCode() throws Exception {
        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.REDIRECT_URI, new String[]{TEST_REDIRECT_URI});
        queryParams.put(RequestParamConstants.RESPONSE_TYPE, new String[]{"invalid-type"});

        invokeDoAuthorizeAndMakeAssertions(queryParams, createExpectedErrorQueryStringParams(OAuth2Error.UNSUPPORTED_RESPONSE_TYPE));
    }

    @Test
    void shouldReturn302WithErrorQueryParamsWhenClientIdParamNotProvided() throws Exception {
        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.REDIRECT_URI, new String[]{TEST_REDIRECT_URI});
        queryParams.put(RequestParamConstants.RESPONSE_TYPE, new String[]{ResponseType.Value.CODE.getValue()});

        invokeDoAuthorizeAndMakeAssertions(queryParams, createExpectedErrorQueryStringParams(OAuth2Error.INVALID_CLIENT));
    }

    @Test
    void shouldReturn302WithAuthCodeQueryParamWhenValidAuthRequest() throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);

        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[]{"test-client-id"});
        queryParams.put(RequestParamConstants.REDIRECT_URI, new String[]{TEST_REDIRECT_URI});
        queryParams.put(RequestParamConstants.RESPONSE_TYPE, new String[]{ResponseType.Value.CODE.getValue()});
        queryParams.put(RequestParamConstants.STATE, new String[]{"test-state"});
        queryParams.put(RequestParamConstants.RESOURCE_ID, new String[]{UUID.randomUUID().toString()});
        queryParams.put(RequestParamConstants.JSON_PAYLOAD, new String[]{"{\"test\": \"test-value\"}"});
        when(mockHttpRequest.getParameterMap()).thenReturn(queryParams);

        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String result = (String) authorizeHandler.generateAuthCode.handle(mockRequest, mockResponse);

        ArgumentCaptor<String> redirectUriCaptor = ArgumentCaptor.forClass(String.class);
        assertNull(result);
        verify(mockResponse).type(DEFAULT_RESPONSE_CONTENT_TYPE);
        verify(mockAuthCodeService).persist(any(AuthorizationCode.class), anyString());
        verify(mockResponse).redirect(redirectUriCaptor.capture());
        assertNotNull(redirectUriCaptor.getValue());
    }

    private String createExpectedErrorQueryStringParams(ErrorObject error) {
        return createExpectedErrorQueryStringParams(error.getCode(), error.getDescription());
    }

    private String createExpectedErrorQueryStringParams(String errorCode, String errorDesc) {
        return "?error="
                + errorCode
                + "&error_description="
                + URLEncoder.encode(errorDesc, StandardCharsets.UTF_8);
    }

    private void invokeDoAuthorizeAndMakeAssertions(Map<String, String[]> queryParams, String expectedErrorCodeAndDescription) throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);

        when(mockHttpRequest.getParameterMap()).thenReturn(queryParams);

        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertNull(result);
        verify(mockViewHelper, never()).render(Collections.emptyMap(), "authorize.mustache");
        verify(mockResponse).type(DEFAULT_RESPONSE_CONTENT_TYPE);
        verify(mockResponse).redirect(TEST_REDIRECT_URI + expectedErrorCodeAndDescription);
    }
}
