package uk.gov.di.ipv.stub.cred.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.domain.Credential;
import uk.gov.di.ipv.stub.cred.fixtures.TestFixtures;
import uk.gov.di.ipv.stub.cred.service.AuthCodeService;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.service.RequestedErrorResponseService;
import uk.gov.di.ipv.stub.cred.utils.ViewHelper;
import uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialGenerator;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.stub.cred.fixtures.TestFixtures.DCMAW_VC;
import static uk.gov.di.ipv.stub.cred.handlers.AuthorizeHandler.CRI_MITIGATION_ENABLED_PARAM;
import static uk.gov.di.ipv.stub.cred.handlers.AuthorizeHandler.SHARED_CLAIMS;

@ExtendWith({SystemStubsExtension.class, MockitoExtension.class})
class AuthorizeHandlerTest {

    public static final String BASE64_ENCRYPTION_PUBLIC_CERT =
            "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUZEakNDQXZZQ0NRQ3JjK3ppU2ZNeUR6QU5CZ2txaGtpRzl3MEJBUXNGQURCSk1Rc3dDUVlEVlFRR0V3SkgKUWpFTk1Bc0dBMVVFQ0F3RVZHVnpkREVOTUFzR0ExVUVCd3dFVkdWemRERU5NQXNHQTFVRUNnd0VWRVZ6ZERFTgpNQXNHQTFVRUN3d0VWR1Z6ZERBZUZ3MHlNVEV5TWpNeE1EVTJNakZhRncweU1qRXlNak14TURVMk1qRmFNRWt4CkN6QUpCZ05WQkFZVEFrZENNUTB3Q3dZRFZRUUlEQVJVWlhOME1RMHdDd1lEVlFRSERBUlVaWE4wTVEwd0N3WUQKVlFRS0RBUlVSWE4wTVEwd0N3WURWUVFMREFSVVpYTjBNSUlDSWpBTkJna3Foa2lHOXcwQkFRRUZBQU9DQWc4QQpNSUlDQ2dLQ0FnRUF3RnJkUzhFUUNLaUQxNXJ1UkE3SFd5T0doeVZ0TlphV3JYOUVGZWNJZTZPQWJCRHhHS2NQCkJLbVJVMDNud3g1THppRWhNL2NlNWw0a3lTazcybFgwYSt6ZTVkb2pqZkx6dFJZcGdiSTlEYUVwMy9GTEdyWkoKRmpPZCtwaU9JZ1lBQms0YTVNdlBuOVlWeEpzNlh2aVFOZThJZVN6Y2xMR1dNV0dXOFRFTnBaMWJwRkNxa2FiRQpTN0cvdUVNMGtkaGhnYVpnVXhpK1JZUUhQcWhtNk1PZGdScWJpeTIxUDBOSFRFVktyaWtZanZYZXdTQnFtZ0xVClBRaTg1ME9qczF3UGRZVFRoajVCT2JZd3o5aEpWbWJIVEhvUGgwSDRGZGphMW9wY1M1ZXRvSGtOWU95MzdTbzgKQ2tzVjZzNnVyN3pVcWE5RlRMTXJNVnZhN2pvRHRzV2JXSjhsM2pheS9PSEV3UlI5RFNvTHVhYlppK2tWekZGUwp2eGRDTU52VzJEMmNSdzNHWW1HMGk4cXMxMXRsalFMTEV0S2EyWXJBZERSRXlFUFlKR1NYSjJDUXhqbGRpMzYrCmlHYitzNkExWVNCNzRxYldkbVcxWktqcGFPZmtmclRBZ3FocUc5UURrd2hPSk5CblVDUTBpZVpGYXV3MUZJM04KS0c1WEZSMzdKR05EL1luTGxCS1gzVzNMSGVIY1hTYUphYzYxOHFHbzgxVFduVzA2MVMzTGRVRWcyWGJ0SXJPKworNEdlNDlJbXRSTUFrcmhUUjAzMXc3ZDVnVXJtZWxCcTNzaVBmUmFkYmJ2OUM1VENHOG4zVDM1VkpLNFcybEduCkl5WUFzc09wYWxyN1Q5TmVuTzUxcUJmK2gyTjVVWitTVDV0TkYwM2s5enpKdGZORDZEcUNySHNDQXdFQUFUQU4KQmdrcWhraUc5dzBCQVFzRkFBT0NBZ0VBQWNjblhwYUNJaVNzcG5oZ0tlTk9iSm9aaUJzSWNyTU4wVU1tSmVaagpSNkM2MHQzM1lEZDhXR2VhOW91WmVUZEFYOFIxYTlZOVFtV3JMMnpUTXIwbEwxdkRleXd0eUtjTFloVmFZaHUrCi9ibVFKTjJ5TnhWdU9ONkxtbkhBUFBFdjBtc3RWM1JuQXVxYlcvTm5DU0ZkUnFsSmlYT2hRLzlQUHJUUDZzck8KT2QwVHJ6VkE3RXlQT014TjJpSUdBcTJRemFBb3B6VDFVNmF4bnpHRmZ6aTZVSGlRYURSbGhuODhGUEpNT3JMUQpyS3NlUkk4MUtIaGptZG5uOFdlWC9BaGZWSk8wejZ2TU1xRGx5QmlSUmV3VmVQcjZTejl5T2RCQVZlNFUzSDdHCmdDV3p2akEzYkxjZEpobUw4dHQvVFpFcndMblFDd2Izc3pMODNSSDl0dXIzaWdwQnJoUzlWWnM4ZldyeWY0MDgKNnU0dWd3Y1luT0NpaGtwMk9ESjVtOThCbmdZem1wT2NDZW1KTkg3WkJ1SWhDVkNjRitCejlBbTlRSjJXdzdFZApTeGNDcFQxY0hSd29Fd0I5a01ORmtpYlkzbFJBQ3BtTmQ3SWpWUU5ZNTlmeFBBdGo4cFlSYWJGa2JhSUtkT2FwCkxySE1jbmRCTXpMYkk1bGl1a2hQUTlGLyt5QkMybVRRZ0MvVzU5dThraW4yQTFRbDJRWUNXQzFYVWFXaXFxRVUKbVQ5SjU5L0dKZ3hIT1pNSXB4OERDK0ZYRDZkbEF1bUJLZzcxZnpsdjdNb3dKWWFFcFJEUlJubjU0YnQ4UmpVRwpRREpBV1VseHluSlF0dCtqdmFNR0lSZ2M2RkdJcUVVV1VzUU9wUDEwNFg4dUtPQWNSTjlmMWNSSGxTeUErTUp5Cnd1UT0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQo=";

    public static final String VALID_RESPONSE_TYPE = "code";
    public static final String INVALID_REDIRECT_URI = "invalid-redirect-uri";
    public static final String INVALID_RESPONSE_TYPE = "cosssde";
    private Response mockResponse;
    private Request mockRequest;
    private ViewHelper mockViewHelper;
    private AuthorizeHandler authorizeHandler;
    private AuthCodeService mockAuthCodeService;
    private CredentialService mockCredentialService;
    private VerifiableCredentialGenerator mockVcGenerator;
    private RequestedErrorResponseService requestedErrorResponseService =
            new RequestedErrorResponseService();

    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String VALID_REDIRECT_URI = "https://valid.example.com";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Mock private HttpClient httpClient;
    @Mock private SignedJWT mockSignedJwt;

    @SystemStub
    private EnvironmentVariables environmentVariables =
            new EnvironmentVariables("CLIENT_CONFIG", TestFixtures.CLIENT_CONFIG);

    @Captor ArgumentCaptor<Map<String, Object>> viewParamsCaptor;

    @BeforeEach
    void setup() {
        CredentialIssuerConfig.resetClientConfigs();

        mockResponse = mock(Response.class);
        mockRequest = mock(Request.class);
        mockViewHelper = mock(ViewHelper.class);
        mockAuthCodeService = mock(AuthCodeService.class);
        mockCredentialService = mock(CredentialService.class);
        mockVcGenerator = mock(VerifiableCredentialGenerator.class);
        httpClient = mock(HttpClient.class);

        authorizeHandler =
                new AuthorizeHandler(
                        mockViewHelper,
                        mockAuthCodeService,
                        mockCredentialService,
                        requestedErrorResponseService,
                        mockVcGenerator,
                        httpClient);
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
        verify(httpClient, times(0)).send(any(), any());
    }

    @Test
    void doAuthorizeShouldReturn400WhenRedirectUriParamNotRegistered() throws Exception {
        Map<String, String[]> queryParams = invalidRedirectUriDoAuthorizeQueryParams();
        QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);

        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertEquals(
                "redirect_uri param provided does not match any of the redirect_uri values configured",
                result);
        verify(mockViewHelper, never()).render(Collections.emptyMap(), "authorize.mustache");
        verify(mockResponse).status(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    void doAuthorizeShouldReturn302WithErrorQueryParamsWhenResponseTypeParamNotCode()
            throws Exception {
        Map<String, String[]> queryParams = invalidResponseTypeDoAuthorizeQueryParams();

        invokeDoAuthorizeAndMakeAssertions(
                queryParams,
                createExpectedErrorQueryStringParams(OAuth2Error.UNSUPPORTED_RESPONSE_TYPE));
    }

    @Test
    void doAuthorizeShouldReturn400WithErrorMessageWhenClientIdParamNotProvided() throws Exception {
        Map<String, String[]> queryParams = validDoAuthorizeQueryParams();
        queryParams.remove(RequestParamConstants.CLIENT_ID);

        QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertEquals("Error: Could not find client configuration details for: null", result);
        verify(mockResponse).status(400);
    }

    @Test
    void doAuthorizeShouldReturn400WithErrorMessagesWhenClientIdParamNotRegistered()
            throws Exception {
        Map<String, String[]> queryParams = validDoAuthorizeQueryParams();
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[] {"not-registered"});

        QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertEquals(
                "Error: Could not find client configuration details for: not-registered", result);
        verify(mockResponse).status(400);
    }

    @Test
    void doAuthorizeShouldReturn400WithErrorMessagesWhenRequestParamMissing() throws Exception {
        Map<String, String[]> queryParams = validDoAuthorizeQueryParams();
        queryParams.remove(RequestParamConstants.REQUEST);

        QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertEquals("request param must be provided", result);
        verify(mockResponse).status(400);
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

        verify(mockViewHelper).render(viewParamsCaptor.capture(), eq("authorize.mustache"));

        assertTrue(
                Boolean.parseBoolean(viewParamsCaptor.getValue().get("isEvidenceType").toString()));

        Map<String, Object> claims = DefaultSharedClaims();
        assertEquals(gson.toJson(claims), viewParamsCaptor.getValue().get("shared_claims"));
    }

    @Test
    void doAuthorizeShouldRenderMustacheTemplateWhenValidRequestReceivedWithEncryptedRequestJWT()
            throws Exception {
        QueryParamsMap queryParamsMap = toQueryParamsMap(validEncryptedDoAuthorizeQueryParams());
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String renderOutput = "rendered output";
        when(mockViewHelper.render(anyMap(), eq("authorize.mustache"))).thenReturn(renderOutput);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertEquals(renderOutput, result);

        verify(mockViewHelper).render(viewParamsCaptor.capture(), eq("authorize.mustache"));

        assertTrue(
                Boolean.parseBoolean(viewParamsCaptor.getValue().get("isEvidenceType").toString()));

        Map<String, Object> claims = DefaultSharedClaims();
        assertEquals(gson.toJson(claims), viewParamsCaptor.getValue().get("shared_claims"));
        assertFalse((Boolean) viewParamsCaptor.getValue().get(CRI_MITIGATION_ENABLED_PARAM));
    }

    @Test
    void doAuthorizeShouldRenderMustacheTemplateWhenValidRequestReceivedWithMitigationEnabled()
            throws Exception {
        environmentVariables.set("MITIGATION_ENABLED", "True");
        QueryParamsMap queryParamsMap = toQueryParamsMap(validEncryptedDoAuthorizeQueryParams());
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String renderOutput = "rendered output";
        when(mockViewHelper.render(anyMap(), eq("authorize.mustache"))).thenReturn(renderOutput);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertEquals(renderOutput, result);

        verify(mockViewHelper).render(viewParamsCaptor.capture(), eq("authorize.mustache"));

        assertTrue(
                Boolean.parseBoolean(viewParamsCaptor.getValue().get("isEvidenceType").toString()));

        assertTrue((Boolean) viewParamsCaptor.getValue().get(CRI_MITIGATION_ENABLED_PARAM));
    }

    @Test
    void
            doAuthorizeShouldRenderMustacheTemplateWhenValidRequestReceivedWhenSignatureVerificationFails()
                    throws Exception {
        Map<String, String[]> queryParams = validDoAuthorizeQueryParams();
        String signedJWT = signedRequestJwt(DefaultClaimSetBuilder().build()).serialize();
        String invalidSignatureJwt = signedJWT.substring(0, signedJWT.length() - 4) + "Nope";
        queryParams.put(RequestParamConstants.REQUEST, new String[] {invalidSignatureJwt});
        QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String renderOutput = "rendered output";
        when(mockViewHelper.render(anyMap(), eq("authorize.mustache"))).thenReturn(renderOutput);

        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        assertEquals(renderOutput, result);
        verify(mockViewHelper).render(viewParamsCaptor.capture(), eq("authorize.mustache"));
        assertEquals(
                "Error: Signature of the shared attribute JWT is not valid",
                viewParamsCaptor.getValue().get("shared_claims"));
    }

    @Test
    void generateResponseShouldReturn302WithAuthCodeQueryParamWhenValidAuthRequest()
            throws Exception {
        Map<String, String[]> queryParams = validGenerateResponseQueryParams();
        queryParams.put(
                CredentialIssuerConfig.BASE_STUB_MANAGED_POST_URL_PARAM,
                new String[] {"http://test.com"});
        queryParams.put(
                CredentialIssuerConfig.STUB_MANAGEMENT_API_KEY_PARAM, new String[] {"api:key"});
        QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);
        when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(mockSignedJwt.serialize()).thenReturn(DCMAW_VC);

        String result =
                (String) authorizeHandler.generateResponse.handle(mockRequest, mockResponse);

        ArgumentCaptor<String> redirectUriCaptor = ArgumentCaptor.forClass(String.class);
        assertNull(result);
        verify(mockResponse).type(DEFAULT_RESPONSE_CONTENT_TYPE);
        verify(mockAuthCodeService)
                .persist(any(AuthorizationCode.class), anyString(), eq(VALID_REDIRECT_URI));
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
                        VALID_REDIRECT_URI
                                + "?error=invalid_json&iss=Credential+Issuer+Stub&error_description=Unable+to+generate+valid+JSON+Payload");
    }

    @Test
    void generateResponseShouldPersistSharedAttributesCombinedWithJsonInput() throws Exception {
        Map<String, String[]> queryParams = validGenerateResponseQueryParams();
        queryParams.put(
                CredentialIssuerConfig.MITIGATED_CONTRAINDICATORS_PARAM, new String[] {"V03"});
        queryParams.put(
                CredentialIssuerConfig.BASE_STUB_MANAGED_POST_URL_PARAM,
                new String[] {"http://test.com"});
        queryParams.put(
                CredentialIssuerConfig.STUB_MANAGEMENT_API_KEY_PARAM, new String[] {"api:key"});
        QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

        authorizeHandler.generateResponse.handle(mockRequest, mockResponse);

        ArgumentCaptor<Credential> persistedCredential = ArgumentCaptor.forClass(Credential.class);

        verify(mockVcGenerator).generate(persistedCredential.capture());
        Map<String, Object> persistedAttributes = persistedCredential.getValue().getAttributes();
        Map<String, Object> persistedEvidence = persistedCredential.getValue().getEvidence();
        assertEquals(List.of("123 random street, M13 7GE"), persistedAttributes.get("addresses"));
        assertEquals("test-value", persistedAttributes.get("test"));
        assertEquals("IdentityCheck", persistedEvidence.get("type"));
        assertNotNull(persistedEvidence.get("txn"));
        assertNotNull(persistedEvidence.get("strengthScore"));
        assertNotNull(persistedEvidence.get("validityScore"));
        assertNotNull(persistedCredential.getValue().getExp());

        verify(mockCredentialService)
                .persist(eq(mockSignedJwt.serialize()), eq("26c6ad15-a595-4e13-9497-f7c891fabe1d"));
    }

    @Test
    void generateResponseShouldPersistSharedAttributesCombinedWithJsonInput_withoutVCExp()
            throws Exception {
        Map<String, String[]> queryParams = validGenerateResponseQueryParams();
        queryParams.remove(CredentialIssuerConfig.EXPIRY_FLAG);
        queryParams.put(
                CredentialIssuerConfig.BASE_STUB_MANAGED_POST_URL_PARAM,
                new String[] {"http://test.com"});
        queryParams.put(
                CredentialIssuerConfig.STUB_MANAGEMENT_API_KEY_PARAM, new String[] {"api:key"});
        QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);
        when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        authorizeHandler.generateResponse.handle(mockRequest, mockResponse);

        ArgumentCaptor<Credential> persistedCredential = ArgumentCaptor.forClass(Credential.class);

        verify(mockVcGenerator).generate(persistedCredential.capture());
        Map<String, Object> persistedAttributes = persistedCredential.getValue().getAttributes();
        Map<String, Object> persistedEvidence = persistedCredential.getValue().getEvidence();
        assertEquals(List.of("123 random street, M13 7GE"), persistedAttributes.get("addresses"));
        assertEquals("test-value", persistedAttributes.get("test"));
        assertEquals("IdentityCheck", persistedEvidence.get("type"));
        assertNotNull(persistedEvidence.get("txn"));
        assertNotNull(persistedEvidence.get("strengthScore"));
        assertNotNull(persistedEvidence.get("validityScore"));
        assertNull(persistedCredential.getValue().getExp());

        verify(mockCredentialService)
                .persist(eq(mockSignedJwt.serialize()), eq("26c6ad15-a595-4e13-9497-f7c891fabe1d"));
    }

    @Test
    void generateResponseShouldPersistSharedAttributesCombinedWithJsonInput_withMitigationEnabled()
            throws Exception {
        environmentVariables.set("MITIGATION_ENABLED", "True");
        Map<String, String[]> queryParams = validGenerateResponseQueryParams();
        queryParams.put(
                CredentialIssuerConfig.MITIGATED_CONTRAINDICATORS_PARAM, new String[] {"V03"});
        queryParams.put(
                CredentialIssuerConfig.BASE_STUB_MANAGED_POST_URL_PARAM,
                new String[] {"http://test.com"});
        queryParams.put(
                CredentialIssuerConfig.STUB_MANAGEMENT_API_KEY_PARAM, new String[] {"api:key"});
        QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(mockSignedJwt.serialize()).thenReturn(DCMAW_VC);
        when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

        authorizeHandler.generateResponse.handle(mockRequest, mockResponse);

        ArgumentCaptor<HttpRequest> requestArgumentCaptor =
                ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(requestArgumentCaptor.capture(), any());
        // This is a poor proxy for checking the content of the request. We can't access the content
        // directly.
        // The content is:
        // '{"mitigations":["M01"],"vcJti","urn:uuid:e937e812-dafe-42e4-a094-6c9d41fee50c\n"}'
        // If this changes, this test will fail.
        assertEquals(79, requestArgumentCaptor.getValue().bodyPublisher().get().contentLength());

        ArgumentCaptor<Credential> persistedCredential = ArgumentCaptor.forClass(Credential.class);

        verify(mockVcGenerator).generate(persistedCredential.capture());
        verify(mockCredentialService)
                .persist(eq(mockSignedJwt.serialize()), eq("26c6ad15-a595-4e13-9497-f7c891fabe1d"));

        assertNotNull(persistedCredential.getValue().getExp());
    }

    @Test
    void
            generateResponseShouldPersistSharedAttributesCombinedWithJsonInput_withMitigationEnabled_postFailed()
                    throws Exception {
        environmentVariables.set("MITIGATION_ENABLED", "True");
        Map<String, String[]> queryParams = validGenerateResponseQueryParams();
        queryParams.put(
                CredentialIssuerConfig.MITIGATED_CONTRAINDICATORS_PARAM, new String[] {"V03"});
        queryParams.put(
                CredentialIssuerConfig.BASE_STUB_MANAGED_POST_URL_PARAM,
                new String[] {"http://test.com"});
        queryParams.put(
                CredentialIssuerConfig.STUB_MANAGEMENT_API_KEY_PARAM, new String[] {"api:key"});
        QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(403);
        when(httpResponse.body()).thenReturn("Access denied");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        authorizeHandler.generateResponse.handle(mockRequest, mockResponse);

        verify(httpClient, times(1)).send(any(), any());
    }

    @Test
    void generateResponseShouldRedirectWithRequestedOAuthErrorResponse() throws Exception {
        Map<String, String[]> queryParams = validGenerateResponseQueryParams();
        queryParams.put(
                RequestParamConstants.REQUESTED_OAUTH_ERROR, new String[] {"invalid_request"});
        queryParams.put(
                RequestParamConstants.REQUESTED_OAUTH_ERROR_ENDPOINT, new String[] {"auth"});
        queryParams.put(
                RequestParamConstants.REQUESTED_OAUTH_ERROR_DESCRIPTION,
                new String[] {"An error description"});
        QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);

        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        authorizeHandler.generateResponse.handle(mockRequest, mockResponse);

        verify(mockResponse)
                .redirect(
                        VALID_REDIRECT_URI
                                + "?iss=Credential+Issuer+Stub&state=test-state&error=invalid_request&error_description=An+error+description");
    }

    @Test
    void doAuthorizeShouldUseDefaultScopeValueWhenNoScopeInRequest() throws Exception {

        // Arrange
        QueryParamsMap queryParamsMap = toQueryParamsMap(validEncryptedDoAuthorizeQueryParams());
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String renderOutput = "rendered output";
        when(mockViewHelper.render(anyMap(), eq("authorize.mustache"))).thenReturn(renderOutput);

        // Act
        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        // Assert
        verify(mockViewHelper).render(viewParamsCaptor.capture(), eq("authorize.mustache"));
        assertEquals(
                "No scope provided in request",
                viewParamsCaptor.getValue().get("scope").toString());
    }

    @Test
    void doAuthorizeShouldUseRequestScopeValueWhenScopeInRequest() throws Exception {

        // Arrange
        var claimsSet = DefaultClaimSetBuilder().claim("scope", "test scope").build();
        QueryParamsMap queryParamsMap =
                toQueryParamsMap(validEncryptedDoAuthorizeQueryParams(claimsSet));
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String renderOutput = "rendered output";
        when(mockViewHelper.render(anyMap(), eq("authorize.mustache"))).thenReturn(renderOutput);

        // Act
        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        // Assert
        verify(mockViewHelper).render(viewParamsCaptor.capture(), eq("authorize.mustache"));
        assertEquals("test scope", viewParamsCaptor.getValue().get("scope").toString());
    }

    @Test
    void doAuthorizeShouldUseDefaultContextValueWhenNoContextInRequest() throws Exception {

        // Arrange
        QueryParamsMap queryParamsMap = toQueryParamsMap(validEncryptedDoAuthorizeQueryParams());
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String renderOutput = "rendered output";
        when(mockViewHelper.render(anyMap(), eq("authorize.mustache"))).thenReturn(renderOutput);

        // Act
        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        // Assert
        verify(mockViewHelper).render(viewParamsCaptor.capture(), eq("authorize.mustache"));
        assertEquals(
                "No context provided in request",
                viewParamsCaptor.getValue().get("context").toString());
    }

    @Test
    void doAuthorizeShouldUseRequestContextValueWhenContextInRequest() throws Exception {

        // Arrange
        var claimsSet = DefaultClaimSetBuilder().claim("context", "test context").build();
        QueryParamsMap queryParamsMap =
                toQueryParamsMap(validEncryptedDoAuthorizeQueryParams(claimsSet));
        when(mockRequest.queryMap()).thenReturn(queryParamsMap);

        String renderOutput = "rendered output";
        when(mockViewHelper.render(anyMap(), eq("authorize.mustache"))).thenReturn(renderOutput);

        // Act
        String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

        // Assert
        verify(mockViewHelper).render(viewParamsCaptor.capture(), eq("authorize.mustache"));
        assertEquals("test context", viewParamsCaptor.getValue().get("context").toString());
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
        verify(mockResponse).redirect(VALID_REDIRECT_URI + expectedErrorCodeAndDescription);
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
        queryParams.put(RequestParamConstants.REQUESTED_OAUTH_ERROR, new String[] {"none"});
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[] {"clientIdValid"});
        queryParams.put(
                RequestParamConstants.REQUEST,
                new String[] {signedRequestJwt(DefaultClaimSetBuilder().build()).serialize()});
        return queryParams;
    }

    private Map<String, String[]> validEncryptedDoAuthorizeQueryParams() throws Exception {
        return validEncryptedDoAuthorizeQueryParams(DefaultClaimSetBuilder().build());
    }

    private Map<String, String[]> validEncryptedDoAuthorizeQueryParams(JWTClaimsSet claimsSet)
            throws Exception {
        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.REQUESTED_OAUTH_ERROR, new String[] {"none"});
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[] {"clientIdValid"});
        queryParams.put(
                RequestParamConstants.REQUEST,
                new String[] {encryptedRequestJwt(signedRequestJwt(claimsSet)).serialize()});
        return queryParams;
    }

    private JWEObject encryptedRequestJwt(SignedJWT validRequestJWT)
            throws CertificateException, JOSEException {
        JWEHeader header =
                new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A128CBC_HS256)
                        .type(new JOSEObjectType("JWE"))
                        .build();
        JWEObject jweObject = new JWEObject(header, new Payload(validRequestJWT.serialize()));
        jweObject.encrypt(new RSAEncrypter((RSAPublicKey) getEncryptionPublicKey().getPublicKey()));
        return jweObject;
    }

    private Certificate getEncryptionPublicKey() throws CertificateException {
        byte[] binaryCertificate = Base64.getDecoder().decode(BASE64_ENCRYPTION_PUBLIC_CERT);
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return factory.generateCertificate(new ByteArrayInputStream(binaryCertificate));
    }

    private Map<String, String[]> invalidResponseTypeDoAuthorizeQueryParams() throws Exception {
        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.REQUESTED_OAUTH_ERROR, new String[] {"none"});
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[] {"clientIdValid"});
        queryParams.put(
                RequestParamConstants.REQUEST,
                new String[] {
                    signedRequestJwt(
                                    DefaultClaimSetBuilder(
                                                    INVALID_RESPONSE_TYPE, VALID_REDIRECT_URI)
                                            .build())
                            .serialize()
                });
        return queryParams;
    }

    private Map<String, String[]> invalidRedirectUriDoAuthorizeQueryParams() throws Exception {
        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put(RequestParamConstants.REQUESTED_OAUTH_ERROR, new String[] {"none"});
        queryParams.put(RequestParamConstants.CLIENT_ID, new String[] {"clientIdValid"});
        queryParams.put(
                RequestParamConstants.REQUEST,
                new String[] {
                    signedRequestJwt(
                                    DefaultClaimSetBuilder(
                                                    VALID_RESPONSE_TYPE, INVALID_REDIRECT_URI)
                                            .build())
                            .serialize()
                });
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
        queryParams.put(
                CredentialIssuerConfig.EVIDENCE_CONTRAINDICATOR_PARAM, new String[] {"A01, D03"});
        queryParams.put(
                CredentialIssuerConfig.EXPIRY_FLAG,
                new String[] {CredentialIssuerConfig.EXPIRY_FLAG_CHK_BOX_VALUE});
        queryParams.put(CredentialIssuerConfig.EXPIRY_HOURS, new String[] {"5"});
        queryParams.put(CredentialIssuerConfig.EXPIRY_MINUTES, new String[] {"0"});
        queryParams.put(CredentialIssuerConfig.EXPIRY_SECONDS, new String[] {"0"});
        return queryParams;
    }

    private JWTClaimsSet.Builder DefaultClaimSetBuilder() {
        return DefaultClaimSetBuilder(VALID_RESPONSE_TYPE, VALID_REDIRECT_URI);
    }

    private JWTClaimsSet.Builder DefaultClaimSetBuilder(String responseType, String redirectUri) {
        Instant instant = Instant.now();

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
                .claim(SHARED_CLAIMS, DefaultSharedClaims());
    }

    private Map<String, Object> DefaultSharedClaims() {
        Map<String, Object> sharedClaims = new LinkedHashMap<>();
        sharedClaims.put("addresses", Collections.singletonList("123 random street, M13 7GE"));
        sharedClaims.put(
                "names",
                List.of(
                        Map.of(
                                "nameParts",
                                Arrays.asList(
                                        Map.of("value", "Daniel"),
                                        Map.of("value", "Dan"),
                                        Map.of("value", "Danny")))));
        sharedClaims.put("birthDate", List.of(Map.of("value", "01/01/1980")));

        return sharedClaims;
    }

    private SignedJWT signedRequestJwt(JWTClaimsSet claimsSet) throws Exception {
        ECDSASigner ecdsaSigner = new ECDSASigner(getPrivateKey());
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.ES256), claimsSet);
        signedJWT.sign(ecdsaSigner);

        return signedJWT;
    }
}
