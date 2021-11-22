package uk.gov.di.ipv.stub.cred.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import uk.gov.di.ipv.stub.cred.utils.ViewHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.stub.cred.handlers.QueryStringParamConstants.PERMITTED_RESPONSE_TYPE;
import static uk.gov.di.ipv.stub.cred.handlers.QueryStringParamConstants.QUERY_STRING_PARAM_CLIENT_ID;
import static uk.gov.di.ipv.stub.cred.handlers.QueryStringParamConstants.QUERY_STRING_PARAM_REDIRECT_URI;
import static uk.gov.di.ipv.stub.cred.handlers.QueryStringParamConstants.QUERY_STRING_PARAM_RESPONSE_TYPE;
import static uk.gov.di.ipv.stub.cred.handlers.QueryStringParamConstants.QUERY_STRING_PARAM_STATE;

public class AuthorizeHandlerTest {

    private Response mockResponse;
    private Request mockRequest;
    private ViewHelper mockViewHelper;
    private AuthorizeHandler authorizeHandler;

    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String TEST_REDIRECT_URI = "https://example.com";

    @BeforeEach
    void setup() {
        mockResponse = mock(Response.class);
        mockRequest = mock(Request.class);
        mockViewHelper = mock(ViewHelper.class);

        authorizeHandler = new AuthorizeHandler(mockViewHelper);
    }

    @Test
    void shouldRenderMustacheTemplateWhenValidRequestReceived() throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);

        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(QUERY_STRING_PARAM_CLIENT_ID, new String[]{"test-client-id"});
        queryParams.put(QUERY_STRING_PARAM_REDIRECT_URI, new String[]{TEST_REDIRECT_URI});
        queryParams.put(QUERY_STRING_PARAM_RESPONSE_TYPE, new String[]{PERMITTED_RESPONSE_TYPE});
        queryParams.put(QUERY_STRING_PARAM_STATE, new String[]{"test-state"});
        when(mockHttpRequest.getParameterMap()).thenReturn(queryParams);

        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String renderOutput = "rendered output";
        when(mockViewHelper.render(Collections.emptyMap(), "authorize.mustache")).thenReturn(renderOutput);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertEquals(renderOutput, result);
        verify(mockViewHelper).render(Collections.emptyMap(), "authorize.mustache");
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
        queryParams.put(QUERY_STRING_PARAM_REDIRECT_URI, new String[]{TEST_REDIRECT_URI});

        invokeDoAuthorizeAndMakeAssertsions(queryParams, "?error=invalid_request&error_description=response_type+param+must+be+provided");
    }

    @Test
    void shouldReturn302WithErrorQueryParamsWhenResponseTypeParamNotCode() throws Exception {
        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(QUERY_STRING_PARAM_REDIRECT_URI, new String[]{TEST_REDIRECT_URI});
        queryParams.put(QUERY_STRING_PARAM_RESPONSE_TYPE, new String[]{"invalid-type"});

        invokeDoAuthorizeAndMakeAssertsions(queryParams, "?error=unsupported_response_type&error_description=response_type+param+must+be+set+to+%27code%27");
    }

    @Test
    void shouldReturn302WithErrorQueryParamsWhenClientIdParamNotProvided() throws Exception {
        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(QUERY_STRING_PARAM_REDIRECT_URI, new String[]{TEST_REDIRECT_URI});
        queryParams.put(QUERY_STRING_PARAM_RESPONSE_TYPE, new String[]{PERMITTED_RESPONSE_TYPE});

        invokeDoAuthorizeAndMakeAssertsions(queryParams, "?error=invalid_request&error_description=client_id+param+must+be+provided");
    }

    @Test
    void shouldReturn302WithAuthCodeQueryParamWhenValidAuthRequest() throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);

        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(QUERY_STRING_PARAM_CLIENT_ID, new String[]{"test-client-id"});
        queryParams.put(QUERY_STRING_PARAM_REDIRECT_URI, new String[]{TEST_REDIRECT_URI});
        queryParams.put(QUERY_STRING_PARAM_RESPONSE_TYPE, new String[]{PERMITTED_RESPONSE_TYPE});
        queryParams.put(QUERY_STRING_PARAM_STATE, new String[]{"test-state"});
        when(mockHttpRequest.getParameterMap()).thenReturn(queryParams);

        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String result = (String) authorizeHandler.generateAuthCode.handle(mockRequest, mockResponse);

        ArgumentCaptor<String> redirectUriCaptor = ArgumentCaptor.forClass(String.class);
        assertNull(result);
        verify(mockResponse).type(DEFAULT_RESPONSE_CONTENT_TYPE);
        verify(mockResponse).redirect(redirectUriCaptor.capture());
        assertNotNull(redirectUriCaptor.getValue());
    }


    private void invokeDoAuthorizeAndMakeAssertsions(Map<String, String[]> queryParams, String expectedErrorCodeAndDescription) throws Exception {
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
