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
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.di.ipv.stub.cred.domain.ApiAuthRequest;
import uk.gov.di.ipv.stub.cred.domain.Credential;
import uk.gov.di.ipv.stub.cred.domain.F2fDetails;
import uk.gov.di.ipv.stub.cred.domain.Mitigations;
import uk.gov.di.ipv.stub.cred.domain.RequestedError;
import uk.gov.di.ipv.stub.cred.fixtures.TestFixtures;
import uk.gov.di.ipv.stub.cred.service.AuthCodeService;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.service.RequestedErrorResponseService;
import uk.gov.di.ipv.stub.cred.utils.StubSsmClient;
import uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialGenerator;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.stub.cred.fixtures.TestFixtures.CLIENT_CONFIG;
import static uk.gov.di.ipv.stub.cred.fixtures.TestFixtures.DCMAW_VC;
import static uk.gov.di.ipv.stub.cred.fixtures.TestFixtures.RSA_PRIVATE_KEY_JWK;
import static uk.gov.di.ipv.stub.cred.handlers.AuthorizeHandler.CRI_MITIGATION_ENABLED_PARAM;
import static uk.gov.di.ipv.stub.cred.handlers.AuthorizeHandler.SHARED_CLAIMS;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.CIMIT_STUB_API_KEY;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.CIMIT_STUB_URL;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.CLIENT_ID;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.JSON_PAYLOAD;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.MITIGATED_CI;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.REQUEST;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.REQUESTED_OAUTH_ERROR;
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
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthorizeHandlerTest {

    @SystemStub
    private static final EnvironmentVariables ENVIRONMENT_VARIABLES =
            new EnvironmentVariables(
                    "ENVIRONMENT", "TEST",
                    "F2F_STUB_QUEUE_URL", "https://example.com/stub-queue",
                    "F2F_STUB_QUEUE_API_KEY", "example-key",
                    "PRIVATE_ENCRYPTION_KEY_JWK", RSA_PRIVATE_KEY_JWK);

    private static final String BASE64_ENCRYPTION_PUBLIC_CERT =
            "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUZEakNDQXZZQ0NRQ3JjK3ppU2ZNeUR6QU5CZ2txaGtpRzl3MEJBUXNGQURCSk1Rc3dDUVlEVlFRR0V3SkgKUWpFTk1Bc0dBMVVFQ0F3RVZHVnpkREVOTUFzR0ExVUVCd3dFVkdWemRERU5NQXNHQTFVRUNnd0VWRVZ6ZERFTgpNQXNHQTFVRUN3d0VWR1Z6ZERBZUZ3MHlNVEV5TWpNeE1EVTJNakZhRncweU1qRXlNak14TURVMk1qRmFNRWt4CkN6QUpCZ05WQkFZVEFrZENNUTB3Q3dZRFZRUUlEQVJVWlhOME1RMHdDd1lEVlFRSERBUlVaWE4wTVEwd0N3WUQKVlFRS0RBUlVSWE4wTVEwd0N3WURWUVFMREFSVVpYTjBNSUlDSWpBTkJna3Foa2lHOXcwQkFRRUZBQU9DQWc4QQpNSUlDQ2dLQ0FnRUF3RnJkUzhFUUNLaUQxNXJ1UkE3SFd5T0doeVZ0TlphV3JYOUVGZWNJZTZPQWJCRHhHS2NQCkJLbVJVMDNud3g1THppRWhNL2NlNWw0a3lTazcybFgwYSt6ZTVkb2pqZkx6dFJZcGdiSTlEYUVwMy9GTEdyWkoKRmpPZCtwaU9JZ1lBQms0YTVNdlBuOVlWeEpzNlh2aVFOZThJZVN6Y2xMR1dNV0dXOFRFTnBaMWJwRkNxa2FiRQpTN0cvdUVNMGtkaGhnYVpnVXhpK1JZUUhQcWhtNk1PZGdScWJpeTIxUDBOSFRFVktyaWtZanZYZXdTQnFtZ0xVClBRaTg1ME9qczF3UGRZVFRoajVCT2JZd3o5aEpWbWJIVEhvUGgwSDRGZGphMW9wY1M1ZXRvSGtOWU95MzdTbzgKQ2tzVjZzNnVyN3pVcWE5RlRMTXJNVnZhN2pvRHRzV2JXSjhsM2pheS9PSEV3UlI5RFNvTHVhYlppK2tWekZGUwp2eGRDTU52VzJEMmNSdzNHWW1HMGk4cXMxMXRsalFMTEV0S2EyWXJBZERSRXlFUFlKR1NYSjJDUXhqbGRpMzYrCmlHYitzNkExWVNCNzRxYldkbVcxWktqcGFPZmtmclRBZ3FocUc5UURrd2hPSk5CblVDUTBpZVpGYXV3MUZJM04KS0c1WEZSMzdKR05EL1luTGxCS1gzVzNMSGVIY1hTYUphYzYxOHFHbzgxVFduVzA2MVMzTGRVRWcyWGJ0SXJPKworNEdlNDlJbXRSTUFrcmhUUjAzMXc3ZDVnVXJtZWxCcTNzaVBmUmFkYmJ2OUM1VENHOG4zVDM1VkpLNFcybEduCkl5WUFzc09wYWxyN1Q5TmVuTzUxcUJmK2gyTjVVWitTVDV0TkYwM2s5enpKdGZORDZEcUNySHNDQXdFQUFUQU4KQmdrcWhraUc5dzBCQVFzRkFBT0NBZ0VBQWNjblhwYUNJaVNzcG5oZ0tlTk9iSm9aaUJzSWNyTU4wVU1tSmVaagpSNkM2MHQzM1lEZDhXR2VhOW91WmVUZEFYOFIxYTlZOVFtV3JMMnpUTXIwbEwxdkRleXd0eUtjTFloVmFZaHUrCi9ibVFKTjJ5TnhWdU9ONkxtbkhBUFBFdjBtc3RWM1JuQXVxYlcvTm5DU0ZkUnFsSmlYT2hRLzlQUHJUUDZzck8KT2QwVHJ6VkE3RXlQT014TjJpSUdBcTJRemFBb3B6VDFVNmF4bnpHRmZ6aTZVSGlRYURSbGhuODhGUEpNT3JMUQpyS3NlUkk4MUtIaGptZG5uOFdlWC9BaGZWSk8wejZ2TU1xRGx5QmlSUmV3VmVQcjZTejl5T2RCQVZlNFUzSDdHCmdDV3p2akEzYkxjZEpobUw4dHQvVFpFcndMblFDd2Izc3pMODNSSDl0dXIzaWdwQnJoUzlWWnM4ZldyeWY0MDgKNnU0dWd3Y1luT0NpaGtwMk9ESjVtOThCbmdZem1wT2NDZW1KTkg3WkJ1SWhDVkNjRitCejlBbTlRSjJXdzdFZApTeGNDcFQxY0hSd29Fd0I5a01ORmtpYlkzbFJBQ3BtTmQ3SWpWUU5ZNTlmeFBBdGo4cFlSYWJGa2JhSUtkT2FwCkxySE1jbmRCTXpMYkk1bGl1a2hQUTlGLyt5QkMybVRRZ0MvVzU5dThraW4yQTFRbDJRWUNXQzFYVWFXaXFxRVUKbVQ5SjU5L0dKZ3hIT1pNSXB4OERDK0ZYRDZkbEF1bUJLZzcxZnpsdjdNb3dKWWFFcFJEUlJubjU0YnQ4UmpVRwpRREpBV1VseHluSlF0dCtqdmFNR0lSZ2M2RkdJcUVVV1VzUU9wUDEwNFg4dUtPQWNSTjlmMWNSSGxTeUErTUp5Cnd1UT0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQo="; // pragma: allowlist secret
    private static final String VALID_RESPONSE_TYPE = "code";
    private static final String INVALID_REDIRECT_URI = "invalid-redirect-uri";
    private static final String INVALID_RESPONSE_TYPE = "cosssde";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(INDENT_OUTPUT);
    private static final String VALID_REDIRECT_URI = "https://valid.example.com";

    @Mock private HttpClient mockHttpClient;
    @Mock private SignedJWT mockSignedJwt;
    @Mock private Context mockContext;
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
    @Captor ArgumentCaptor<Map<String, Object>> jsonArgumentCaptor;
    @Captor ArgumentCaptor<AuthorizationCode> authCoreArgumentCaptor;

    @BeforeAll
    public static void beforeAllSetUp() {
        StubSsmClient.setClientConfigParams(CLIENT_CONFIG);
    }

    @Nested
    class doAuthorizeTests {
        @BeforeEach
        void setup() {
            ENVIRONMENT_VARIABLES.set("MITIGATION_ENABLED", "False");
        }

        @Test
        void doAuthorizeShouldRenderMustacheTemplateWhenValidRequestReceived() throws Exception {
            setupQueryParams(validDoAuthorizeQueryParams());

            authorizeHandler.doAuthorize(mockContext);

            verify(mockContext).render(eq("templates/authorize.mustache"), anyMap());
            verify(mockHttpClient, times(0)).send(any(), any());
        }

        @Test
        void doAuthorizeShouldReturn400WhenRedirectUriParamNotRegistered() throws Exception {
            var queryParams = validDoAuthorizeQueryParams();
            queryParams.put(
                    REQUEST,
                    signedRequestJwt(
                                    defaultClaimSetBuilder(
                                                    VALID_RESPONSE_TYPE, INVALID_REDIRECT_URI)
                                            .build())
                            .serialize());
            setupQueryParams(queryParams);

            var e =
                    assertThrows(
                            BadRequestResponse.class,
                            () -> authorizeHandler.doAuthorize(mockContext));

            assertEquals(
                    "redirect_uri param provided does not match any of the redirect_uri values configured",
                    e.getMessage());
        }

        @Test
        void doAuthorizeShouldReturn302WithErrorQueryParamsWhenResponseTypeParamNotCode()
                throws Exception {
            var queryParams = validDoAuthorizeQueryParams();
            queryParams.put(
                    RequestParamConstants.REQUEST,
                    signedRequestJwt(
                                    defaultClaimSetBuilder(
                                                    INVALID_RESPONSE_TYPE, VALID_REDIRECT_URI)
                                            .build())
                            .serialize());
            setupQueryParams(queryParams);

            authorizeHandler.doAuthorize(mockContext);

            verify(mockContext, never()).render(eq("templates/authorize.mustache"), anyMap());
            verify(mockContext)
                    .redirect(
                            VALID_REDIRECT_URI
                                    + createExpectedErrorQueryStringParams(
                                            OAuth2Error.UNSUPPORTED_RESPONSE_TYPE));
        }

        @Test
        void doAuthorizeShouldReturn400WithErrorMessageWhenClientIdParamNotProvided()
                throws Exception {
            var queryParams = validDoAuthorizeQueryParams();
            queryParams.remove(CLIENT_ID);
            setupQueryParams(queryParams);

            var e =
                    assertThrows(
                            BadRequestResponse.class,
                            () -> authorizeHandler.doAuthorize(mockContext));

            assertEquals(
                    "Error: Could not find client configuration details for: null", e.getMessage());
        }

        @Test
        void doAuthorizeShouldReturn400WithErrorMessagesWhenClientIdParamNotRegistered()
                throws Exception {
            var queryParams = validDoAuthorizeQueryParams();
            queryParams.put(CLIENT_ID, "not-registered");
            setupQueryParams(queryParams);

            var e =
                    assertThrows(
                            BadRequestResponse.class,
                            () -> authorizeHandler.doAuthorize(mockContext));

            assertEquals(
                    "Error: Could not find client configuration details for: not-registered",
                    e.getMessage());
        }

        @Test
        void doAuthorizeShouldReturn400WithErrorMessagesWhenRequestParamMissing() throws Exception {
            var queryParams = validDoAuthorizeQueryParams();
            queryParams.remove(REQUEST);
            setupQueryParams(queryParams);

            var e =
                    assertThrows(
                            BadRequestResponse.class,
                            () -> authorizeHandler.doAuthorize(mockContext));

            assertEquals("request param must be provided", e.getMessage());
        }

        @Test
        void doAuthorizeShouldRenderMustacheTemplateWhenValidRequestReceivedWithRequestJWT()
                throws Exception {
            setupQueryParams(validDoAuthorizeQueryParams());

            authorizeHandler.doAuthorize(mockContext);

            verify(mockContext)
                    .render(eq("templates/authorize.mustache"), viewParamsCaptor.capture());

            assertTrue(
                    Boolean.parseBoolean(
                            viewParamsCaptor.getValue().get("isEvidenceType").toString()));

            assertEquals(
                    OBJECT_MAPPER.writeValueAsString(defaultSharedClaims()),
                    viewParamsCaptor.getValue().get("shared_claims"));
        }

        @Test
        void
                doAuthorizeShouldRenderMustacheTemplateWhenValidRequestReceivedWithEncryptedRequestJWT()
                        throws Exception {
            ENVIRONMENT_VARIABLES.set("CREDENTIAL_ISSUER_TYPE", "EVIDENCE");
            setupQueryParams(validEncryptedDoAuthorizeQueryParams());

            authorizeHandler.doAuthorize(mockContext);

            verify(mockContext)
                    .render(eq("templates/authorize.mustache"), viewParamsCaptor.capture());

            assertTrue(
                    Boolean.parseBoolean(
                            viewParamsCaptor.getValue().get("isEvidenceType").toString()));

            assertEquals(
                    OBJECT_MAPPER.writeValueAsString(defaultSharedClaims()),
                    viewParamsCaptor.getValue().get("shared_claims"));
            assertFalse((Boolean) viewParamsCaptor.getValue().get(CRI_MITIGATION_ENABLED_PARAM));
        }

        @Test
        void doAuthorizeShouldRenderMustacheTemplateWhenValidRequestReceivedWithMitigationEnabled()
                throws Exception {
            ENVIRONMENT_VARIABLES.set("MITIGATION_ENABLED", "True");
            ENVIRONMENT_VARIABLES.set("CREDENTIAL_ISSUER_TYPE", "EVIDENCE");
            setupQueryParams(validEncryptedDoAuthorizeQueryParams());

            authorizeHandler.doAuthorize(mockContext);

            verify(mockContext)
                    .render(eq("templates/authorize.mustache"), viewParamsCaptor.capture());

            assertTrue(
                    Boolean.parseBoolean(
                            viewParamsCaptor.getValue().get("isEvidenceType").toString()));

            assertTrue((Boolean) viewParamsCaptor.getValue().get(CRI_MITIGATION_ENABLED_PARAM));
        }

        @Test
        void
                doAuthorizeShouldRenderMustacheTemplateWhenValidRequestReceivedWhenSignatureVerificationFails()
                        throws Exception {
            ENVIRONMENT_VARIABLES.set("CREDENTIAL_ISSUER_TYPE", "EVIDENCE_DRIVING_LICENCE");

            var queryParams = validDoAuthorizeQueryParams();
            String signedJWT = signedRequestJwt(defaultClaimSetBuilder().build()).serialize();
            String invalidSignatureJwt = signedJWT.substring(0, signedJWT.length() - 4) + "Nope";
            queryParams.put(REQUEST, invalidSignatureJwt);
            setupQueryParams(queryParams);

            authorizeHandler.doAuthorize(mockContext);

            verify(mockContext)
                    .render(eq("templates/authorize.mustache"), viewParamsCaptor.capture());
            assertEquals(
                    "Error: Signature of the shared attribute JWT is not valid",
                    viewParamsCaptor.getValue().get("shared_claims"));
        }

        @Test
        void doAuthorizeShouldUseDefaultScopeValueWhenNoScopeInRequest() throws Exception {
            // Arrange
            setupQueryParams(validEncryptedDoAuthorizeQueryParams());

            // Act
            authorizeHandler.doAuthorize(mockContext);

            // Assert
            verify(mockContext)
                    .render(eq("templates/authorize.mustache"), viewParamsCaptor.capture());
            assertEquals(
                    "No scope provided in request",
                    viewParamsCaptor.getValue().get("scope").toString());
        }

        @Test
        void doAuthorizeShouldUseRequestScopeValueWhenScopeInRequest() throws Exception {
            // Arrange
            var claimsSet = defaultClaimSetBuilder().claim("scope", "test scope").build();
            setupQueryParams(validEncryptedDoAuthorizeQueryParams(claimsSet));

            // Act
            authorizeHandler.doAuthorize(mockContext);

            // Assert
            verify(mockContext)
                    .render(eq("templates/authorize.mustache"), viewParamsCaptor.capture());
            assertEquals("test scope", viewParamsCaptor.getValue().get("scope").toString());
        }

        @Test
        void doAuthorizeShouldUseDefaultContextValueWhenNoContextInRequest() throws Exception {
            // Arrange
            setupQueryParams(validEncryptedDoAuthorizeQueryParams());

            // Act
            authorizeHandler.doAuthorize(mockContext);

            // Assert
            verify(mockContext)
                    .render(eq("templates/authorize.mustache"), viewParamsCaptor.capture());
            assertEquals(
                    "No context provided in request",
                    viewParamsCaptor.getValue().get("context").toString());
        }

        @Test
        void doAuthorizeShouldUseRequestContextValueWhenContextInRequest() throws Exception {
            // Arrange
            var claimsSet = defaultClaimSetBuilder().claim("context", "test context").build();
            setupQueryParams(validEncryptedDoAuthorizeQueryParams(claimsSet));

            // Act
            authorizeHandler.doAuthorize(mockContext);

            // Assert
            verify(mockContext)
                    .render(eq("templates/authorize.mustache"), viewParamsCaptor.capture());
            assertEquals("test context", viewParamsCaptor.getValue().get("context").toString());
        }
    }

    @Nested
    class FormAuthorizeTests {
        @BeforeEach
        void setup() {
            ENVIRONMENT_VARIABLES.set("MITIGATION_ENABLED", "False");
        }

        @Test
        void formAuthorizeShouldReturn302WithAuthCodeQueryParamWhenValidAuthRequest()
                throws Exception {
            setupQueryParams(validDoAuthorizeQueryParams());
            setupFormParams(validGenerateResponseFormParams());

            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

            authorizeHandler.formAuthorize(mockContext);

            verify(mockAuthCodeService)
                    .persist(any(AuthorizationCode.class), anyString(), eq(VALID_REDIRECT_URI));
            verify(mockContext).redirect(notNull());
        }

        @Test
        void formAuthorizeShouldReturnErrorResponseWhenInvalidJsonPayloadProvided()
                throws Exception {
            setupQueryParams(validDoAuthorizeQueryParams());
            var formParams = validGenerateResponseFormParams();
            formParams.put(JSON_PAYLOAD, "invalid-json");
            setupFormParams(formParams);

            authorizeHandler.formAuthorize(mockContext);

            verify(mockContext)
                    .redirect(
                            VALID_REDIRECT_URI
                                    + "?error=invalid_json&iss=Credential+Issuer+Stub&error_description=Unable+to+generate+valid+JSON+Payload");
        }

        @Test
        void formAuthorizeShouldPersistSharedAttributesCombinedWithJsonInput_withMitigationEnabled()
                throws Exception {
            ENVIRONMENT_VARIABLES.set("MITIGATION_ENABLED", "True");

            setupQueryParams(validDoAuthorizeQueryParams());
            var formParams = validGenerateResponseFormParams();
            formParams.put(MITIGATED_CI, "XX");
            formParams.put(CIMIT_STUB_URL, "http://test.com");
            formParams.put(CIMIT_STUB_API_KEY, "api:key");
            setupFormParams(formParams);

            var httpResponse = mock(HttpResponse.class);
            when(httpResponse.statusCode()).thenReturn(200);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);
            when(mockSignedJwt.serialize()).thenReturn(DCMAW_VC);
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

            authorizeHandler.formAuthorize(mockContext);

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
            ENVIRONMENT_VARIABLES.set("MITIGATION_ENABLED", "True");

            setupQueryParams(validDoAuthorizeQueryParams());
            var formParams = validGenerateResponseFormParams();
            formParams.put(MITIGATED_CI, "XX");
            formParams.put(CIMIT_STUB_URL, "http://test.com");
            formParams.put(CIMIT_STUB_API_KEY, "api:key");
            setupFormParams(formParams);

            when(mockSignedJwt.serialize()).thenReturn(DCMAW_VC);
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

            var httpResponse = mock(HttpResponse.class);
            when(httpResponse.statusCode()).thenReturn(403);
            when(httpResponse.body()).thenReturn("Access denied");
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            authorizeHandler.formAuthorize(mockContext);

            verify(mockHttpClient, times(1)).send(any(), any());

            verify(mockContext)
                    .redirect(
                            VALID_REDIRECT_URI
                                    + "?error=failed_to_post&iss=Credential+Issuer+Stub&error_description=Failed+to+post+CI+mitigation+to+management+stub+api.");
        }

        @Test
        void formAuthorizeShouldRedirectWithRequestedOAuthErrorResponse() throws Exception {
            setupQueryParams(validDoAuthorizeQueryParams());
            var formParams = validGenerateResponseFormParams();
            formParams.put(REQUESTED_OAUTH_ERROR, "invalid_request");
            formParams.put(RequestParamConstants.REQUESTED_OAUTH_ERROR_ENDPOINT, "auth");
            formParams.put(
                    RequestParamConstants.REQUESTED_OAUTH_ERROR_DESCRIPTION,
                    "An error description");
            setupFormParams(formParams);

            authorizeHandler.formAuthorize(mockContext);

            verify(mockContext)
                    .redirect(
                            VALID_REDIRECT_URI
                                    + "?iss=Credential+Issuer+Stub&state=test-state&error=invalid_request&error_description=An+error+description");
        }

        @Test
        void formAuthorizeShouldNotIncludeSharedAttributesForUserAssertedCriType()
                throws Exception {
            ENVIRONMENT_VARIABLES.set("CREDENTIAL_ISSUER_TYPE", "USER_ASSERTED");

            setupQueryParams(validDoAuthorizeQueryParams());
            var formParams = validGenerateResponseFormParams();
            formParams.put("ci", "");
            setupFormParams(formParams);

            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

            authorizeHandler.formAuthorize(mockContext);

            ArgumentCaptor<Credential> persistedCredential =
                    ArgumentCaptor.forClass(Credential.class);

            verify(mockVcGenerator).generate(persistedCredential.capture());
            Map<String, Object> persistedAttributes =
                    persistedCredential.getValue().credentialSubject();
            assertNull(persistedAttributes.get("address"));
            assertNull(persistedAttributes.get("name"));
            assertNull(persistedAttributes.get("birthDate"));
            assertEquals("test-value", persistedAttributes.get("test"));

            verify(mockCredentialService).persist(eq(mockSignedJwt.serialize()), any(String.class));
        }
    }

    @Nested
    class ApiAuthorizeTests {
        @BeforeEach
        void setup() {
            ENVIRONMENT_VARIABLES.set("MITIGATION_ENABLED", "False");
        }

        @Test
        void apiAuthorizeShouldReturn302WithAuthCodeQueryParamWhenValidAuthRequest()
                throws Exception {
            when(mockContext.bodyAsClass(ApiAuthRequest.class))
                    .thenReturn(
                            new ApiAuthRequest(
                                    "clientIdValid",
                                    signedRequestJwt(defaultClaimSetBuilder().build()).serialize(),
                                    "{\"passport\":[{\"expiryDate\":\"2030-01-01\",\"icaoIssuerCode\":\"GBR\",\"documentNumber\":\"321654987\"}],\"name\":[{\"nameParts\":[{\"type\":\"GivenName\",\"value\":\"Kenneth\"},{\"type\":\"FamilyName\",\"value\":\"Decerqueira\"}]}],\"birthDate\":[{\"value\":\"1965-07-08\"}]}",
                                    "{\"activityHistoryScore\":1,\"checkDetails\":[{\"checkMethod\":\"vri\"},{\"biometricVerificationProcessLevel\":3,\"checkMethod\":\"bvr\"}],\"validityScore\":2,\"strengthScore\":3,\"type\":\"IdentityCheck\"}\"",
                                    null,
                                    null,
                                    null,
                                    null));
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

            authorizeHandler.apiAuthorize(mockContext);

            verify(mockAuthCodeService)
                    .persist(authCoreArgumentCaptor.capture(), anyString(), eq(VALID_REDIRECT_URI));
            verify(mockContext).status(200);
            verify(mockContext).json(jsonArgumentCaptor.capture());
            assertTrue(
                    jsonArgumentCaptor
                            .getValue()
                            .get("redirectUri")
                            .toString()
                            .contains(authCoreArgumentCaptor.getValue().getValue()));
        }

        @Test
        void apiAuthorizeShouldAllowNbfToBeSet() throws Exception {
            when(mockContext.bodyAsClass(ApiAuthRequest.class))
                    .thenReturn(
                            new ApiAuthRequest(
                                    "clientIdValid",
                                    signedRequestJwt(defaultClaimSetBuilder().build()).serialize(),
                                    "{\"passport\":[{\"expiryDate\":\"2030-01-01\",\"icaoIssuerCode\":\"GBR\",\"documentNumber\":\"321654987\"}],\"name\":[{\"nameParts\":[{\"type\":\"GivenName\",\"value\":\"Kenneth\"},{\"type\":\"FamilyName\",\"value\":\"Decerqueira\"}]}],\"birthDate\":[{\"value\":\"1965-07-08\"}]}",
                                    "{\"activityHistoryScore\":1,\"checkDetails\":[{\"checkMethod\":\"vri\"},{\"biometricVerificationProcessLevel\":3,\"checkMethod\":\"bvr\"}],\"validityScore\":2,\"strengthScore\":3,\"type\":\"IdentityCheck\"}\"",
                                    1714577018L,
                                    null,
                                    null,
                                    null));
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

            authorizeHandler.apiAuthorize(mockContext);

            verify(mockAuthCodeService)
                    .persist(authCoreArgumentCaptor.capture(), anyString(), eq(VALID_REDIRECT_URI));
            verify(mockContext).status(200);
            verify(mockContext).json(jsonArgumentCaptor.capture());
            assertTrue(
                    jsonArgumentCaptor
                            .getValue()
                            .get("redirectUri")
                            .toString()
                            .contains(authCoreArgumentCaptor.getValue().getValue()));
            verify(mockVcGenerator).generate(credentialArgumentCaptor.capture());
            assertEquals(1714577018L, credentialArgumentCaptor.getValue().nbf());
        }

        @Test
        void apiAuthorizeShouldAllowVcToBeSentToCriResponseQueue() throws Exception {
            when(mockContext.bodyAsClass(ApiAuthRequest.class))
                    .thenReturn(
                            new ApiAuthRequest(
                                    "clientIdValid",
                                    signedRequestJwt(defaultClaimSetBuilder().build()).serialize(),
                                    "{\"passport\":[{\"expiryDate\":\"2030-01-01\",\"icaoIssuerCode\":\"GBR\",\"documentNumber\":\"321654987\"}],\"name\":[{\"nameParts\":[{\"type\":\"GivenName\",\"value\":\"Kenneth\"},{\"type\":\"FamilyName\",\"value\":\"Decerqueira\"}]}],\"birthDate\":[{\"value\":\"1965-07-08\"}]}",
                                    "{\"activityHistoryScore\":1,\"checkDetails\":[{\"checkMethod\":\"vri\"},{\"biometricVerificationProcessLevel\":3,\"checkMethod\":\"bvr\"}],\"validityScore\":2,\"strengthScore\":3,\"type\":\"IdentityCheck\"}\"",
                                    null,
                                    null,
                                    new F2fDetails(
                                            true, false, "stubQueue_criResponseQueue_build", 0),
                                    null));
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);
            when(mockSignedJwt.serialize()).thenReturn(DCMAW_VC);
            var httpResponse = mock(HttpResponse.class);
            when(httpResponse.statusCode()).thenReturn(200);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            authorizeHandler.apiAuthorize(mockContext);

            verify(mockHttpClient).send(httpRequestArgumentCaptor.capture(), any());
            assertEquals(
                    "https://example.com/stub-queue",
                    httpRequestArgumentCaptor.getValue().uri().toString());
        }

        @Test
        void apiAuthorizeShouldAllowErrorToBeSentToCriResponseQueue() throws Exception {
            when(mockContext.bodyAsClass(ApiAuthRequest.class))
                    .thenReturn(
                            new ApiAuthRequest(
                                    "clientIdValid",
                                    signedRequestJwt(defaultClaimSetBuilder().build()).serialize(),
                                    "{\"passport\":[{\"expiryDate\":\"2030-01-01\",\"icaoIssuerCode\":\"GBR\",\"documentNumber\":\"321654987\"}],\"name\":[{\"nameParts\":[{\"type\":\"GivenName\",\"value\":\"Kenneth\"},{\"type\":\"FamilyName\",\"value\":\"Decerqueira\"}]}],\"birthDate\":[{\"value\":\"1965-07-08\"}]}",
                                    "{\"activityHistoryScore\":1,\"checkDetails\":[{\"checkMethod\":\"vri\"},{\"biometricVerificationProcessLevel\":3,\"checkMethod\":\"bvr\"}],\"validityScore\":2,\"strengthScore\":3,\"type\":\"IdentityCheck\"}\"",
                                    null,
                                    null,
                                    new F2fDetails(
                                            false, true, "stubQueue_criResponseQueue_build", 0),
                                    null));
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);
            when(mockSignedJwt.serialize()).thenReturn(DCMAW_VC);
            var httpResponse = mock(HttpResponse.class);
            when(httpResponse.statusCode()).thenReturn(200);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            authorizeHandler.apiAuthorize(mockContext);

            verify(mockHttpClient).send(httpRequestArgumentCaptor.capture(), any());
            assertEquals(
                    "https://example.com/stub-queue",
                    httpRequestArgumentCaptor.getValue().uri().toString());
        }

        @Test
        void apiAuthorizeShouldReturnErrorResponseWhenInvalidCredentialSubjectJsonProvided()
                throws Exception {
            when(mockContext.bodyAsClass(ApiAuthRequest.class))
                    .thenReturn(
                            new ApiAuthRequest(
                                    "clientIdValid",
                                    signedRequestJwt(defaultClaimSetBuilder().build()).serialize(),
                                    "not json",
                                    "{\"activityHistoryScore\":1,\"checkDetails\":[{\"checkMethod\":\"vri\"},{\"biometricVerificationProcessLevel\":3,\"checkMethod\":\"bvr\"}],\"validityScore\":2,\"strengthScore\":3,\"type\":\"IdentityCheck\"}\"",
                                    null,
                                    null,
                                    null,
                                    null));

            authorizeHandler.apiAuthorize(mockContext);

            verify(mockContext).status(200);
            verify(mockContext).json(jsonArgumentCaptor.capture());
            assertEquals(
                    VALID_REDIRECT_URI
                            + "?error=invalid_json&iss=Credential+Issuer+Stub&error_description=Unable+to+generate+valid+JSON+Payload",
                    jsonArgumentCaptor.getValue().get("redirectUri").toString());
        }

        @Test
        void apiAuthorizeShouldAllowMitigationsWhenEnabled() throws Exception {
            ENVIRONMENT_VARIABLES.set("MITIGATION_ENABLED", "True");

            when(mockContext.bodyAsClass(ApiAuthRequest.class))
                    .thenReturn(
                            new ApiAuthRequest(
                                    "clientIdValid",
                                    signedRequestJwt(defaultClaimSetBuilder().build()).serialize(),
                                    "{\"passport\":[{\"expiryDate\":\"2030-01-01\",\"icaoIssuerCode\":\"GBR\",\"documentNumber\":\"321654987\"}],\"name\":[{\"nameParts\":[{\"type\":\"GivenName\",\"value\":\"Kenneth\"},{\"type\":\"FamilyName\",\"value\":\"Decerqueira\"}]}],\"birthDate\":[{\"value\":\"1965-07-08\"}]}",
                                    "{\"activityHistoryScore\":1,\"checkDetails\":[{\"checkMethod\":\"vri\"},{\"biometricVerificationProcessLevel\":3,\"checkMethod\":\"bvr\"}],\"validityScore\":2,\"strengthScore\":3,\"type\":\"IdentityCheck\"}\"",
                                    null,
                                    new Mitigations(
                                            List.of("XX"),
                                            "https://cimit.stubs.account.gov.uk",
                                            "anAPIKey"),
                                    null,
                                    null));

            var httpResponse = mock(HttpResponse.class);
            when(httpResponse.statusCode()).thenReturn(200);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);
            when(mockSignedJwt.serialize()).thenReturn(DCMAW_VC);
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

            authorizeHandler.apiAuthorize(mockContext);

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
            ENVIRONMENT_VARIABLES.set("MITIGATION_ENABLED", "True");

            when(mockContext.bodyAsClass(ApiAuthRequest.class))
                    .thenReturn(
                            new ApiAuthRequest(
                                    "clientIdValid",
                                    signedRequestJwt(defaultClaimSetBuilder().build()).serialize(),
                                    "{\"passport\":[{\"expiryDate\":\"2030-01-01\",\"icaoIssuerCode\":\"GBR\",\"documentNumber\":\"321654987\"}],\"name\":[{\"nameParts\":[{\"type\":\"GivenName\",\"value\":\"Kenneth\"},{\"type\":\"FamilyName\",\"value\":\"Decerqueira\"}]}],\"birthDate\":[{\"value\":\"1965-07-08\"}]}",
                                    "{\"activityHistoryScore\":1,\"checkDetails\":[{\"checkMethod\":\"vri\"},{\"biometricVerificationProcessLevel\":3,\"checkMethod\":\"bvr\"}],\"validityScore\":2,\"strengthScore\":3,\"type\":\"IdentityCheck\"}\"",
                                    null,
                                    null,
                                    null,
                                    null));
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

            authorizeHandler.apiAuthorize(mockContext);

            verify(mockAuthCodeService)
                    .persist(authCoreArgumentCaptor.capture(), anyString(), eq(VALID_REDIRECT_URI));
            verify(mockContext).status(200);
            verify(mockContext).json(jsonArgumentCaptor.capture());
            assertTrue(
                    jsonArgumentCaptor
                            .getValue()
                            .get("redirectUri")
                            .toString()
                            .contains(authCoreArgumentCaptor.getValue().getValue()));
        }

        @Test
        void apiAuthorizeShouldRedirectWithRequestedOAuthErrorResponse() throws Exception {
            when(mockContext.bodyAsClass(ApiAuthRequest.class))
                    .thenReturn(
                            new ApiAuthRequest(
                                    "clientIdValid",
                                    signedRequestJwt(defaultClaimSetBuilder().build()).serialize(),
                                    "{\"passport\":[{\"expiryDate\":\"2030-01-01\",\"icaoIssuerCode\":\"GBR\",\"documentNumber\":\"321654987\"}],\"name\":[{\"nameParts\":[{\"type\":\"GivenName\",\"value\":\"Kenneth\"},{\"type\":\"FamilyName\",\"value\":\"Decerqueira\"}]}],\"birthDate\":[{\"value\":\"1965-07-08\"}]}",
                                    "{\"activityHistoryScore\":1,\"checkDetails\":[{\"checkMethod\":\"vri\"},{\"biometricVerificationProcessLevel\":3,\"checkMethod\":\"bvr\"}],\"validityScore\":2,\"strengthScore\":3,\"type\":\"IdentityCheck\"}\"",
                                    null,
                                    null,
                                    null,
                                    new RequestedError(
                                            "invalid_request",
                                            "a bad thing happened",
                                            "auth",
                                            null)));

            authorizeHandler.apiAuthorize(mockContext);

            verify(mockContext).status(200);
            verify(mockContext).json(jsonArgumentCaptor.capture());
            assertEquals(
                    VALID_REDIRECT_URI
                            + "?iss=Credential+Issuer+Stub&state=test-state&error=invalid_request&error_description=a+bad+thing+happened",
                    jsonArgumentCaptor.getValue().get("redirectUri").toString());
        }

        @Test
        void apiAuthorizeShouldAllowRequestedOAuthErrorResponseFromTokenEndpoint()
                throws Exception {
            when(mockContext.bodyAsClass(ApiAuthRequest.class))
                    .thenReturn(
                            new ApiAuthRequest(
                                    "clientIdValid",
                                    signedRequestJwt(defaultClaimSetBuilder().build()).serialize(),
                                    "{\"passport\":[{\"expiryDate\":\"2030-01-01\",\"icaoIssuerCode\":\"GBR\",\"documentNumber\":\"321654987\"}],\"name\":[{\"nameParts\":[{\"type\":\"GivenName\",\"value\":\"Kenneth\"},{\"type\":\"FamilyName\",\"value\":\"Decerqueira\"}]}],\"birthDate\":[{\"value\":\"1965-07-08\"}]}",
                                    "{\"activityHistoryScore\":1,\"checkDetails\":[{\"checkMethod\":\"vri\"},{\"biometricVerificationProcessLevel\":3,\"checkMethod\":\"bvr\"}],\"validityScore\":2,\"strengthScore\":3,\"type\":\"IdentityCheck\"}\"",
                                    null,
                                    null,
                                    null,
                                    new RequestedError(
                                            "invalid_request",
                                            "a bad thing happened at the token endpoint",
                                            "token",
                                            null)));
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

            authorizeHandler.apiAuthorize(mockContext);

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
            when(mockContext.bodyAsClass(ApiAuthRequest.class))
                    .thenReturn(
                            new ApiAuthRequest(
                                    "clientIdValid",
                                    signedRequestJwt(defaultClaimSetBuilder().build()).serialize(),
                                    "{\"passport\":[{\"expiryDate\":\"2030-01-01\",\"icaoIssuerCode\":\"GBR\",\"documentNumber\":\"321654987\"}],\"name\":[{\"nameParts\":[{\"type\":\"GivenName\",\"value\":\"Kenneth\"},{\"type\":\"FamilyName\",\"value\":\"Decerqueira\"}]}],\"birthDate\":[{\"value\":\"1965-07-08\"}]}",
                                    "{\"activityHistoryScore\":1,\"checkDetails\":[{\"checkMethod\":\"vri\"},{\"biometricVerificationProcessLevel\":3,\"checkMethod\":\"bvr\"}],\"validityScore\":2,\"strengthScore\":3,\"type\":\"IdentityCheck\"}\"",
                                    null,
                                    null,
                                    null,
                                    new RequestedError(null, null, null, "404")));
            when(mockVcGenerator.generate(any())).thenReturn(mockSignedJwt);

            authorizeHandler.apiAuthorize(mockContext);

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

    private ECPrivateKey getPrivateKey() throws InvalidKeySpecException, NoSuchAlgorithmException {
        return (ECPrivateKey)
                KeyFactory.getInstance("EC")
                        .generatePrivate(
                                new PKCS8EncodedKeySpec(
                                        Base64.getDecoder().decode(TestFixtures.EC_PRIVATE_KEY_1)));
    }

    private Map<String, String> validDoAuthorizeQueryParams() throws Exception {
        var params = new HashMap<String, String>();
        params.put(CLIENT_ID, "clientIdValid");
        params.put(REQUEST, signedRequestJwt(defaultClaimSetBuilder().build()).serialize());
        return params;
    }

    private Map<String, String> validEncryptedDoAuthorizeQueryParams() throws Exception {
        return validEncryptedDoAuthorizeQueryParams(defaultClaimSetBuilder().build());
    }

    private Map<String, String> validEncryptedDoAuthorizeQueryParams(JWTClaimsSet claimsSet)
            throws Exception {
        var params = new HashMap<String, String>();
        params.put(CLIENT_ID, "clientIdValid");
        params.put(REQUEST, encryptedRequestJwt(signedRequestJwt(claimsSet)).serialize());
        return params;
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

    private Map<String, String> validGenerateResponseFormParams() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put(JSON_PAYLOAD, "{\"test\": \"test-value\"}");
        queryParams.put(STRENGTH, "2");
        queryParams.put(VALIDITY, "3");
        queryParams.put(VC_NOT_BEFORE_FLAG, "on");
        queryParams.put(VC_NOT_BEFORE_DAY, "1");
        queryParams.put(VC_NOT_BEFORE_MONTH, "1");
        queryParams.put(VC_NOT_BEFORE_YEAR, "2022");
        queryParams.put(VC_NOT_BEFORE_HOURS, "0");
        queryParams.put(VC_NOT_BEFORE_MINUTES, "0");
        queryParams.put(VC_NOT_BEFORE_SECONDS, "0");
        return queryParams;
    }

    private void setupQueryParams(Map<String, String> queryParams) {
        queryParams.forEach((key, value) -> when(mockContext.queryParam(key)).thenReturn(value));
    }

    private void setupFormParams(Map<String, String> formParams) {
        formParams.forEach((key, value) -> when(mockContext.formParam(key)).thenReturn(value));
    }

    private JWTClaimsSet.Builder defaultClaimSetBuilder() {
        return defaultClaimSetBuilder(VALID_RESPONSE_TYPE, VALID_REDIRECT_URI);
    }

    private JWTClaimsSet.Builder defaultClaimSetBuilder(String responseType, String redirectUri) {
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
                .claim(SHARED_CLAIMS, defaultSharedClaims());
    }

    private Map<String, Object> defaultSharedClaims() {
        Map<String, Object> sharedClaims = new LinkedHashMap<>();
        sharedClaims.put("address", Collections.singletonList("123 random street, M13 7GE"));
        sharedClaims.put(
                "name",
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
