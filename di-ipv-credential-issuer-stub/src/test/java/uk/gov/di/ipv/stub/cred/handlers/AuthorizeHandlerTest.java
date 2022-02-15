package uk.gov.di.ipv.stub.cred.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ResponseType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.fixtures.TestFixtures;
import uk.gov.di.ipv.stub.cred.service.AuthCodeService;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.utils.ViewHelper;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SystemStubsExtension.class)
class AuthorizeHandlerTest {

    private Response mockResponse;
    private Request mockRequest;
    private ViewHelper mockViewHelper;
    private AuthorizeHandler authorizeHandler;
    private AuthCodeService mockAuthCodeService;
    private CredentialService mockCredentialService;

    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String TEST_REDIRECT_URI = "https://valid.example.com";

    @SystemStub
    private final EnvironmentVariables environmentVariables = new EnvironmentVariables("CLIENT_CONFIG", TestFixtures.CLIENT_CONFIG);

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
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[]{"clientIdValid"});
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

        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[]{"clientIdValid"});
        queryParams.put(RequestParamConstants.RESPONSE_TYPE, new String[]{ResponseType.Value.CODE.getValue()});
        queryParams.put(RequestParamConstants.STATE, new String[]{"test-state"});
        when(mockHttpRequest.getParameterMap()).thenReturn(queryParams);

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
    void shouldReturn302WithErrorQueryParamsWhenClientIdParamNotRegistered() throws Exception {
        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[]{"not-registered"});
        queryParams.put(RequestParamConstants.REDIRECT_URI, new String[]{TEST_REDIRECT_URI});
        queryParams.put(RequestParamConstants.RESPONSE_TYPE, new String[]{ResponseType.Value.CODE.getValue()});

        invokeDoAuthorizeAndMakeAssertions(queryParams, createExpectedErrorQueryStringParams(OAuth2Error.INVALID_CLIENT));
    }

    @Test
    void shouldRenderMustacheTemplateWhenValidRequestReceivedWithRequestJWT() throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        List<String> givenNames = Arrays.asList("Daniel", "Dan", "Danny");
        List<String> dateOfBirths = Arrays.asList("01/01/1980", "02/01/1980");
        List<String> addresses = Arrays.asList("{\"line1\":\"\321 Street\",\"postcode\":\"M34 1AA\"");
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim("givenNames", givenNames)
                .claim("dateOfBirths", dateOfBirths)
                .claim("addresses", addresses)
                .build();

        RSASSASigner rsaSigner = new RSASSASigner(getPrivateKey());
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);
        signedJWT.sign(rsaSigner);

        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[]{"clientIdValid"});
        queryParams.put(RequestParamConstants.REDIRECT_URI, new String[]{TEST_REDIRECT_URI});
        queryParams.put(RequestParamConstants.RESPONSE_TYPE, new String[]{ResponseType.Value.CODE.getValue()});
        queryParams.put(RequestParamConstants.STATE, new String[]{"test-state"});
        queryParams.put(RequestParamConstants.REQUEST, new String[]{signedJWT.serialize()});
        when(mockHttpRequest.getParameterMap()).thenReturn(queryParams);

        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String renderOutput = "rendered output";
        when(mockViewHelper.render(anyMap(), eq("authorize.mustache"))).thenReturn(renderOutput);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertEquals(renderOutput, result);

        ArgumentCaptor<Map<String, Object>> frontendParamsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockViewHelper).render(frontendParamsCaptor.capture(), eq("authorize.mustache"));

        assertTrue(Boolean.parseBoolean(frontendParamsCaptor.getValue().get("isEvidenceType").toString()));
        assertEquals(gson.toJson(claimsSet.toJSONObject()), frontendParamsCaptor.getValue().get("sharedAttributes"));
    }

    @Test
    void shouldRenderMustacheTemplateWhenValidRequestReceivedWithInvalidRequestJWT() throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);

        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[]{"clientIdValid"});
        queryParams.put(RequestParamConstants.REDIRECT_URI, new String[]{TEST_REDIRECT_URI});
        queryParams.put(RequestParamConstants.RESPONSE_TYPE, new String[]{ResponseType.Value.CODE.getValue()});
        queryParams.put(RequestParamConstants.STATE, new String[]{"test-state"});
        queryParams.put(RequestParamConstants.REQUEST, new String[]{"invalid-shared-attributes-JWT"});
        when(mockHttpRequest.getParameterMap()).thenReturn(queryParams);

        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String renderOutput = "rendered output";
        when(mockViewHelper.render(anyMap(), eq("authorize.mustache"))).thenReturn(renderOutput);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertEquals(renderOutput, result);
        ArgumentCaptor<Map<String, Object>> frontendParamsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockViewHelper).render(frontendParamsCaptor.capture(), eq("authorize.mustache"));
        assertTrue(Boolean.parseBoolean(frontendParamsCaptor.getValue().get("isEvidenceType").toString()));
        assertEquals("Error: failed to parse shared attribute JWT", frontendParamsCaptor.getValue().get("sharedAttributes"));
    }

    @Test
    void shouldRenderMustacheTemplateWhenValidRequestReceivedWithMissingRequestJWT() throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);

        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[]{"clientIdValid"});
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
        ArgumentCaptor<Map<String, String>> frontendParamsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockViewHelper).render(frontendParamsCaptor.capture(), eq("authorize.mustache"));
        assertEquals("Error: missing 'request' query parameter", frontendParamsCaptor.getValue().get("sharedAttributes"));
    }

    @Test
    void shouldRenderMustacheTemplateWhenValidRequestReceivedWhenSignatureVerificationFails() throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);

        List<String> givenNames = Arrays.asList("Daniel", "Dan", "Danny");
        List<String> dateOfBirths = Arrays.asList("01/01/1980", "02/01/1980");
        List<String> addresses = Arrays.asList("{\"line1\":\"\321 Street\",\"postcode\":\"M34 1AA\"");
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim("givenNames", givenNames)
                .claim("dateOfBirths", dateOfBirths)
                .claim("addresses", addresses)
                .build();

        RSAKey rsaJWK = new RSAKeyGenerator(2048)
                .keyID("123")
                .generate();
        JWSSigner signer = new RSASSASigner(rsaJWK);

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);
        signedJWT.sign(signer);

        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[]{"clientIdValid"});
        queryParams.put(RequestParamConstants.REDIRECT_URI, new String[]{TEST_REDIRECT_URI});
        queryParams.put(RequestParamConstants.RESPONSE_TYPE, new String[]{ResponseType.Value.CODE.getValue()});
        queryParams.put(RequestParamConstants.STATE, new String[]{"test-state"});
        queryParams.put(RequestParamConstants.REQUEST, new String[]{signedJWT.serialize()});
        when(mockHttpRequest.getParameterMap()).thenReturn(queryParams);

        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String renderOutput = "rendered output";
        when(mockViewHelper.render(anyMap(), eq("authorize.mustache"))).thenReturn(renderOutput);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertEquals(renderOutput, result);
        ArgumentCaptor<Map<String, String>> frontendParamsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockViewHelper).render(frontendParamsCaptor.capture(), eq("authorize.mustache"));
        assertEquals("Error: Signature of the shared attribute JWT is not valid", frontendParamsCaptor.getValue().get("sharedAttributes"));
    }

    @Test
    void shouldReturn302WithAuthCodeQueryParamWhenValidAuthRequest() throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);

        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[]{"clientIdValid"});
        queryParams.put(RequestParamConstants.REDIRECT_URI, new String[]{TEST_REDIRECT_URI});
        queryParams.put(RequestParamConstants.RESPONSE_TYPE, new String[]{ResponseType.Value.CODE.getValue()});
        queryParams.put(RequestParamConstants.STATE, new String[]{"test-state"});
        queryParams.put(RequestParamConstants.RESOURCE_ID, new String[]{UUID.randomUUID().toString()});
        queryParams.put(RequestParamConstants.JSON_PAYLOAD, new String[]{"{\"test\": \"test-value\"}"});
        queryParams.put(CredentialIssuerConfig.EVIDENCE_STRENGTH_PARAM, new String[]{"2"});
        queryParams.put(CredentialIssuerConfig.EVIDENCE_VALIDITY_PARAM, new String[]{"3"});
        when(mockHttpRequest.getParameterMap()).thenReturn(queryParams);

        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String result = (String) authorizeHandler.generateResponse.handle(mockRequest, mockResponse);

        ArgumentCaptor<String> redirectUriCaptor = ArgumentCaptor.forClass(String.class);
        assertNull(result);
        verify(mockResponse).type(DEFAULT_RESPONSE_CONTENT_TYPE);
        verify(mockAuthCodeService).persist(any(AuthorizationCode.class), anyString());
        verify(mockResponse).redirect(redirectUriCaptor.capture());
        assertNotNull(redirectUriCaptor.getValue());
    }

    @Test
    void shouldCalldoAuthorizeMethodWhenInvalidJsonPayloadProvided() throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);

        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[]{"clientIdValid"});
        queryParams.put(RequestParamConstants.REDIRECT_URI, new String[]{TEST_REDIRECT_URI});
        queryParams.put(RequestParamConstants.RESPONSE_TYPE, new String[]{ResponseType.Value.CODE.getValue()});
        queryParams.put(RequestParamConstants.STATE, new String[]{"test-state"});
        queryParams.put(RequestParamConstants.RESOURCE_ID, new String[]{UUID.randomUUID().toString()});
        queryParams.put(RequestParamConstants.JSON_PAYLOAD, new String[]{"invalid-json"});
        queryParams.put(CredentialIssuerConfig.EVIDENCE_STRENGTH_PARAM, new String[]{"2"});
        queryParams.put(CredentialIssuerConfig.EVIDENCE_VALIDITY_PARAM, new String[]{"3"});
        when(mockHttpRequest.getParameterMap()).thenReturn(queryParams);

        QueryParamsMap queryParamsMap = new QueryParamsMap(mockHttpRequest);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        authorizeHandler.generateResponse.handle(mockRequest, mockResponse);

        verify(mockRequest).attribute("error", "Invalid JSON");
        verify(mockViewHelper).render(anyMap(), eq("authorize.mustache"));
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

    private RSAPrivateKey getPrivateKey() throws InvalidKeySpecException, NoSuchAlgorithmException {
        return (RSAPrivateKey)
                KeyFactory.getInstance("RSA")
                        .generatePrivate(
                                new PKCS8EncodedKeySpec(
                                        Base64.getDecoder().decode(TestFixtures.PRIVATE_KEY)));
    }
}
