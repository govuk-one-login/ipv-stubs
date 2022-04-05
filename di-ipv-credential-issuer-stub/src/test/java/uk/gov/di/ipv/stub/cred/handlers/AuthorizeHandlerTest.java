package uk.gov.di.ipv.stub.cred.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
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
import uk.gov.di.ipv.stub.cred.domain.Credential;
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
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import static uk.gov.di.ipv.stub.cred.handlers.AuthorizeHandler.CLAIMS_CLAIM;
import static uk.gov.di.ipv.stub.cred.handlers.AuthorizeHandler.VC_HTTP_API_CLAIM;

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
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @SystemStub
    private final EnvironmentVariables environmentVariables =
            new EnvironmentVariables("CLIENT_CONFIG", TestFixtures.CLIENT_CONFIG);

    @BeforeEach
    void setup() {
        CredentialIssuerConfig.resetClientConfigs();

        mockResponse = mock(Response.class);
        mockRequest = mock(Request.class);
        mockViewHelper = mock(ViewHelper.class);
        mockAuthCodeService = mock(AuthCodeService.class);
        mockCredentialService = mock(CredentialService.class);

        authorizeHandler =
                new AuthorizeHandler(mockViewHelper, mockAuthCodeService, mockCredentialService);
    }

    @Test
    void doAuthorizeShouldRenderMustacheTemplateWhenValidRequestReceived() throws Exception {
        QueryParamsMap validQueryParamsMap = toQueryParamsMap(validDoAuthorizeQueryParams());
        when(mockRequest.queryMap()).thenReturn(validQueryParamsMap);

        String renderOutput = "rendered output";
        when(mockViewHelper.render(anyMap(), eq("authorize.mustache"))).thenReturn(renderOutput);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertEquals(renderOutput, result);
        verify(mockViewHelper).render(anyMap(), eq("authorize.mustache"));
    }

    @Test
    void doAuthorizeShouldReturn400WhenRedirectUriParamNotRegistered() throws Exception {
        Map<String, String[]> queryParams = validDoAuthorizeQueryParams();
        queryParams.put(
                RequestParamConstants.REDIRECT_URI,
                new String[] {"https://not-registered.exanple.com"});
        QueryParamsMap qpmWithoutRedirectUri = toQueryParamsMap(queryParams);

        when(mockRequest.queryMap()).thenReturn(qpmWithoutRedirectUri);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertEquals(
                "redirect_uri param provided does not match any of the redirect_uri values configured",
                result);
        verify(mockViewHelper, never()).render(Collections.emptyMap(), "authorize.mustache");
        verify(mockResponse).status(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    void doAuthorizeShouldReturn400WhenRedirectUriParamNotProvided() throws Exception {
        Map<String, String[]> queryParams = validDoAuthorizeQueryParams();
        queryParams.remove(RequestParamConstants.REDIRECT_URI);
        QueryParamsMap qpmWithoutRedirectUri = toQueryParamsMap(queryParams);

        when(mockRequest.queryMap()).thenReturn(qpmWithoutRedirectUri);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertEquals("redirect_uri param must be provided", result);
        verify(mockViewHelper, never()).render(Collections.emptyMap(), "authorize.mustache");
        verify(mockResponse).status(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    void doAuthorizeShouldReturn302WithErrorQueryParamsWhenResponseTypeParamNotProvided()
            throws Exception {
        Map<String, String[]> queryParams = validDoAuthorizeQueryParams();
        queryParams.remove(RequestParamConstants.RESPONSE_TYPE);

        invokeDoAuthorizeAndMakeAssertions(
                queryParams,
                createExpectedErrorQueryStringParams(OAuth2Error.UNSUPPORTED_RESPONSE_TYPE));
    }

    @Test
    void doAuthorizeShouldReturn302WithErrorQueryParamsWhenResponseTypeParamNotCode()
            throws Exception {
        Map<String, String[]> queryParams = validDoAuthorizeQueryParams();
        queryParams.put(RequestParamConstants.RESPONSE_TYPE, new String[] {"invalid-type"});

        invokeDoAuthorizeAndMakeAssertions(
                queryParams,
                createExpectedErrorQueryStringParams(OAuth2Error.UNSUPPORTED_RESPONSE_TYPE));
    }

    @Test
    void doAuthorizeShouldReturn302WithErrorQueryParamsWhenClientIdParamNotProvided()
            throws Exception {
        Map<String, String[]> queryParams = validDoAuthorizeQueryParams();
        queryParams.remove(RequestParamConstants.CLIENT_ID);

        invokeDoAuthorizeAndMakeAssertions(
                queryParams, createExpectedErrorQueryStringParams(OAuth2Error.INVALID_CLIENT));
    }

    @Test
    void doAuthorizeShouldReturn302WithErrorQueryParamsWhenClientIdParamNotRegistered()
            throws Exception {
        Map<String, String[]> queryParams = validDoAuthorizeQueryParams();
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[] {"not-registered"});

        invokeDoAuthorizeAndMakeAssertions(
                queryParams, createExpectedErrorQueryStringParams(OAuth2Error.INVALID_CLIENT));
    }

    @Test
    void doAuthorizeShouldRenderMustacheTemplateWhenValidRequestReceivedWithRequestJWT()
            throws Exception {
        QueryParamsMap queryParamsMap = toQueryParamsMap(validDoAuthorizeQueryParams());
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String renderOutput = "rendered output";
        when(mockViewHelper.render(anyMap(), eq("authorize.mustache"))).thenReturn(renderOutput);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertEquals(renderOutput, result);

        ArgumentCaptor<Map<String, Object>> frontendParamsCaptor =
                ArgumentCaptor.forClass(Map.class);
        verify(mockViewHelper).render(frontendParamsCaptor.capture(), eq("authorize.mustache"));

        assertTrue(
                Boolean.parseBoolean(
                        frontendParamsCaptor.getValue().get("isEvidenceType").toString()));

        Map<String, Object> claims =
                (Map<String, Object>) validClaimsSet().toJSONObject().get(CLAIMS_CLAIM);
        assertEquals(
                gson.toJson(claims.get(VC_HTTP_API_CLAIM)),
                frontendParamsCaptor.getValue().get("sharedAttributes"));
    }

    @Test
    void doAuthorizeShouldRenderMustacheTemplateWhenValidRequestReceivedWithInvalidRequestJWT()
            throws Exception {
        Map<String, String[]> queryParams = validDoAuthorizeQueryParams();
        queryParams.put(
                RequestParamConstants.REQUEST, new String[] {"invalid-shared-attributes-JWT"});
        QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String renderOutput = "rendered output";
        when(mockViewHelper.render(anyMap(), eq("authorize.mustache"))).thenReturn(renderOutput);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertEquals(renderOutput, result);
        ArgumentCaptor<Map<String, Object>> frontendParamsCaptor =
                ArgumentCaptor.forClass(Map.class);
        verify(mockViewHelper).render(frontendParamsCaptor.capture(), eq("authorize.mustache"));
        assertTrue(
                Boolean.parseBoolean(
                        frontendParamsCaptor.getValue().get("isEvidenceType").toString()));
        assertEquals(
                "Error: failed to parse something: Invalid serialized unsecured/JWS/JWE object: Missing part delimiters",
                frontendParamsCaptor.getValue().get("sharedAttributes"));
    }

    @Test
    void
            doAuthorizeShouldRenderMustacheTemplateWhenValidRequestReceivedWithRequestJWTMissingVcHttpApiClaim()
                    throws Exception {
        Map<String, String[]> queryParams = validDoAuthorizeQueryParams();
        queryParams.put(
                RequestParamConstants.REQUEST,
                new String[] {
                    signedRequestJwt(
                                    new JWTClaimsSet.Builder()
                                            .claim("NO_VC_HTTP_API_CLAIM", "nope")
                                            .build())
                            .serialize()
                });
        QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String renderOutput = "rendered output";
        when(mockViewHelper.render(anyMap(), eq("authorize.mustache"))).thenReturn(renderOutput);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertEquals(renderOutput, result);
        ArgumentCaptor<Map<String, Object>> frontendParamsCaptor =
                ArgumentCaptor.forClass(Map.class);
        verify(mockViewHelper).render(frontendParamsCaptor.capture(), eq("authorize.mustache"));
        assertTrue(
                Boolean.parseBoolean(
                        frontendParamsCaptor.getValue().get("isEvidenceType").toString()));
        assertEquals(
                "Error: vc_http_api claim not found in JWT",
                frontendParamsCaptor.getValue().get("sharedAttributes"));
    }

    @Test
    void doAuthorizeShouldRenderMustacheTemplateWhenValidRequestReceivedWithMissingRequestJWT()
            throws Exception {
        Map<String, String[]> queryParams = validDoAuthorizeQueryParams();
        queryParams.remove(RequestParamConstants.REQUEST);
        QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String renderOutput = "rendered output";
        when(mockViewHelper.render(anyMap(), eq("authorize.mustache"))).thenReturn(renderOutput);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertEquals(renderOutput, result);
        ArgumentCaptor<Map<String, String>> frontendParamsCaptor =
                ArgumentCaptor.forClass(Map.class);
        verify(mockViewHelper).render(frontendParamsCaptor.capture(), eq("authorize.mustache"));
        assertEquals(
                "Error: missing 'request' query parameter",
                frontendParamsCaptor.getValue().get("sharedAttributes"));
    }

    @Test
    void
            doAuthorizeShouldRenderMustacheTemplateWhenValidRequestReceivedWhenSignatureVerificationFails()
                    throws Exception {
        Map<String, String[]> queryParams = validDoAuthorizeQueryParams();
        String invalidSignatureJwt = signedRequestJwt(validClaimsSet()).serialize() + "Nope";
        queryParams.put(RequestParamConstants.REQUEST, new String[] {invalidSignatureJwt});
        QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String renderOutput = "rendered output";
        when(mockViewHelper.render(anyMap(), eq("authorize.mustache"))).thenReturn(renderOutput);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertEquals(renderOutput, result);
        ArgumentCaptor<Map<String, String>> frontendParamsCaptor =
                ArgumentCaptor.forClass(Map.class);
        verify(mockViewHelper).render(frontendParamsCaptor.capture(), eq("authorize.mustache"));
        assertEquals(
                "Error: Signature of the shared attribute JWT is not valid",
                frontendParamsCaptor.getValue().get("sharedAttributes"));
    }

    @Test
    void generateResponseShouldReturn302WithAuthCodeQueryParamWhenValidAuthRequest()
            throws Exception {
        QueryParamsMap queryParamsMap = toQueryParamsMap(validGenerateResponseQueryParams());
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String result =
                (String) authorizeHandler.generateResponse.handle(mockRequest, mockResponse);

        ArgumentCaptor<String> redirectUriCaptor = ArgumentCaptor.forClass(String.class);
        assertNull(result);
        verify(mockResponse).type(DEFAULT_RESPONSE_CONTENT_TYPE);
        verify(mockAuthCodeService)
                .persist(any(AuthorizationCode.class), anyString(), eq(TEST_REDIRECT_URI));
        verify(mockResponse).redirect(redirectUriCaptor.capture());
        assertNotNull(redirectUriCaptor.getValue());
    }

    @Test
    void generateResponseShouldCallDoAuthorizeMethodWhenInvalidJsonPayloadProvided()
            throws Exception {
        Map<String, String[]> queryParams = validGenerateResponseQueryParams();
        queryParams.put(RequestParamConstants.JSON_PAYLOAD, new String[] {"invalid-json"});
        QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);

        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        authorizeHandler.generateResponse.handle(mockRequest, mockResponse);

        verify(mockResponse)
                .redirect(
                        TEST_REDIRECT_URI
                                + "?error=invalid_json&iss=Credential+Issuer+Stub&error_description=Unable+to+generate+valid+JSON+Payload");
    }

    @Test
    void generateResponseShouldPersistSharedAttributesCombinedWithJsonInput() throws Exception {
        QueryParamsMap queryParamsMap = toQueryParamsMap(validGenerateResponseQueryParams());
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        authorizeHandler.generateResponse.handle(mockRequest, mockResponse);

        ArgumentCaptor<Credential> persistedCredential = ArgumentCaptor.forClass(Credential.class);

        verify(mockCredentialService)
                .persist(persistedCredential.capture(), eq("26c6ad15-a595-4e13-9497-f7c891fabe1d"));
        Map<String, Object> persistedAttributes = persistedCredential.getValue().getAttributes();
        assertEquals(persistedAttributes.get("addresses"), List.of("123 random street, M13 7GE"));
        assertEquals(persistedAttributes.get("test"), "test-value");
    }

    private String createExpectedErrorQueryStringParams(ErrorObject error) {
        return createExpectedErrorQueryStringParams(error.getCode(), error.getDescription());
    }

    private String createExpectedErrorQueryStringParams(String errorCode, String errorDesc) {
        return "?error="
                + errorCode
                + "&error_description="
                + URLEncoder.encode(errorDesc, StandardCharsets.UTF_8)
                + "&state=test-state";
    }

    private void invokeDoAuthorizeAndMakeAssertions(
            Map<String, String[]> queryParams, String expectedErrorCodeAndDescription)
            throws Exception {
        QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertNull(result);
        verify(mockViewHelper, never()).render(Collections.emptyMap(), "authorize.mustache");
        verify(mockResponse).type(DEFAULT_RESPONSE_CONTENT_TYPE);
        verify(mockResponse).redirect(TEST_REDIRECT_URI + expectedErrorCodeAndDescription);
    }

    private ECPrivateKey getPrivateKey() throws InvalidKeySpecException, NoSuchAlgorithmException {
        return (ECPrivateKey)
                KeyFactory.getInstance("EC")
                        .generatePrivate(
                                new PKCS8EncodedKeySpec(
                                        Base64.getDecoder().decode(TestFixtures.EC_PRIVATE_KEY_1)));
    }

    private QueryParamsMap toQueryParamsMap(Map<String, String[]> queryParams) {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
        when(mockHttpRequest.getParameterMap()).thenReturn(queryParams);
        return new QueryParamsMap(mockHttpRequest);
    }

    private Map<String, String[]> validDoAuthorizeQueryParams() throws Exception {
        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[] {"clientIdValid"});
        queryParams.put(RequestParamConstants.REDIRECT_URI, new String[] {TEST_REDIRECT_URI});
        queryParams.put(
                RequestParamConstants.RESPONSE_TYPE,
                new String[] {ResponseType.Value.CODE.getValue()});
        queryParams.put(RequestParamConstants.STATE, new String[] {"test-state"});
        queryParams.put(
                RequestParamConstants.REQUEST,
                new String[] {signedRequestJwt(validClaimsSet()).serialize()});
        return queryParams;
    }

    private Map<String, String[]> validGenerateResponseQueryParams() throws Exception {
        Map<String, String[]> queryParams = new HashMap<>(validDoAuthorizeQueryParams());
        queryParams.put(
                RequestParamConstants.JSON_PAYLOAD, new String[] {"{\"test\": \"test-value\"}"});
        queryParams.put(
                RequestParamConstants.RESOURCE_ID,
                new String[] {"26c6ad15-a595-4e13-9497-f7c891fabe1d"});
        queryParams.put(CredentialIssuerConfig.EVIDENCE_STRENGTH_PARAM, new String[] {"2"});
        queryParams.put(CredentialIssuerConfig.EVIDENCE_VALIDITY_PARAM, new String[] {"3"});
        return queryParams;
    }

    private JWTClaimsSet validClaimsSet() {
        Map<String, List<String>> vcHttpApiClaim = new LinkedHashMap<>();
        vcHttpApiClaim.put("dateOfBirths", Arrays.asList("01/01/1980", "02/01/1980"));
        vcHttpApiClaim.put("addresses", Collections.singletonList("123 random street, M13 7GE"));
        vcHttpApiClaim.put("givenNames", Arrays.asList("Daniel", "Dan", "Danny"));

        return new JWTClaimsSet.Builder()
                .claim(CLAIMS_CLAIM, Map.of(VC_HTTP_API_CLAIM, vcHttpApiClaim))
                .build();
    }

    private SignedJWT signedRequestJwt(JWTClaimsSet claimsSet) throws Exception {
        ECDSASigner ecdsaSigner = new ECDSASigner(getPrivateKey());
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.ES256), claimsSet);
        signedJWT.sign(ecdsaSigner);

        return signedJWT;
    }
}
