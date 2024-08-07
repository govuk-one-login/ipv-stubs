package uk.gov.di.ipv.stub.cred.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import uk.gov.di.ipv.stub.cred.domain.Credential;
import uk.gov.di.ipv.stub.cred.fixtures.TestFixtures;
import uk.gov.di.ipv.stub.cred.service.AuthCodeService;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.service.RequestedErrorResponseService;
import uk.gov.di.ipv.stub.cred.utils.StubSsmClient;
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

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.stub.cred.fixtures.TestFixtures.CLIENT_CONFIG;
import static uk.gov.di.ipv.stub.cred.fixtures.TestFixtures.DCMAW_VC;
import static uk.gov.di.ipv.stub.cred.handlers.AuthorizeHandler.CRI_MITIGATION_ENABLED_PARAM;
import static uk.gov.di.ipv.stub.cred.handlers.AuthorizeHandler.SHARED_CLAIMS;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.CIMIT_STUB_API_KEY;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.CIMIT_STUB_URL;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.MITIGATED_CI;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.STRENGTH;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VALIDITY;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VC_NOT_BEFORE_DAY;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VC_NOT_BEFORE_FLAG;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VC_NOT_BEFORE_HOURS;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VC_NOT_BEFORE_MINUTES;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VC_NOT_BEFORE_MONTH;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VC_NOT_BEFORE_SECONDS;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VC_NOT_BEFORE_YEAR;

@ExtendWith({SystemStubsExtension.class, MockitoExtension.class})
class AuthorizeHandlerTest {

    @SystemStub
    private EnvironmentVariables environmentVariables =
            new EnvironmentVariables(
                    "ENVIRONMENT", "TEST",
                    "F2F_STUB_QUEUE_URL", "https://example.com/stub-queue",
                    "F2F_STUB_QUEUE_API_KEY", "example-key");

    private static final String BASE64_ENCRYPTION_PUBLIC_CERT =
            "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUZEakNDQXZZQ0NRQ3JjK3ppU2ZNeUR6QU5CZ2txaGtpRzl3MEJBUXNGQURCSk1Rc3dDUVlEVlFRR0V3SkgKUWpFTk1Bc0dBMVVFQ0F3RVZHVnpkREVOTUFzR0ExVUVCd3dFVkdWemRERU5NQXNHQTFVRUNnd0VWRVZ6ZERFTgpNQXNHQTFVRUN3d0VWR1Z6ZERBZUZ3MHlNVEV5TWpNeE1EVTJNakZhRncweU1qRXlNak14TURVMk1qRmFNRWt4CkN6QUpCZ05WQkFZVEFrZENNUTB3Q3dZRFZRUUlEQVJVWlhOME1RMHdDd1lEVlFRSERBUlVaWE4wTVEwd0N3WUQKVlFRS0RBUlVSWE4wTVEwd0N3WURWUVFMREFSVVpYTjBNSUlDSWpBTkJna3Foa2lHOXcwQkFRRUZBQU9DQWc4QQpNSUlDQ2dLQ0FnRUF3RnJkUzhFUUNLaUQxNXJ1UkE3SFd5T0doeVZ0TlphV3JYOUVGZWNJZTZPQWJCRHhHS2NQCkJLbVJVMDNud3g1THppRWhNL2NlNWw0a3lTazcybFgwYSt6ZTVkb2pqZkx6dFJZcGdiSTlEYUVwMy9GTEdyWkoKRmpPZCtwaU9JZ1lBQms0YTVNdlBuOVlWeEpzNlh2aVFOZThJZVN6Y2xMR1dNV0dXOFRFTnBaMWJwRkNxa2FiRQpTN0cvdUVNMGtkaGhnYVpnVXhpK1JZUUhQcWhtNk1PZGdScWJpeTIxUDBOSFRFVktyaWtZanZYZXdTQnFtZ0xVClBRaTg1ME9qczF3UGRZVFRoajVCT2JZd3o5aEpWbWJIVEhvUGgwSDRGZGphMW9wY1M1ZXRvSGtOWU95MzdTbzgKQ2tzVjZzNnVyN3pVcWE5RlRMTXJNVnZhN2pvRHRzV2JXSjhsM2pheS9PSEV3UlI5RFNvTHVhYlppK2tWekZGUwp2eGRDTU52VzJEMmNSdzNHWW1HMGk4cXMxMXRsalFMTEV0S2EyWXJBZERSRXlFUFlKR1NYSjJDUXhqbGRpMzYrCmlHYitzNkExWVNCNzRxYldkbVcxWktqcGFPZmtmclRBZ3FocUc5UURrd2hPSk5CblVDUTBpZVpGYXV3MUZJM04KS0c1WEZSMzdKR05EL1luTGxCS1gzVzNMSGVIY1hTYUphYzYxOHFHbzgxVFduVzA2MVMzTGRVRWcyWGJ0SXJPKworNEdlNDlJbXRSTUFrcmhUUjAzMXc3ZDVnVXJtZWxCcTNzaVBmUmFkYmJ2OUM1VENHOG4zVDM1VkpLNFcybEduCkl5WUFzc09wYWxyN1Q5TmVuTzUxcUJmK2gyTjVVWitTVDV0TkYwM2s5enpKdGZORDZEcUNySHNDQXdFQUFUQU4KQmdrcWhraUc5dzBCQVFzRkFBT0NBZ0VBQWNjblhwYUNJaVNzcG5oZ0tlTk9iSm9aaUJzSWNyTU4wVU1tSmVaagpSNkM2MHQzM1lEZDhXR2VhOW91WmVUZEFYOFIxYTlZOVFtV3JMMnpUTXIwbEwxdkRleXd0eUtjTFloVmFZaHUrCi9ibVFKTjJ5TnhWdU9ONkxtbkhBUFBFdjBtc3RWM1JuQXVxYlcvTm5DU0ZkUnFsSmlYT2hRLzlQUHJUUDZzck8KT2QwVHJ6VkE3RXlQT014TjJpSUdBcTJRemFBb3B6VDFVNmF4bnpHRmZ6aTZVSGlRYURSbGhuODhGUEpNT3JMUQpyS3NlUkk4MUtIaGptZG5uOFdlWC9BaGZWSk8wejZ2TU1xRGx5QmlSUmV3VmVQcjZTejl5T2RCQVZlNFUzSDdHCmdDV3p2akEzYkxjZEpobUw4dHQvVFpFcndMblFDd2Izc3pMODNSSDl0dXIzaWdwQnJoUzlWWnM4ZldyeWY0MDgKNnU0dWd3Y1luT0NpaGtwMk9ESjVtOThCbmdZem1wT2NDZW1KTkg3WkJ1SWhDVkNjRitCejlBbTlRSjJXdzdFZApTeGNDcFQxY0hSd29Fd0I5a01ORmtpYlkzbFJBQ3BtTmQ3SWpWUU5ZNTlmeFBBdGo4cFlSYWJGa2JhSUtkT2FwCkxySE1jbmRCTXpMYkk1bGl1a2hQUTlGLyt5QkMybVRRZ0MvVzU5dThraW4yQTFRbDJRWUNXQzFYVWFXaXFxRVUKbVQ5SjU5L0dKZ3hIT1pNSXB4OERDK0ZYRDZkbEF1bUJLZzcxZnpsdjdNb3dKWWFFcFJEUlJubjU0YnQ4UmpVRwpRREpBV1VseHluSlF0dCtqdmFNR0lSZ2M2RkdJcUVVV1VzUU9wUDEwNFg4dUtPQWNSTjlmMWNSSGxTeUErTUp5Cnd1UT0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQo=";
    private static final String VALID_RESPONSE_TYPE = "code";
    private static final String INVALID_REDIRECT_URI = "invalid-redirect-uri";
    private static final String INVALID_RESPONSE_TYPE = "cosssde";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(INDENT_OUTPUT);
    private static final String APPLICATION_JSON = "application/json";
    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String VALID_REDIRECT_URI = "https://valid.example.com";

    @Mock private HttpClient mockHttpClient;
    @Mock private SignedJWT mockSignedJwt;
    @Mock private Response mockResponse;
    @Mock private Request mockRequest;
    @Mock private ViewHelper mockViewHelper;
    @Mock private AuthCodeService mockAuthCodeService;
    @Mock private CredentialService mockCredentialService;
    @Mock private VerifiableCredentialGenerator mockVcGenerator;

    @Spy
    private RequestedErrorResponseService requestedErrorResponseService =
            new RequestedErrorResponseService();

    @InjectMocks private AuthorizeHandler authorizeHandler;

    @Captor ArgumentCaptor<Map<String, Object>> viewParamsCaptor;
    @Captor ArgumentCaptor<Credential> credentialArgumentCaptor;
    @Captor ArgumentCaptor<HttpRequest> httpRequestArgumentCaptor;
    @Captor ArgumentCaptor<String> stringArgumentCaptor;
    @Captor ArgumentCaptor<AuthorizationCode> authCoreArgumentCaptor;

    @BeforeAll
    public static void beforeAllSetUp() {
        StubSsmClient.setClientConfigParams(CLIENT_CONFIG);
    }

    @Nested
    class doAuthorizeTests {
        @BeforeEach
        void setup() {
            environmentVariables.set("MITIGATION_ENABLED", "False");
        }

        @Test
        void doAuthorizeShouldRenderMustacheTemplateWhenValidRequestReceived() throws Exception {
            QueryParamsMap validQueryParamsMap = toQueryParamsMap(validDoAuthorizeQueryParams());
            when(mockRequest.queryMap()).thenReturn(validQueryParamsMap);

            String renderOutput = "rendered output";
            when(mockViewHelper.render(anyMap(), eq("authorize.mustache")))
                    .thenReturn(renderOutput);

            String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

            assertEquals(renderOutput, result);
            verify(mockViewHelper).render(anyMap(), eq("authorize.mustache"));
            verify(mockHttpClient, times(0)).send(any(), any());
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
        void doAuthorizeShouldReturn400WithErrorMessageWhenClientIdParamNotProvided()
                throws Exception {
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
                    "Error: Could not find client configuration details for: not-registered",
                    result);
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
            when(mockViewHelper.render(anyMap(), eq("authorize.mustache")))
                    .thenReturn(renderOutput);

            String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

            assertEquals(renderOutput, result);

            verify(mockViewHelper).render(viewParamsCaptor.capture(), eq("authorize.mustache"));

            assertTrue(
                    Boolean.parseBoolean(
                            viewParamsCaptor.getValue().get("isEvidenceType").toString()));

            Map<String, Object> claims = DefaultSharedClaims();
            assertEquals(
                    OBJECT_MAPPER.writeValueAsString(claims),
                    viewParamsCaptor.getValue().get("shared_claims"));
        }

        @Test
        void
                doAuthorizeShouldRenderMustacheTemplateWhenValidRequestReceivedWithEncryptedRequestJWT()
                        throws Exception {
            environmentVariables.set("CREDENTIAL_ISSUER_TYPE", "EVIDENCE");
            QueryParamsMap queryParamsMap =
                    toQueryParamsMap(validEncryptedDoAuthorizeQueryParams());
            when(mockRequest.queryMap()).thenReturn(queryParamsMap);

            String renderOutput = "rendered output";
            when(mockViewHelper.render(anyMap(), eq("authorize.mustache")))
                    .thenReturn(renderOutput);

            String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

            assertEquals(renderOutput, result);

            verify(mockViewHelper).render(viewParamsCaptor.capture(), eq("authorize.mustache"));

            assertTrue(
                    Boolean.parseBoolean(
                            viewParamsCaptor.getValue().get("isEvidenceType").toString()));

            Map<String, Object> claims = DefaultSharedClaims();
            assertEquals(
                    OBJECT_MAPPER.writeValueAsString(claims),
                    viewParamsCaptor.getValue().get("shared_claims"));
            assertFalse((Boolean) viewParamsCaptor.getValue().get(CRI_MITIGATION_ENABLED_PARAM));
        }

        @Test
        void doAuthorizeShouldRenderMustacheTemplateWhenValidRequestReceivedWithMitigationEnabled()
                throws Exception {
            environmentVariables.set("MITIGATION_ENABLED", "True");
            environmentVariables.set("CREDENTIAL_ISSUER_TYPE", "EVIDENCE");
            QueryParamsMap queryParamsMap =
                    toQueryParamsMap(validEncryptedDoAuthorizeQueryParams());
            when(mockRequest.queryMap()).thenReturn(queryParamsMap);

            String renderOutput = "rendered output";
            when(mockViewHelper.render(anyMap(), eq("authorize.mustache")))
                    .thenReturn(renderOutput);

            String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

            assertEquals(renderOutput, result);

            verify(mockViewHelper).render(viewParamsCaptor.capture(), eq("authorize.mustache"));

            assertTrue(
                    Boolean.parseBoolean(
                            viewParamsCaptor.getValue().get("isEvidenceType").toString()));

            assertTrue((Boolean) viewParamsCaptor.getValue().get(CRI_MITIGATION_ENABLED_PARAM));
        }

        @Test
        void
                doAuthorizeShouldRenderMustacheTemplateWhenValidRequestReceivedWhenSignatureVerificationFails()
                        throws Exception {
            environmentVariables.set("CREDENTIAL_ISSUER_TYPE", "EVIDENCE_DRIVING_LICENCE");
            Map<String, String[]> queryParams = validDoAuthorizeQueryParams();
            String signedJWT = signedRequestJwt(DefaultClaimSetBuilder().build()).serialize();
            String invalidSignatureJwt = signedJWT.substring(0, signedJWT.length() - 4) + "Nope";
            queryParams.put(RequestParamConstants.REQUEST, new String[] {invalidSignatureJwt});
            QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);
            when(mockRequest.queryMap()).thenReturn(queryParamsMap);

            String renderOutput = "rendered output";
            when(mockViewHelper.render(anyMap(), eq("authorize.mustache")))
                    .thenReturn(renderOutput);

            String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

            assertEquals(renderOutput, result);
            verify(mockViewHelper).render(viewParamsCaptor.capture(), eq("authorize.mustache"));
            assertEquals(
                    "Error: Signature of the shared attribute JWT is not valid",
                    viewParamsCaptor.getValue().get("shared_claims"));
        }

        @Test
        void doAuthorizeShouldUseDefaultScopeValueWhenNoScopeInRequest() throws Exception {
            // Arrange
            QueryParamsMap queryParamsMap =
                    toQueryParamsMap(validEncryptedDoAuthorizeQueryParams());
            when(mockRequest.queryMap()).thenReturn(queryParamsMap);

            String renderOutput = "rendered output";
            when(mockViewHelper.render(anyMap(), eq("authorize.mustache")))
                    .thenReturn(renderOutput);

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
            when(mockViewHelper.render(anyMap(), eq("authorize.mustache")))
                    .thenReturn(renderOutput);

            // Act
            String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

            // Assert
            verify(mockViewHelper).render(viewParamsCaptor.capture(), eq("authorize.mustache"));
            assertEquals("test scope", viewParamsCaptor.getValue().get("scope").toString());
        }

        @Test
        void doAuthorizeShouldUseDefaultContextValueWhenNoContextInRequest() throws Exception {
            // Arrange
            QueryParamsMap queryParamsMap =
                    toQueryParamsMap(validEncryptedDoAuthorizeQueryParams());
            when(mockRequest.queryMap()).thenReturn(queryParamsMap);

            String renderOutput = "rendered output";
            when(mockViewHelper.render(anyMap(), eq("authorize.mustache")))
                    .thenReturn(renderOutput);

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
            when(mockViewHelper.render(anyMap(), eq("authorize.mustache")))
                    .thenReturn(renderOutput);

            // Act
            String result = (String) authorizeHandler.doAuthorize.handle(mockRequest, mockResponse);

            // Assert
            verify(mockViewHelper).render(viewParamsCaptor.capture(), eq("authorize.mustache"));
            assertEquals("test context", viewParamsCaptor.getValue().get("context").toString());
        }
    }

    @Nested
    class FormAuthorizeTests {
        @BeforeEach
        void setup() {
            environmentVariables.set("MITIGATION_ENABLED", "False");
        }

        @Test
        void formAuthorizeShouldReturn302WithAuthCodeQueryParamWhenValidAuthRequest()
                throws Exception {
            QueryParamsMap queryParamsMap = toQueryParamsMap(validGenerateResponseQueryParams());
            when(mockRequest.queryMap()).thenReturn(queryParamsMap);
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

            String result =
                    (String) authorizeHandler.formAuthorize.handle(mockRequest, mockResponse);

            ArgumentCaptor<String> redirectUriCaptor = ArgumentCaptor.forClass(String.class);
            assertNull(result);
            verify(mockResponse).type(DEFAULT_RESPONSE_CONTENT_TYPE);
            verify(mockAuthCodeService)
                    .persist(any(AuthorizationCode.class), anyString(), eq(VALID_REDIRECT_URI));
            verify(mockResponse).redirect(redirectUriCaptor.capture());
            assertNotNull(redirectUriCaptor.getValue());
        }

        @Test
        void formAuthorizeShouldReturnErrorResponseWhenInvalidJsonPayloadProvided()
                throws Exception {
            Map<String, String[]> queryParams = validGenerateResponseQueryParams();
            queryParams.put(RequestParamConstants.JSON_PAYLOAD, new String[] {"invalid-json"});
            QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);

            when(mockRequest.queryMap()).thenReturn(queryParamsMap);

            authorizeHandler.formAuthorize.handle(mockRequest, mockResponse);

            verify(mockResponse)
                    .redirect(
                            VALID_REDIRECT_URI
                                    + "?error=invalid_json&iss=Credential+Issuer+Stub&error_description=Unable+to+generate+valid+JSON+Payload");
        }

        @Test
        void formAuthorizeShouldPersistSharedAttributesCombinedWithJsonInput() throws Exception {
            QueryParamsMap queryParamsMap = toQueryParamsMap(validGenerateResponseQueryParams());
            when(mockRequest.queryMap()).thenReturn(queryParamsMap);
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);
            when(mockSignedJwt.serialize()).thenReturn(DCMAW_VC);

            authorizeHandler.formAuthorize.handle(mockRequest, mockResponse);

            ArgumentCaptor<Credential> persistedCredential =
                    ArgumentCaptor.forClass(Credential.class);

            verify(mockVcGenerator).generate(persistedCredential.capture());
            Map<String, Object> persistedAttributes =
                    persistedCredential.getValue().getAttributes();
            Map<String, Object> persistedEvidence = persistedCredential.getValue().getEvidence();
            assertEquals(
                    List.of("123 random street, M13 7GE"), persistedAttributes.get("addresses"));
            assertEquals("test-value", persistedAttributes.get("test"));
            assertEquals("IdentityCheck", persistedEvidence.get("type"));
            assertNotNull(persistedEvidence.get("txn"));
            assertNotNull(persistedEvidence.get("strengthScore"));
            assertNotNull(persistedEvidence.get("validityScore"));

            verify(mockCredentialService).persist(eq(mockSignedJwt.serialize()), any(String.class));
        }

        @Test
        void formAuthorizeShouldPersistSharedAttributesCombinedWithJsonInput_withMitigationEnabled()
                throws Exception {
            environmentVariables.set("MITIGATION_ENABLED", "True");
            Map<String, String[]> queryParams = validGenerateResponseQueryParams();
            queryParams.put(MITIGATED_CI, new String[] {"V03"});
            queryParams.put(CIMIT_STUB_URL, new String[] {"http://test.com"});
            queryParams.put(CIMIT_STUB_API_KEY, new String[] {"api:key"});
            QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);
            when(mockRequest.queryMap()).thenReturn(queryParamsMap);

            HttpResponse httpResponse = mock(HttpResponse.class);
            when(httpResponse.statusCode()).thenReturn(200);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);
            when(mockSignedJwt.serialize()).thenReturn(DCMAW_VC);
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

            authorizeHandler.formAuthorize.handle(mockRequest, mockResponse);

            ArgumentCaptor<HttpRequest> requestArgumentCaptor =
                    ArgumentCaptor.forClass(HttpRequest.class);
            verify(mockHttpClient, times(1)).send(requestArgumentCaptor.capture(), any());
            // This is a poor proxy for checking the content of the request. We can't access the
            // content
            // directly.
            // The content is:
            // '{"mitigations":["M01"],"vcJti","urn:uuid:e937e812-dafe-42e4-a094-6c9d41fee50c\n"}'
            // If this changes, this test will fail.
            assertEquals(
                    79, requestArgumentCaptor.getValue().bodyPublisher().get().contentLength());

            ArgumentCaptor<Credential> persistedCredential =
                    ArgumentCaptor.forClass(Credential.class);

            verify(mockVcGenerator).generate(persistedCredential.capture());
            verify(mockCredentialService).persist(eq(mockSignedJwt.serialize()), any(String.class));
        }

        @Test
        void
                formAuthorizeShouldPersistSharedAttributesCombinedWithJsonInput_withMitigationEnabled_postFailed()
                        throws Exception {
            environmentVariables.set("MITIGATION_ENABLED", "True");
            Map<String, String[]> queryParams = validGenerateResponseQueryParams();
            queryParams.put(MITIGATED_CI, new String[] {"V03"});
            queryParams.put(CIMIT_STUB_URL, new String[] {"http://test.com"});
            queryParams.put(CIMIT_STUB_API_KEY, new String[] {"api:key"});
            QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);
            when(mockRequest.queryMap()).thenReturn(queryParamsMap);
            when(mockSignedJwt.serialize()).thenReturn(DCMAW_VC);
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

            HttpResponse httpResponse = mock(HttpResponse.class);
            when(httpResponse.statusCode()).thenReturn(403);
            when(httpResponse.body()).thenReturn("Access denied");
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            authorizeHandler.formAuthorize.handle(mockRequest, mockResponse);

            verify(mockHttpClient, times(1)).send(any(), any());

            verify(mockResponse)
                    .redirect(
                            VALID_REDIRECT_URI
                                    + "?error=failed_to_post&iss=Credential+Issuer+Stub&error_description=Failed+to+post+CI+mitigation+to+management+stub+api.");
        }

        @Test
        void formAuthorizeShouldRedirectWithRequestedOAuthErrorResponse() throws Exception {
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

            authorizeHandler.formAuthorize.handle(mockRequest, mockResponse);

            verify(mockResponse)
                    .redirect(
                            VALID_REDIRECT_URI
                                    + "?iss=Credential+Issuer+Stub&state=test-state&error=invalid_request&error_description=An+error+description");
        }

        @Test
        void formAuthorizeShouldNotIncludeSharedAttributesForUserAssertedCriType()
                throws Exception {
            environmentVariables.set("CREDENTIAL_ISSUER_TYPE", "USER_ASSERTED");
            Map<String, String[]> queryParams = validGenerateResponseQueryParams();
            queryParams.put("ci", new String[] {""});
            QueryParamsMap queryParamsMap = toQueryParamsMap(queryParams);
            when(mockRequest.queryMap()).thenReturn(queryParamsMap);
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

            authorizeHandler.formAuthorize.handle(mockRequest, mockResponse);

            ArgumentCaptor<Credential> persistedCredential =
                    ArgumentCaptor.forClass(Credential.class);

            verify(mockVcGenerator).generate(persistedCredential.capture());
            Map<String, Object> persistedAttributes =
                    persistedCredential.getValue().getAttributes();
            assertNull(persistedAttributes.get("addresses"));
            assertNull(persistedAttributes.get("names"));
            assertNull(persistedAttributes.get("birthDate"));
            assertEquals("test-value", persistedAttributes.get("test"));

            verify(mockCredentialService).persist(eq(mockSignedJwt.serialize()), any(String.class));
        }
    }

    @Nested
    class ApiAuthorizeTests {
        @BeforeEach
        void setup() {
            environmentVariables.set("MITIGATION_ENABLED", "False");
        }

        @Test
        void apiAuthorizeShouldReturn302WithAuthCodeQueryParamWhenValidAuthRequest()
                throws Exception {
            when(mockRequest.body())
                    .thenReturn(
                            "{"
                                    + "  \"clientId\": \"clientIdValid\","
                                    + "  \"request\": \""
                                    + signedRequestJwt(DefaultClaimSetBuilder().build()).serialize()
                                    + "\","
                                    + "  \"credentialSubjectJson\": \"{\\\"passport\\\":[{\\\"expiryDate\\\":\\\"2030-01-01\\\",\\\"icaoIssuerCode\\\":\\\"GBR\\\",\\\"documentNumber\\\":\\\"321654987\\\"}],\\\"name\\\":[{\\\"nameParts\\\":[{\\\"type\\\":\\\"GivenName\\\",\\\"value\\\":\\\"Kenneth\\\"},{\\\"type\\\":\\\"FamilyName\\\",\\\"value\\\":\\\"Decerqueira\\\"}]}],\\\"birthDate\\\":[{\\\"value\\\":\\\"1965-07-08\\\"}]}\","
                                    + "  \"evidenceJson\": \"{\\\"activityHistoryScore\\\":1,\\\"checkDetails\\\":[{\\\"checkMethod\\\":\\\"vri\\\"},{\\\"biometricVerificationProcessLevel\\\":3,\\\"checkMethod\\\":\\\"bvr\\\"}],\\\"validityScore\\\":2,\\\"strengthScore\\\":3,\\\"type\\\":\\\"IdentityCheck\\\"}\""
                                    + "}");
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

            authorizeHandler.apiAuthorize.handle(mockRequest, mockResponse);

            verify(mockResponse).type(APPLICATION_JSON);
            verify(mockAuthCodeService)
                    .persist(authCoreArgumentCaptor.capture(), anyString(), eq(VALID_REDIRECT_URI));
            verify(mockResponse).redirect(stringArgumentCaptor.capture());
            assertTrue(
                    stringArgumentCaptor
                            .getValue()
                            .contains(authCoreArgumentCaptor.getValue().getValue()));
        }

        @Test
        void apiAuthorizeShouldAllowNbfToBeSet() throws Exception {
            when(mockRequest.body())
                    .thenReturn(
                            "{"
                                    + "  \"clientId\": \"clientIdValid\","
                                    + "  \"request\": \""
                                    + signedRequestJwt(DefaultClaimSetBuilder().build()).serialize()
                                    + "\","
                                    + "  \"credentialSubjectJson\": \"{\\\"passport\\\":[{\\\"expiryDate\\\":\\\"2030-01-01\\\",\\\"icaoIssuerCode\\\":\\\"GBR\\\",\\\"documentNumber\\\":\\\"321654987\\\"}],\\\"name\\\":[{\\\"nameParts\\\":[{\\\"type\\\":\\\"GivenName\\\",\\\"value\\\":\\\"Kenneth\\\"},{\\\"type\\\":\\\"FamilyName\\\",\\\"value\\\":\\\"Decerqueira\\\"}]}],\\\"birthDate\\\":[{\\\"value\\\":\\\"1965-07-08\\\"}]}\","
                                    + "  \"evidenceJson\": \"{\\\"activityHistoryScore\\\":1,\\\"checkDetails\\\":[{\\\"checkMethod\\\":\\\"vri\\\"},{\\\"biometricVerificationProcessLevel\\\":3,\\\"checkMethod\\\":\\\"bvr\\\"}],\\\"validityScore\\\":2,\\\"strengthScore\\\":3,\\\"type\\\":\\\"IdentityCheck\\\"}\","
                                    + "  \"nbf\": 1714577018"
                                    + "}");
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

            authorizeHandler.apiAuthorize.handle(mockRequest, mockResponse);

            verify(mockResponse).type(APPLICATION_JSON);
            verify(mockAuthCodeService)
                    .persist(authCoreArgumentCaptor.capture(), anyString(), eq(VALID_REDIRECT_URI));
            verify(mockResponse).redirect(stringArgumentCaptor.capture());
            assertTrue(
                    stringArgumentCaptor
                            .getValue()
                            .contains(authCoreArgumentCaptor.getValue().getValue()));

            verify(mockVcGenerator).generate(credentialArgumentCaptor.capture());
            assertEquals(1714577018L, credentialArgumentCaptor.getValue().getNbf());
        }

        @Test
        void apiAuthorizeShouldAllowVcToBeSentToF2fQueue() throws Exception {
            when(mockRequest.body())
                    .thenReturn(
                            "{"
                                    + "  \"clientId\": \"clientIdValid\","
                                    + "  \"request\": \""
                                    + signedRequestJwt(DefaultClaimSetBuilder().build()).serialize()
                                    + "\","
                                    + "  \"credentialSubjectJson\": \"{\\\"passport\\\":[{\\\"expiryDate\\\":\\\"2030-01-01\\\",\\\"icaoIssuerCode\\\":\\\"GBR\\\",\\\"documentNumber\\\":\\\"321654987\\\"}],\\\"name\\\":[{\\\"nameParts\\\":[{\\\"type\\\":\\\"GivenName\\\",\\\"value\\\":\\\"Kenneth\\\"},{\\\"type\\\":\\\"FamilyName\\\",\\\"value\\\":\\\"Decerqueira\\\"}]}],\\\"birthDate\\\":[{\\\"value\\\":\\\"1965-07-08\\\"}]}\","
                                    + "  \"evidenceJson\": \"{\\\"activityHistoryScore\\\":1,\\\"checkDetails\\\":[{\\\"checkMethod\\\":\\\"vri\\\"},{\\\"biometricVerificationProcessLevel\\\":3,\\\"checkMethod\\\":\\\"bvr\\\"}],\\\"validityScore\\\":2,\\\"strengthScore\\\":3,\\\"type\\\":\\\"IdentityCheck\\\"}\","
                                    + "  \"f2f\": {"
                                    + "    \"sendVcToQueue\": true,"
                                    + "    \"queueName\": \"stubQueue_F2FQueue_build\""
                                    + "  }"
                                    + "}");
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);
            when(mockSignedJwt.serialize()).thenReturn(DCMAW_VC);
            var httpResponse = mock(HttpResponse.class);
            when(httpResponse.statusCode()).thenReturn(200);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            authorizeHandler.apiAuthorize.handle(mockRequest, mockResponse);

            verify(mockHttpClient).send(httpRequestArgumentCaptor.capture(), any());
            assertEquals(
                    "https://example.com/stub-queue",
                    httpRequestArgumentCaptor.getValue().uri().toString());
        }

        @Test
        void apiAuthorizeShouldAllowErrorToBeSentToF2fQueue() throws Exception {
            when(mockRequest.body())
                    .thenReturn(
                            "{"
                                    + "  \"clientId\": \"clientIdValid\","
                                    + "  \"request\": \""
                                    + signedRequestJwt(DefaultClaimSetBuilder().build()).serialize()
                                    + "\","
                                    + "  \"credentialSubjectJson\": \"{\\\"passport\\\":[{\\\"expiryDate\\\":\\\"2030-01-01\\\",\\\"icaoIssuerCode\\\":\\\"GBR\\\",\\\"documentNumber\\\":\\\"321654987\\\"}],\\\"name\\\":[{\\\"nameParts\\\":[{\\\"type\\\":\\\"GivenName\\\",\\\"value\\\":\\\"Kenneth\\\"},{\\\"type\\\":\\\"FamilyName\\\",\\\"value\\\":\\\"Decerqueira\\\"}]}],\\\"birthDate\\\":[{\\\"value\\\":\\\"1965-07-08\\\"}]}\","
                                    + "  \"evidenceJson\": \"{\\\"activityHistoryScore\\\":1,\\\"checkDetails\\\":[{\\\"checkMethod\\\":\\\"vri\\\"},{\\\"biometricVerificationProcessLevel\\\":3,\\\"checkMethod\\\":\\\"bvr\\\"}],\\\"validityScore\\\":2,\\\"strengthScore\\\":3,\\\"type\\\":\\\"IdentityCheck\\\"}\","
                                    + "  \"f2f\": {"
                                    + "    \"sendErrorToQueue\": true,"
                                    + "    \"queueName\": \"stubQueue_F2FQueue_build\""
                                    + "  }"
                                    + "}");
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);
            when(mockSignedJwt.serialize()).thenReturn(DCMAW_VC);
            var httpResponse = mock(HttpResponse.class);
            when(httpResponse.statusCode()).thenReturn(200);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            authorizeHandler.apiAuthorize.handle(mockRequest, mockResponse);

            verify(mockHttpClient).send(httpRequestArgumentCaptor.capture(), any());
            assertEquals(
                    "https://example.com/stub-queue",
                    httpRequestArgumentCaptor.getValue().uri().toString());
        }

        @Test
        void apiAuthorizeShouldReturnErrorResponseWhenInvalidCredentialSubjectJsonProvided()
                throws Exception {
            when(mockRequest.body())
                    .thenReturn(
                            "{"
                                    + "  \"clientId\": \"clientIdValid\","
                                    + "  \"request\": \""
                                    + signedRequestJwt(DefaultClaimSetBuilder().build()).serialize()
                                    + "\","
                                    + "  \"credentialSubjectJson\": \"This is not JSON\","
                                    + "  \"evidenceJson\": \"{\\\"activityHistoryScore\\\":1,\\\"checkDetails\\\":[{\\\"checkMethod\\\":\\\"vri\\\"},{\\\"biometricVerificationProcessLevel\\\":3,\\\"checkMethod\\\":\\\"bvr\\\"}],\\\"validityScore\\\":2,\\\"strengthScore\\\":3,\\\"type\\\":\\\"IdentityCheck\\\"}\""
                                    + "}");

            authorizeHandler.apiAuthorize.handle(mockRequest, mockResponse);

            verify(mockResponse)
                    .redirect(
                            VALID_REDIRECT_URI
                                    + "?error=invalid_json&iss=Credential+Issuer+Stub&error_description=Unable+to+generate+valid+JSON+Payload");
        }

        @Test
        void apiAuthorizeShouldPersistSharedAttributesCombinedWithJsonInput() throws Exception {
            when(mockRequest.body())
                    .thenReturn(
                            "{"
                                    + "  \"clientId\": \"clientIdValid\","
                                    + "  \"request\": \""
                                    + signedRequestJwt(DefaultClaimSetBuilder().build()).serialize()
                                    + "\","
                                    + "  \"credentialSubjectJson\": \"{\\\"passport\\\":[{\\\"expiryDate\\\":\\\"2030-01-01\\\",\\\"icaoIssuerCode\\\":\\\"GBR\\\",\\\"documentNumber\\\":\\\"321654987\\\"}],\\\"name\\\":[{\\\"nameParts\\\":[{\\\"type\\\":\\\"GivenName\\\",\\\"value\\\":\\\"Kenneth\\\"},{\\\"type\\\":\\\"FamilyName\\\",\\\"value\\\":\\\"Decerqueira\\\"}]}],\\\"birthDate\\\":[{\\\"value\\\":\\\"1965-07-08\\\"}]}\","
                                    + "  \"evidenceJson\": \"{\\\"activityHistoryScore\\\":1,\\\"checkDetails\\\":[{\\\"checkMethod\\\":\\\"vri\\\"},{\\\"biometricVerificationProcessLevel\\\":3,\\\"checkMethod\\\":\\\"bvr\\\"}],\\\"validityScore\\\":2,\\\"strengthScore\\\":3,\\\"type\\\":\\\"IdentityCheck\\\"}\""
                                    + "}");
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

            authorizeHandler.apiAuthorize.handle(mockRequest, mockResponse);

            ArgumentCaptor<Credential> persistedCredential =
                    ArgumentCaptor.forClass(Credential.class);

            verify(mockVcGenerator).generate(persistedCredential.capture());
            Map<String, Object> persistedAttributes =
                    persistedCredential.getValue().getAttributes();
            Map<String, Object> persistedEvidence = persistedCredential.getValue().getEvidence();
            assertEquals(
                    List.of("123 random street, M13 7GE"), persistedAttributes.get("addresses"));
            assertEquals(
                    List.of(Map.of("value", "1965-07-08")), persistedAttributes.get("birthDate"));
            assertEquals("IdentityCheck", persistedEvidence.get("type"));
            assertNotNull(persistedEvidence.get("txn"));
            assertNotNull(persistedEvidence.get("strengthScore"));
            assertNotNull(persistedEvidence.get("validityScore"));

            verify(mockCredentialService).persist(eq(mockSignedJwt.serialize()), any(String.class));
        }

        @Test
        void apiAuthorizeShouldAllowMitigationsWhenEnabled() throws Exception {
            environmentVariables.set("MITIGATION_ENABLED", "True");
            when(mockRequest.body())
                    .thenReturn(
                            "{"
                                    + "  \"clientId\": \"clientIdValid\","
                                    + "  \"request\": \""
                                    + signedRequestJwt(DefaultClaimSetBuilder().build()).serialize()
                                    + "\","
                                    + "  \"credentialSubjectJson\": \"{\\\"passport\\\":[{\\\"expiryDate\\\":\\\"2030-01-01\\\",\\\"icaoIssuerCode\\\":\\\"GBR\\\",\\\"documentNumber\\\":\\\"321654987\\\"}],\\\"name\\\":[{\\\"nameParts\\\":[{\\\"type\\\":\\\"GivenName\\\",\\\"value\\\":\\\"Kenneth\\\"},{\\\"type\\\":\\\"FamilyName\\\",\\\"value\\\":\\\"Decerqueira\\\"}]}],\\\"birthDate\\\":[{\\\"value\\\":\\\"1965-07-08\\\"}]}\","
                                    + "  \"evidenceJson\": \"{\\\"activityHistoryScore\\\":1,\\\"checkDetails\\\":[{\\\"checkMethod\\\":\\\"vri\\\"},{\\\"biometricVerificationProcessLevel\\\":3,\\\"checkMethod\\\":\\\"bvr\\\"}],\\\"validityScore\\\":2,\\\"strengthScore\\\":3,\\\"type\\\":\\\"IdentityCheck\\\"}\","
                                    + "  \"mitigations\": {"
                                    + "    \"mitigatedCi\": [\"XX\"],"
                                    + "    \"cimitStubUrl\": \"https://cimit.stubs.account.gov.uk\","
                                    + "    \"cimitStubApiKey\": \"anAPIKey\""
                                    + "  }"
                                    + "}");

            var httpResponse = mock(HttpResponse.class);
            when(httpResponse.statusCode()).thenReturn(200);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);
            when(mockSignedJwt.serialize()).thenReturn(DCMAW_VC);
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

            authorizeHandler.apiAuthorize.handle(mockRequest, mockResponse);

            ArgumentCaptor<HttpRequest> requestArgumentCaptor =
                    ArgumentCaptor.forClass(HttpRequest.class);
            verify(mockHttpClient, times(1)).send(requestArgumentCaptor.capture(), any());
            // This is a poor proxy for checking the content of the request. We can't access the
            // content
            // directly.
            // The content is:
            // '{"mitigations":["M01"],"vcJti","urn:uuid:e937e812-dafe-42e4-a094-6c9d41fee50c\n"}'
            // If this changes, this test will fail.
            assertEquals(
                    79, requestArgumentCaptor.getValue().bodyPublisher().get().contentLength());

            ArgumentCaptor<Credential> persistedCredential =
                    ArgumentCaptor.forClass(Credential.class);

            verify(mockVcGenerator).generate(persistedCredential.capture());
            verify(mockCredentialService).persist(eq(mockSignedJwt.serialize()), any(String.class));
        }

        @Test
        void apiAuthorizeShouldHandleNoMitigationsWhenMitigationsEnabled() throws Exception {
            environmentVariables.set("MITIGATION_ENABLED", "True");
            when(mockRequest.body())
                    .thenReturn(
                            "{"
                                    + "  \"clientId\": \"clientIdValid\","
                                    + "  \"request\": \""
                                    + signedRequestJwt(DefaultClaimSetBuilder().build()).serialize()
                                    + "\","
                                    + "  \"credentialSubjectJson\": \"{\\\"passport\\\":[{\\\"expiryDate\\\":\\\"2030-01-01\\\",\\\"icaoIssuerCode\\\":\\\"GBR\\\",\\\"documentNumber\\\":\\\"321654987\\\"}],\\\"name\\\":[{\\\"nameParts\\\":[{\\\"type\\\":\\\"GivenName\\\",\\\"value\\\":\\\"Kenneth\\\"},{\\\"type\\\":\\\"FamilyName\\\",\\\"value\\\":\\\"Decerqueira\\\"}]}],\\\"birthDate\\\":[{\\\"value\\\":\\\"1965-07-08\\\"}]}\","
                                    + "  \"evidenceJson\": \"{\\\"activityHistoryScore\\\":1,\\\"checkDetails\\\":[{\\\"checkMethod\\\":\\\"vri\\\"},{\\\"biometricVerificationProcessLevel\\\":3,\\\"checkMethod\\\":\\\"bvr\\\"}],\\\"validityScore\\\":2,\\\"strengthScore\\\":3,\\\"type\\\":\\\"IdentityCheck\\\"}\""
                                    + "}");
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

            authorizeHandler.apiAuthorize.handle(mockRequest, mockResponse);

            verify(mockResponse).type(APPLICATION_JSON);
            verify(mockAuthCodeService)
                    .persist(authCoreArgumentCaptor.capture(), anyString(), eq(VALID_REDIRECT_URI));
            verify(mockResponse).redirect(stringArgumentCaptor.capture());
            assertTrue(
                    stringArgumentCaptor
                            .getValue()
                            .contains(authCoreArgumentCaptor.getValue().getValue()));
        }

        @Test
        void apiAuthorizeShouldRedirectWithRequestedOAuthErrorResponse() throws Exception {
            when(mockRequest.body())
                    .thenReturn(
                            "{"
                                    + "  \"clientId\": \"clientIdValid\","
                                    + "  \"request\": \""
                                    + signedRequestJwt(DefaultClaimSetBuilder().build()).serialize()
                                    + "\","
                                    + "  \"requestedError\": {"
                                    + "    \"endpoint\": \"auth\","
                                    + "    \"error\": \"invalid_request\","
                                    + "    \"description\": \"a bad thing happened\""
                                    + "  }"
                                    + "}");

            authorizeHandler.apiAuthorize.handle(mockRequest, mockResponse);

            verify(mockResponse)
                    .redirect(
                            VALID_REDIRECT_URI
                                    + "?iss=Credential+Issuer+Stub&state=test-state&error=invalid_request&error_description=a+bad+thing+happened");
        }

        @Test
        void apiAuthorizeShouldAllowRequestedOAuthErrorResponseFromTokenEndpoint()
                throws Exception {
            when(mockRequest.body())
                    .thenReturn(
                            "{"
                                    + "  \"clientId\": \"clientIdValid\","
                                    + "  \"request\": \""
                                    + signedRequestJwt(DefaultClaimSetBuilder().build()).serialize()
                                    + "\","
                                    + "  \"credentialSubjectJson\": \"{\\\"passport\\\":[{\\\"expiryDate\\\":\\\"2030-01-01\\\",\\\"icaoIssuerCode\\\":\\\"GBR\\\",\\\"documentNumber\\\":\\\"321654987\\\"}],\\\"name\\\":[{\\\"nameParts\\\":[{\\\"type\\\":\\\"GivenName\\\",\\\"value\\\":\\\"Kenneth\\\"},{\\\"type\\\":\\\"FamilyName\\\",\\\"value\\\":\\\"Decerqueira\\\"}]}],\\\"birthDate\\\":[{\\\"value\\\":\\\"1965-07-08\\\"}]}\","
                                    + "  \"evidenceJson\": \"{\\\"activityHistoryScore\\\":1,\\\"checkDetails\\\":[{\\\"checkMethod\\\":\\\"vri\\\"},{\\\"biometricVerificationProcessLevel\\\":3,\\\"checkMethod\\\":\\\"bvr\\\"}],\\\"validityScore\\\":2,\\\"strengthScore\\\":3,\\\"type\\\":\\\"IdentityCheck\\\"}\","
                                    + "  \"requestedError\": {"
                                    + "    \"endpoint\": \"token\","
                                    + "    \"error\": \"invalid_request\","
                                    + "    \"description\": \"a bad thing happened at the token endpoint\""
                                    + "  }"
                                    + "}");
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

            authorizeHandler.apiAuthorize.handle(mockRequest, mockResponse);

            verify(requestedErrorResponseService).persist(stringArgumentCaptor.capture(), any());
            var tokenErrorResponse =
                    requestedErrorResponseService
                            .getRequestedAccessTokenErrorResponse(stringArgumentCaptor.getValue())
                            .getErrorObject();
            assertEquals("invalid_request", tokenErrorResponse.getCode());
            assertEquals(
                    "a bad thing happened at the token endpoint",
                    tokenErrorResponse.getDescription());
        }

        @Test
        void apiAuthorizeShouldAllowRequestedOAuthErrorResponseFromCredentialEndpoint()
                throws Exception {
            when(mockRequest.body())
                    .thenReturn(
                            "{"
                                    + "  \"clientId\": \"clientIdValid\","
                                    + "  \"request\": \""
                                    + signedRequestJwt(DefaultClaimSetBuilder().build()).serialize()
                                    + "\","
                                    + "  \"credentialSubjectJson\": \"{\\\"passport\\\":[{\\\"expiryDate\\\":\\\"2030-01-01\\\",\\\"icaoIssuerCode\\\":\\\"GBR\\\",\\\"documentNumber\\\":\\\"321654987\\\"}],\\\"name\\\":[{\\\"nameParts\\\":[{\\\"type\\\":\\\"GivenName\\\",\\\"value\\\":\\\"Kenneth\\\"},{\\\"type\\\":\\\"FamilyName\\\",\\\"value\\\":\\\"Decerqueira\\\"}]}],\\\"birthDate\\\":[{\\\"value\\\":\\\"1965-07-08\\\"}]}\","
                                    + "  \"evidenceJson\": \"{\\\"activityHistoryScore\\\":1,\\\"checkDetails\\\":[{\\\"checkMethod\\\":\\\"vri\\\"},{\\\"biometricVerificationProcessLevel\\\":3,\\\"checkMethod\\\":\\\"bvr\\\"}],\\\"validityScore\\\":2,\\\"strengthScore\\\":3,\\\"type\\\":\\\"IdentityCheck\\\"}\","
                                    + "  \"requestedError\": {"
                                    + "    \"userInfoError\": \"404\""
                                    + "  }"
                                    + "}");
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

            authorizeHandler.apiAuthorize.handle(mockRequest, mockResponse);

            verify(requestedErrorResponseService).persist(stringArgumentCaptor.capture(), any());
            var credentialErrorResponse =
                    requestedErrorResponseService
                            .getUserInfoErrorByToken(stringArgumentCaptor.getValue())
                            .getErrorObject();
            assertEquals("404", credentialErrorResponse.getCode());
            assertEquals(
                    "UserInfo endpoint 404 triggered by stub",
                    credentialErrorResponse.getDescription());
        }
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
        queryParams.put(STRENGTH, new String[] {"2"});
        queryParams.put(VALIDITY, new String[] {"3"});
        queryParams.put(VC_NOT_BEFORE_FLAG, new String[] {"on"});
        queryParams.put(VC_NOT_BEFORE_DAY, new String[] {"1"});
        queryParams.put(VC_NOT_BEFORE_MONTH, new String[] {"1"});
        queryParams.put(VC_NOT_BEFORE_YEAR, new String[] {"2022"});
        queryParams.put(VC_NOT_BEFORE_HOURS, new String[] {"0"});
        queryParams.put(VC_NOT_BEFORE_MINUTES, new String[] {"0"});
        queryParams.put(VC_NOT_BEFORE_SECONDS, new String[] {"0"});
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
