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

    private static final String BASE64_PRIVATE_KEY =
            "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";

    private static final String BASE64_CERT =
            "MIIFVjCCAz4CCQDGbJ/u6uFT6DANBgkqhkiG9w0BAQsFADBtMQswCQYDVQQGEwJHQjENMAsGA1UECAwEVGVzdDENMAsGA1UEBwwEVGVzdDENMAsGA1UECgwEVEVzdDENMAsGA1UECwwEVEVzdDENMAsGA1UEAwwEVEVzdDETMBEGCSqGSIb3DQEJARYEVGVzdDAeFw0yMjAxMDcxNTM0NTlaFw0yMzAxMDcxNTM0NTlaMG0xCzAJBgNVBAYTAkdCMQ0wCwYDVQQIDARUZXN0MQ0wCwYDVQQHDARUZXN0MQ0wCwYDVQQKDARURXN0MQ0wCwYDVQQLDARURXN0MQ0wCwYDVQQDDARURXN0MRMwEQYJKoZIhvcNAQkBFgRUZXN0MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAy1cVZ1KfFmgFlDQyf/R3LF/Js6jAS2Zzbs8WGSS0ys6Z+XR4x5DTIznZp5cHuuQmqOFylXSw5oGBwMXd2L6NimG9rJnJ4w8Gy5A6ImGsiDZC+3AXRBb5hq/IdDTBjbUqRxAKokSVwotWZt554BdSRPTmlYDujzxnClNKA06Xb/X3rTsgCUmZhUnSVtOzKytP3Bdv88VI5gq5tlZOtKXCB0PnJOqRbBmuL1RNkeTny4ZJW3I2ywSATwDDyDm4pJ8XGGNFKaYYTwr6uNTQ2VHb1FVC33oWbg+Zu9D4p5l7ONicCCF3V+GbvmyeCmHGnXznz0nYX1LFqaKtruEh3/GXyLy5X03Jzq6HhTf1SNFBmzziuCovhbR4v5aFDqAYNPWz+ajOdTUfP1I18c5jR1xGUxEiiLKBZWU1J5mhqCa+0CdI0mi3HwFmluudh47I2Xw++JiqZQpxRqNGcKJOPnWDgKOKXQ/ag37aJkxqoYWk9pQ/pXOdIKm//+B//8nWGo8BA/bfdmMHyzhWWxqtydjie2EZ5ODSdQ+yu1xU5cwP59BEQoU7FKVEGiJa4kzrsI2cgloUPlsPfLENMa5i09exDo//eDB/zNy9ACgGCriov1ex3uv4vHp3WtpZYe+akGEJeP0N5dejs0hkBuX+LUcM30TnQ424tEzcuaJ1F7r4FP0CAwEAATANBgkqhkiG9w0BAQsFAAOCAgEAUh5gZx8S/XoZZoQai2uTyW/lyr1LpXMQyfvdWRr5+/OtFuASG3fAPXOTiUfuqH6Uma8BaXPRbSGWxBOFg0EbyvUY4UczZXZgVqyzkGjD2bVcnGra1OHz2AkcJm7OvzjMUvmXdDiQ8WcKIH16BZVsJFveTffJbM/KxL9UUdSLT0fNw1OvZWN1LxRj+X16B26ZnmaXPdmEC8MfwNcEU63qSlIbAvLg9Dp03weqO1qWR1vI/n1jwqidCUVwT0XF88/pJrds8/8guKlawhp9Yv+jMVYaawBiALR+5PFN56DivtmSVI5uv3oFh5tqJXXn9PhsPcIq0YKGQvvcdZl7vCikS65VzmswXBVFJNsYeeZ5NmiH2ANQd4+BLetgLAoXZxaOJ4nK+3Ml+gMwpZRRAbtixKJQDtVy+Ahuh1TEwTS1CERDYq43LhVYbMcgxdOLpZLvMew2tvJc3HfSWQKuF+NjGn/RwG54GyhjpdbfNZMB/EJXNJMt1j9RSVbPLsWjaENUkZoXE0otSou9tJOR0fwoqBJGUi5GCp98+iBdIQMAvXW5JkoDS6CM1FOfSv9ZXLvfXHOuBfKTDeVNy7u3QvyJ+BdkSc0iH4gj1F2zLHNIaZbDzwRzcDf2s3D1wTtoJ/WxfRSLGBMuUsXSduh9Md1S862N3Ce6wpri1IsgySCP84Y=";

    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String TEST_REDIRECT_URI = "https://example.com";

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @BeforeEach
    void setup() {
        mockResponse = mock(Response.class);
        mockRequest = mock(Request.class);
        mockViewHelper = mock(ViewHelper.class);
        mockAuthCodeService = mock(AuthCodeService.class);
        mockCredentialService = mock(CredentialService.class);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Map<String, Map<String, String>> clientConfig = Map.of("test", Map.of("signingCert", BASE64_CERT));
        String base64 = Base64.getEncoder().encodeToString(gson.toJson(clientConfig).getBytes(StandardCharsets.UTF_8));
        environmentVariables.set("CLIENT_CONFIG", base64);

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
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[]{"test"});
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
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[]{"test"});
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
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[]{"test"});
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
    void shouldRenderMustacheTemplateWhenValidRequestReceivedWithIncorrectClientId() throws Exception {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);

        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[]{"not-a-valid-client"});
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
        assertEquals("Error: Could not find client configuration details for: not-a-valid-client", frontendParamsCaptor.getValue().get("sharedAttributes"));
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
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[]{"test"});
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
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[]{"test-client-id"});
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
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[]{"test-client-id"});
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
                                        Base64.getDecoder().decode(BASE64_PRIVATE_KEY)));
    }
}
