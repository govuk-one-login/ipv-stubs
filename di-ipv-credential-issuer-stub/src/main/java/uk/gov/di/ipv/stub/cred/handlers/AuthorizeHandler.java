package uk.gov.di.ipv.stub.cred.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.shaded.json.JSONObject;
import com.nimbusds.jose.shaded.json.parser.JSONParser;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ResponseMode;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.cred.config.ClientConfig;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.config.CriType;
import uk.gov.di.ipv.stub.cred.domain.*;
import uk.gov.di.ipv.stub.cred.error.CriStubException;
import uk.gov.di.ipv.stub.cred.service.AuthCodeService;
import uk.gov.di.ipv.stub.cred.service.ConfigService;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.service.RequestedErrorResponseService;
import uk.gov.di.ipv.stub.cred.utils.ES256SignatureVerifier;
import uk.gov.di.ipv.stub.cred.utils.ViewHelper;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;
import uk.gov.di.ipv.stub.cred.validation.Validator;
import uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialGenerator;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.nimbusds.jose.shaded.json.parser.JSONParser.MODE_JSON_SIMPLE;
import static uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig.F2F_STUB_QUEUE_NAME_DEFAULT;
import static uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig.F2F_STUB_QUEUE_URL;
import static uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig.getCriType;
import static uk.gov.di.ipv.stub.cred.config.CriType.DOC_CHECK_APP_CRI_TYPE;
import static uk.gov.di.ipv.stub.cred.config.CriType.USER_ASSERTED_CRI_TYPE;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.ACTIVITY_HISTORY;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.CI;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.F2F_STUB_QUEUE_NAME;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.FRAUD;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.STRENGTH;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VALIDITY;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VERIFICATION;

public class AuthorizeHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final String REQUEST_SCOPE = "scope";
    public static final String REQUEST_CONTEXT = "context";
    public static final String SHARED_CLAIMS = "shared_claims";
    public static final String EVIDENCE_REQUESTED = "evidence_requested";

    public static final String CRI_STUB_DATA = "cri_stub_data";
    public static final String CRI_STUB_EVIDENCE_PAYLOADS = "cri_stub_evidence_payloads";
    public static final String CRI_MITIGATION_ENABLED_PARAM = "isCriMitigationEnabled";

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizeHandler.class);

    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String ERROR_CODE_INVALID_REQUEST_JWT = "invalid_request_jwt";

    private static final String RESOURCE_ID_PARAM = "resourceId";
    private static final String IS_EVIDENCE_TYPE_PARAM = "isEvidenceType";
    private static final String IS_EVIDENCE_DRIVING_LICENCE_TYPE_PARAM =
            "isEvidenceDrivingLicenceType";
    private static final String IS_ACTIVITY_TYPE_PARAM = "isActivityType";
    private static final String IS_FRAUD_TYPE_PARAM = "isFraudType";
    private static final String IS_VERIFICATION_TYPE_PARAM = "isVerificationType";
    private static final String IS_DOC_CHECKING_TYPE_PARAM = "isDocCheckingType";
    private static final String IS_USER_ASSERTED_TYPE = "isUserAssertedType";
    private static final String IS_F2F_TYPE = "isF2FType";

    private static final String IS_NINO_TYPE = "isNINOType";
    private static final String HAS_ERROR_PARAM = "hasError";
    private static final String ERROR_PARAM = "error";
    private static final String CRI_NAME_PARAM = "cri-name";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final List<CriType> NO_SHARED_ATTRIBUTES_CRI_TYPES =
            List.of(USER_ASSERTED_CRI_TYPE, DOC_CHECK_APP_CRI_TYPE);

    private final AuthCodeService authCodeService;
    private final CredentialService credentialService;
    private final RequestedErrorResponseService requestedErrorResponseService;
    private final ES256SignatureVerifier es256SignatureVerifier = new ES256SignatureVerifier();
    private ViewHelper viewHelper;
    private VerifiableCredentialGenerator verifiableCredentialGenerator;
    private HttpClient httpClient;

    public AuthorizeHandler(
            ViewHelper viewHelper,
            AuthCodeService authCodeService,
            CredentialService credentialService,
            RequestedErrorResponseService requestedErrorResponseService,
            VerifiableCredentialGenerator verifiableCredentialGenerator,
            HttpClient httpClient) {
        Objects.requireNonNull(viewHelper);
        this.viewHelper = viewHelper;
        this.authCodeService = authCodeService;
        this.credentialService = credentialService;
        this.requestedErrorResponseService = requestedErrorResponseService;
        this.verifiableCredentialGenerator = verifiableCredentialGenerator;
        this.httpClient = httpClient;
    }

    public Route doAuthorize =
            (Request request, Response response) -> {
                QueryParamsMap queryParamsMap = request.queryMap();

                ValidationResult validationResult = validateQueryParams(queryParamsMap);

                if (!validationResult.isValid()) {
                    if (validationResult
                            .getError()
                            .getCode()
                            .equals(ERROR_CODE_INVALID_REQUEST_JWT)) {
                        response.status(HttpServletResponse.SC_BAD_REQUEST);
                        return validationResult.getError().getDescription();
                    }

                    String clientIdValue = queryParamsMap.value(RequestParamConstants.CLIENT_ID);
                    String requestValue = queryParamsMap.value(RequestParamConstants.REQUEST);

                    ClientConfig clientConfig = ConfigService.getClientConfig(clientIdValue);

                    if (clientConfig == null) {
                        response.status(HttpServletResponse.SC_BAD_REQUEST);
                        return "Error: Could not find client configuration details for: "
                                + clientIdValue;
                    }

                    SignedJWT signedJWT =
                            getSignedJWT(requestValue, clientConfig.getEncryptionPrivateKey());

                    AuthorizationErrorResponse errorResponse =
                            new AuthorizationErrorResponse(
                                    URI.create(
                                            signedJWT
                                                    .getJWTClaimsSet()
                                                    .getClaim(RequestParamConstants.REDIRECT_URI)
                                                    .toString()),
                                    validationResult.getError(),
                                    State.parse(
                                            signedJWT
                                                    .getJWTClaimsSet()
                                                    .getClaim(RequestParamConstants.STATE)
                                                    .toString()),
                                    ResponseMode.QUERY);

                    response.type(DEFAULT_RESPONSE_CONTENT_TYPE);
                    response.redirect(errorResponse.toURI().toString());
                    // No content required in response
                    return null;
                }

                Object criStubData = getCriStubData();
                Object criStubEvidencePayloads = getCriStubEvidencePayloads();

                String sharedAttributesJson;
                String evidenceRequestedJson;
                String requestScope;
                String requestContext;
                try {
                    JWTClaimsSet claimsSet = getJwtClaimsSet(queryParamsMap);
                    requestScope = claimsSet.getStringClaim(REQUEST_SCOPE);
                    requestScope =
                            requestScope == null ? "No scope provided in request" : requestScope;
                    requestContext = claimsSet.getStringClaim(REQUEST_CONTEXT);
                    requestContext =
                            requestContext == null
                                    ? "No context provided in request"
                                    : requestContext;
                    sharedAttributesJson = getSharedAttributes(claimsSet);
                    evidenceRequestedJson = getEvidenceRequested(claimsSet);
                } catch (Exception e) {
                    requestScope = e.getMessage();
                    requestContext = e.getMessage();
                    sharedAttributesJson = e.getMessage();
                    evidenceRequestedJson = e.getMessage();
                }

                CriType criType = getCriType();
                LOGGER.info("criType: {}", criType.value);

                Map<String, Object> frontendParams = new HashMap<>();
                frontendParams.put(RESOURCE_ID_PARAM, UUID.randomUUID().toString());
                frontendParams.put(
                        IS_EVIDENCE_TYPE_PARAM,
                        criType.equals(CriType.EVIDENCE_CRI_TYPE)
                                || criType.equals(CriType.EVIDENCE_DRIVING_LICENCE_CRI_TYPE));
                frontendParams.put(
                        IS_EVIDENCE_DRIVING_LICENCE_TYPE_PARAM,
                        criType.equals(CriType.EVIDENCE_DRIVING_LICENCE_CRI_TYPE));
                frontendParams.put(
                        IS_ACTIVITY_TYPE_PARAM, criType.equals(CriType.ACTIVITY_CRI_TYPE));
                frontendParams.put(IS_FRAUD_TYPE_PARAM, criType.equals(CriType.FRAUD_CRI_TYPE));
                frontendParams.put(
                        IS_VERIFICATION_TYPE_PARAM, criType.equals(CriType.VERIFICATION_CRI_TYPE));
                frontendParams.put(
                        IS_DOC_CHECKING_TYPE_PARAM, criType.equals(DOC_CHECK_APP_CRI_TYPE));
                frontendParams.put(IS_F2F_TYPE, criType.equals(CriType.F2F_CRI_TYPE));
                frontendParams.put(IS_NINO_TYPE, criType.equals(CriType.NINO_CRI_TYPE));
                frontendParams.put(
                        CRI_MITIGATION_ENABLED_PARAM,
                        CredentialIssuerConfig.isEnabled(
                                CredentialIssuerConfig.CRI_MITIGATION_ENABLED, "false"));
                frontendParams.put(IS_USER_ASSERTED_TYPE, criType.equals(USER_ASSERTED_CRI_TYPE));
                frontendParams.put(REQUEST_SCOPE, requestScope);
                frontendParams.put(REQUEST_CONTEXT, requestContext);
                if (!criType.equals(DOC_CHECK_APP_CRI_TYPE)) {
                    frontendParams.put(SHARED_CLAIMS, sharedAttributesJson);
                }
                frontendParams.put(EVIDENCE_REQUESTED, evidenceRequestedJson);
                frontendParams.put(CRI_STUB_DATA, criStubData);
                frontendParams.put(CRI_STUB_EVIDENCE_PAYLOADS, criStubEvidencePayloads);
                frontendParams.put(F2F_STUB_QUEUE_NAME, F2F_STUB_QUEUE_NAME_DEFAULT);

                String error = request.attribute(ERROR_PARAM);
                boolean hasError = error != null;
                frontendParams.put(HAS_ERROR_PARAM, hasError);
                if (hasError) {
                    frontendParams.put(ERROR_PARAM, error);
                }

                frontendParams.put(CRI_NAME_PARAM, CredentialIssuerConfig.NAME);

                return viewHelper.render(frontendParams, "authorize.mustache");
            };

    public Route apiAuthorize =
            (Request request, Response response) -> {
                String body = request.body();
                return null;
            };

    public Route generateResponse =
            (Request request, Response response) -> {
                var authRequest = AuthRequest.fromQueryMap(request.queryMap());

                AuthorizationErrorResponse requestedAuthErrorResponse =
                        handleRequestedError(authRequest);
                if (requestedAuthErrorResponse != null) {
                    response.redirect(requestedAuthErrorResponse.toURI().toString());
                    return null;
                }

                var clientIdValue = authRequest.getClientId();
                var jar = authRequest.getJar();

                ClientConfig clientConfig = ConfigService.getClientConfig(clientIdValue);

                if (clientConfig == null) {
                    response.status(HttpServletResponse.SC_BAD_REQUEST);
                    return "Error: Could not find client configuration details for: "
                            + clientIdValue;
                }

                SignedJWT signedJWT = getSignedJWT(jar, clientConfig.getEncryptionPrivateKey());
                String redirectUri =
                        signedJWT
                                .getJWTClaimsSet()
                                .getClaim(RequestParamConstants.REDIRECT_URI)
                                .toString();
                String userId = signedJWT.getJWTClaimsSet().getSubject();
                String state =
                        signedJWT
                                .getJWTClaimsSet()
                                .getClaim(RequestParamConstants.STATE)
                                .toString();

                try {
                    var attributesMap = generateJsonPayload(authRequest.getCredentialSubject());

                    Map<String, Object> credentialAttributesMap;
                    if (NO_SHARED_ATTRIBUTES_CRI_TYPES.contains(getCriType())) {
                        credentialAttributesMap = attributesMap;
                    } else {
                        Map<String, Object> combinedAttributeJson =
                                generateJsonPayload(
                                        getSharedAttributes(signedJWT.getJWTClaimsSet()));
                        combinedAttributeJson.putAll(attributesMap);
                        credentialAttributesMap = combinedAttributeJson;
                    }

                    Map<String, Object> gpgMap;
                    var evidenceJsonPayload = authRequest.getEvidencePayload();
                    if (evidenceJsonPayload == null || evidenceJsonPayload.isEmpty()) {
                        gpgMap =
                                generateGpg45Score(
                                        getCriType(),
                                        authRequest.getEvidenceScore(),
                                        authRequest.getValidityScore(),
                                        authRequest.getActivityScore(),
                                        authRequest.getFraudScore(),
                                        authRequest.getVerificationScore(),
                                        authRequest.getBiometricVerificationScore());
                    } else {
                        gpgMap = generateJsonPayload(evidenceJsonPayload);
                        if (gpgMap.get(CredentialIssuerConfig.EVIDENCE_TXN_PARAM) == null) {
                            gpgMap.put(
                                    CredentialIssuerConfig.EVIDENCE_TXN_PARAM,
                                    UUID.randomUUID().toString());
                        }
                    }

                    if (!authRequest.getCis().isEmpty()) {
                        gpgMap.put(CI, authRequest.getCis());
                    }

                    Long nbf =
                            authRequest.isNbfFlag()
                                    ? authRequest.getNbf()
                                    : Instant.now().getEpochSecond();

                    String signedVcJwt =
                            verifiableCredentialGenerator
                                    .generate(
                                            new Credential(
                                                    credentialAttributesMap,
                                                    gpgMap,
                                                    userId,
                                                    clientIdValue,
                                                    nbf))
                                    .serialize();

                    if (CredentialIssuerConfig.isEnabled(
                            CredentialIssuerConfig.CRI_MITIGATION_ENABLED, "false")) {
                        processMitigatedCIs(userId, authRequest, signedVcJwt);
                    }

                    if (authRequest.isSendF2fVcToQueue() && !authRequest.isSendF2fErrorToQueue()) {
                        HTTPRequest httpRequest =
                                new HTTPRequest(
                                        HTTPRequest.Method.POST, URI.create(F2F_STUB_QUEUE_URL));
                        ObjectMapper objectMapper = new ObjectMapper();
                        F2FEnqueueLambdaRequest enqueueLambdaRequest =
                                new F2FEnqueueLambdaRequest(
                                        authRequest.getF2fQueueName(),
                                        new F2FQueueEvent(userId, state, List.of(signedVcJwt)),
                                        10);
                        String body = objectMapper.writeValueAsString(enqueueLambdaRequest);
                        httpRequest.setQuery(body);
                        httpRequest.send();
                    }

                    if (authRequest.isSendF2fErrorToQueue()) {
                        HTTPRequest httpRequest =
                                new HTTPRequest(
                                        HTTPRequest.Method.POST, URI.create(F2F_STUB_QUEUE_URL));
                        F2FErrorEnqueueLambdaRequest enqueueLambdaRequest =
                                new F2FErrorEnqueueLambdaRequest(
                                        authRequest.getF2fQueueName(),
                                        new F2FQueueErrorEvent(
                                                userId,
                                                state,
                                                "access_denied",
                                                "Something went wrong"),
                                        10);
                        String body = OBJECT_MAPPER.writeValueAsString(enqueueLambdaRequest);
                        httpRequest.setQuery(body);
                        httpRequest.send();
                    }

                    AuthorizationSuccessResponse successResponse =
                            generateAuthCode(state, redirectUri);
                    persistData(
                            authRequest,
                            successResponse.getAuthorizationCode(),
                            signedVcJwt,
                            redirectUri);

                    response.type(DEFAULT_RESPONSE_CONTENT_TYPE);
                    response.redirect(successResponse.toURI().toString());
                } catch (CriStubException e) {
                    AuthorizationErrorResponse errorResponse =
                            generateErrorResponse(e, redirectUri);
                    response.redirect(errorResponse.toURI().toString());
                }
                return null;
            };

    private void processMitigatedCIs(String userId, AuthRequest authRequest, String signedVcJwt)
            throws CriStubException {
        var mitigatedCis = authRequest.getMitigatedCis();
        if (!mitigatedCis.isEmpty()) {
            LOGGER.info("Processing mitigated CI's");
            String cimitStubUrl = authRequest.getCimitStubUrl();
            String cimitStubApikey = authRequest.getCimitStubApikey();
            String postUrlTemplate = "/user/%s/mitigations/%s";
            for (String ciCode : mitigatedCis) {
                String jwtId;
                try {
                    jwtId = SignedJWT.parse(signedVcJwt).getJWTClaimsSet().getJWTID();
                } catch (ParseException e) {
                    throw new CriStubException("Unable to parse signed JWT", e);
                }

                var encodedUserId = URLEncoder.encode(userId, StandardCharsets.UTF_8);
                String postUrl =
                        cimitStubUrl + String.format(postUrlTemplate, encodedUserId, ciCode);
                LOGGER.info("Managed cimit stub postUrl:{}", postUrl);
                try {
                    HttpRequest request =
                            HttpRequest.newBuilder()
                                    .uri(new URI(postUrl))
                                    .header("Content-Type", "application/json")
                                    .header("x-api-key", cimitStubApikey)
                                    .POST(
                                            HttpRequest.BodyPublishers.ofString(
                                                    String.format(
                                                            "{\"mitigations\":[\"M01\"],\"vcJti\":\"%s\"}",
                                                            jwtId)))
                                    .build();

                    HttpResponse response =
                            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    int responseStatusCode = response.statusCode();
                    LOGGER.info("Processed mitigated CI's with response: {}", responseStatusCode);
                    if (responseStatusCode != 200) {
                        String msg = "Failed to post CI mitigation to management stub api.";
                        LOGGER.error(msg);
                        LOGGER.error(response.body().toString());
                        throw new CriStubException("failed_to_post", msg);
                    }
                } catch (URISyntaxException e) {
                    LOGGER.info(
                            "Unable to generate request for post cimit stub. Error message:{}",
                            e.getMessage());
                    throw new CriStubException(
                            "invalid_posturl", "Unable to generate request for post cimit stub", e);
                } catch (IOException | InterruptedException e) {
                    LOGGER.info(
                            "Unable to make post cimit management stub call. Error message:{}",
                            e.getMessage());
                    throw new CriStubException(
                            "failed_to_post", "Unable to make post cimit management stub call", e);
                }
            }
        }
    }

    private AuthorizationSuccessResponse generateAuthCode(String state, String redirectUri) {
        AuthorizationCode authorizationCode = new AuthorizationCode();

        return new AuthorizationSuccessResponse(
                URI.create(redirectUri),
                authorizationCode,
                null,
                State.parse(state),
                ResponseMode.QUERY);
    }

    private AuthorizationErrorResponse generateErrorResponse(
            CriStubException error, String redirectUri) {
        return new AuthorizationErrorResponse(
                URI.create(redirectUri),
                new ErrorObject(error.getMessage(), error.getDescription()),
                null,
                new Issuer(CredentialIssuerConfig.NAME),
                ResponseMode.QUERY);
    }

    private void persistData(
            AuthRequest authRequest,
            AuthorizationCode authorizationCode,
            String signedVcJwt,
            String redirectUri) {
        String resourceId = authRequest.getResourceId();
        this.authCodeService.persist(authorizationCode, resourceId, redirectUri);
        this.credentialService.persist(signedVcJwt, resourceId);
        this.requestedErrorResponseService.persist(authorizationCode.getValue(), authRequest);
    }

    private Map<String, Object> generateJsonPayload(String payload) throws CriStubException {
        if (StringUtils.isBlank(payload)) payload = "{}";
        try {
            return OBJECT_MAPPER.readValue(payload, Map.class);
        } catch (JsonProcessingException e) {
            throw new CriStubException("invalid_json", "Unable to generate valid JSON Payload", e);
        }
    }

    private Map<String, Object> generateGpg45Score(
            CriType criType,
            String strengthValue,
            String validityValue,
            String activityValue,
            String fraudValue,
            String verificationValue,
            String biometricVerificationValue)
            throws CriStubException {

        if (criType.equals(USER_ASSERTED_CRI_TYPE)) {
            // VCs from user asserted CRIs, like address, do not have GPG45 scores
            return null;
        }

        ValidationResult validationResult =
                Validator.verifyGpg45(
                        criType,
                        strengthValue,
                        validityValue,
                        activityValue,
                        fraudValue,
                        verificationValue);

        if (!validationResult.isValid()) {
            throw new CriStubException(
                    "invalid_request", validationResult.getError().getDescription());
        }

        Map<String, Object> gpg45Score = new HashMap<>();
        gpg45Score.put(
                CredentialIssuerConfig.EVIDENCE_TYPE_PARAM,
                CredentialIssuerConfig.EVIDENCE_TYPE_IDENTITY_CHECK);
        gpg45Score.put(CredentialIssuerConfig.EVIDENCE_TXN_PARAM, UUID.randomUUID().toString());
        switch (criType) {
            case EVIDENCE_CRI_TYPE -> {
                gpg45Score.put(STRENGTH, Integer.parseInt(strengthValue));
                gpg45Score.put(VALIDITY, Integer.parseInt(validityValue));
            }
            case ACTIVITY_CRI_TYPE -> gpg45Score.put(
                    ACTIVITY_HISTORY, Integer.parseInt(activityValue));
            case FRAUD_CRI_TYPE -> {
                gpg45Score.put(FRAUD, Integer.parseInt(fraudValue));
                if (StringUtils.isNotBlank(activityValue)) {
                    gpg45Score.put(ACTIVITY_HISTORY, Integer.parseInt(activityValue));
                }
            }
            case VERIFICATION_CRI_TYPE -> gpg45Score.put(
                    VERIFICATION, Integer.parseInt(verificationValue));
            case DOC_CHECK_APP_CRI_TYPE -> {
                int strengthNum =
                        StringUtils.isNotBlank(validityValue) ? Integer.parseInt(strengthValue) : 3;
                gpg45Score.put(STRENGTH, strengthNum);
                int validityNum =
                        StringUtils.isNotBlank(validityValue) ? Integer.parseInt(validityValue) : 2;
                gpg45Score.put(VALIDITY, validityNum);

                if (StringUtils.isNotBlank(activityValue)) {
                    gpg45Score.put(ACTIVITY_HISTORY, Integer.parseInt(activityValue));
                }

                List<Map<String, Object>> checkDetailsValue = new ArrayList<>();
                checkDetailsValue.add(Map.of("checkMethod", "vri"));
                int biometricVerificationNum =
                        StringUtils.isNotBlank(biometricVerificationValue)
                                ? Integer.parseInt(biometricVerificationValue)
                                : 2;
                checkDetailsValue.add(
                        Map.of(
                                "checkMethod",
                                "bvr",
                                "biometricVerificationProcessLevel",
                                biometricVerificationNum));

                if (validityNum < 2) {
                    gpg45Score.put(
                            CredentialIssuerConfig.FAILED_CHECK_DETAILS_PARAM, checkDetailsValue);
                } else {
                    gpg45Score.put(CredentialIssuerConfig.CHECK_DETAILS_PARAM, checkDetailsValue);
                }
            }
            case EVIDENCE_DRIVING_LICENCE_CRI_TYPE -> {
                int strengthNum =
                        StringUtils.isNotBlank(validityValue) ? Integer.parseInt(strengthValue) : 3;
                gpg45Score.put(STRENGTH, strengthNum);
                int validityNum =
                        StringUtils.isNotBlank(validityValue) ? Integer.parseInt(validityValue) : 2;
                gpg45Score.put(VALIDITY, validityNum);

                if (StringUtils.isNotBlank(activityValue)) {
                    gpg45Score.put(ACTIVITY_HISTORY, Integer.parseInt(activityValue));
                }

                List<Map<String, Object>> checkDetailsValue = new ArrayList<>();
                checkDetailsValue.add(
                        Map.of(
                                "identityCheckPolicy",
                                "published",
                                "activityFrom",
                                "1982-05-23",
                                "checkMethod",
                                "data"));

                if (validityNum < 2) {
                    gpg45Score.put(
                            CredentialIssuerConfig.FAILED_CHECK_DETAILS_PARAM, checkDetailsValue);
                } else {
                    gpg45Score.put(CredentialIssuerConfig.CHECK_DETAILS_PARAM, checkDetailsValue);
                }
            }
            case F2F_CRI_TYPE -> {
                int strengthNum =
                        StringUtils.isNotBlank(validityValue) ? Integer.parseInt(strengthValue) : 3;
                gpg45Score.put(STRENGTH, strengthNum);
                int validityNum =
                        StringUtils.isNotBlank(validityValue) ? Integer.parseInt(validityValue) : 2;
                gpg45Score.put(VALIDITY, validityNum);

                if (StringUtils.isNotBlank(verificationValue)) {
                    gpg45Score.put(VERIFICATION, Integer.parseInt(verificationValue));
                }

                List<Map<String, Object>> checkDetailsValue = new ArrayList<>();
                checkDetailsValue.add(
                        Map.of(
                                "identityCheckPolicy",
                                "published",
                                "activityFrom",
                                "1982-05-23",
                                "checkMethod",
                                "data"));

                if (validityNum < 2) {
                    gpg45Score.put(
                            CredentialIssuerConfig.FAILED_CHECK_DETAILS_PARAM, checkDetailsValue);
                } else {
                    gpg45Score.put(CredentialIssuerConfig.CHECK_DETAILS_PARAM, checkDetailsValue);
                }
            }
        }
        return gpg45Score;
    }

    private ValidationResult validateQueryParams(QueryParamsMap queryParams) {
        String clientIdValue = queryParams.value(RequestParamConstants.CLIENT_ID);
        if (Validator.isNullBlankOrEmpty(clientIdValue)
                || ConfigService.getClientConfig(clientIdValue) == null) {
            return new ValidationResult(false, OAuth2Error.INVALID_CLIENT);
        }

        String request = queryParams.value(RequestParamConstants.REQUEST);
        if (Validator.isNullBlankOrEmpty(request)) {
            return new ValidationResult(
                    false,
                    new ErrorObject(
                            ERROR_CODE_INVALID_REQUEST_JWT,
                            "request param must be provided",
                            HttpServletResponse.SC_BAD_REQUEST));
        }

        ValidationResult validationResult = validateRequestClaims(queryParams);
        if (validationResult != null) return validationResult;

        return ValidationResult.createValidResult();
    }

    private ValidationResult validateRequestClaims(QueryParamsMap queryParams) {
        String clientIdValue = queryParams.value(RequestParamConstants.CLIENT_ID);
        ClientConfig clientConfig = ConfigService.getClientConfig(clientIdValue);

        try {
            SignedJWT signedJWT =
                    getSignedJWT(
                            queryParams.value(RequestParamConstants.REQUEST),
                            clientConfig.getEncryptionPrivateKey());
            JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();

            if (Validator.isNullBlankOrEmpty(
                            jwtClaimsSet.getClaim(RequestParamConstants.RESPONSE_TYPE))
                    || !jwtClaimsSet
                            .getClaim(RequestParamConstants.RESPONSE_TYPE)
                            .toString()
                            .equals(ResponseType.Value.CODE.getValue())) {
                return new ValidationResult(false, OAuth2Error.UNSUPPORTED_RESPONSE_TYPE);
            }

            if (Validator.isNullBlankOrEmpty(
                    jwtClaimsSet.getClaim(RequestParamConstants.REDIRECT_URI))) {
                return new ValidationResult(
                        false,
                        new ErrorObject(
                                ERROR_CODE_INVALID_REQUEST_JWT,
                                "redirect_uri param must be provided",
                                HttpServletResponse.SC_BAD_REQUEST));
            }

            if (Validator.redirectUrlIsInvalid(
                    clientIdValue,
                    jwtClaimsSet.getClaim(RequestParamConstants.REDIRECT_URI).toString())) {
                return new ValidationResult(
                        false,
                        new ErrorObject(
                                ERROR_CODE_INVALID_REQUEST_JWT,
                                "redirect_uri param provided does not match any of the redirect_uri values configured",
                                HttpServletResponse.SC_BAD_REQUEST));
            }

            if (Validator.isNullBlankOrEmpty(jwtClaimsSet.getClaim(RequestParamConstants.ISSUER))) {
                return new ValidationResult(
                        false,
                        new ErrorObject(
                                "invalid_issuer",
                                "issuer param must be provided",
                                HttpServletResponse.SC_BAD_REQUEST));
            }

            if (Validator.isNullBlankOrEmpty(
                    jwtClaimsSet.getClaim(RequestParamConstants.AUDIENCE).toString())) {
                return new ValidationResult(
                        false,
                        new ErrorObject(
                                "invalid_issuer",
                                "issuer param must be provided",
                                HttpServletResponse.SC_BAD_REQUEST));
            }

        } catch (ParseException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            return new ValidationResult(
                    false,
                    new ErrorObject(
                            "unable_to_parse_request_jwt",
                            "unable to parse queryParams jwt into signed jwt object",
                            HttpServletResponse.SC_BAD_REQUEST));
        }
        return null;
    }

    private SignedJWT getSignedJWT(String request, PrivateKey encryptionPrivateKey)
            throws ParseException {
        try {
            JWEObject jweObject = getJweObject(request, encryptionPrivateKey);
            return jweObject.getPayload().toSignedJWT();
        } catch (ParseException | JOSEException e) {
            return SignedJWT.parse(request);
        }
    }

    private Object getCriStubData()
            throws UnsupportedEncodingException,
                    com.nimbusds.jose.shaded.json.parser.ParseException {
        JSONObject js =
                (JSONObject)
                        new JSONParser(MODE_JSON_SIMPLE)
                                .parse(
                                        AuthorizeHandler.class.getResourceAsStream(
                                                "/data/criStubData.json"));

        return js.get("data");
    }

    private Object getCriStubEvidencePayloads()
            throws UnsupportedEncodingException,
                    com.nimbusds.jose.shaded.json.parser.ParseException {
        JSONObject js =
                (JSONObject)
                        new JSONParser(MODE_JSON_SIMPLE)
                                .parse(
                                        AuthorizeHandler.class.getResourceAsStream(
                                                "/data/criStubEvidencePayloads.json"));

        return js.get("data");
    }

    private JWTClaimsSet getJwtClaimsSet(QueryParamsMap queryParamsMap) throws Exception {
        String requestParam = queryParamsMap.value(RequestParamConstants.REQUEST);
        String clientIdParam = queryParamsMap.value(RequestParamConstants.CLIENT_ID);

        ClientConfig clientConfig = ConfigService.getClientConfig(clientIdParam);

        if (clientConfig == null) {
            throw new Exception(
                    "Error: Could not find client configuration details for: " + clientIdParam);
        }

        if (!Validator.isNullBlankOrEmpty(requestParam)) {
            try {
                SignedJWT signedJWT =
                        getSignedJWT(requestParam, clientConfig.getEncryptionPrivateKey());
                String publicJwk =
                        getCriType().equals(DOC_CHECK_APP_CRI_TYPE)
                                ? clientConfig.getJwtAuthentication().getSigningPublicJwk()
                                : clientConfig.getSigningPublicJwk();
                if (!es256SignatureVerifier.valid(signedJWT, publicJwk)) {
                    LOGGER.error("JWT signature is invalid");
                    throw new Exception(
                            "Error: Signature of the shared attribute JWT is not valid");
                }

                JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
                if (claimsSet == null) {
                    throw new Exception("Claims set is null");
                }

                return claimsSet;
            } catch (ParseException e) {
                LOGGER.error("Failed to parse something: {}", e.getMessage());
                throw new Exception(
                        String.format("Error: failed to parse something: %s", e.getMessage()));
            } catch (JOSEException e) {
                LOGGER.error("Failed to verify the signature of the JWT", e);
                throw new Exception(
                        "Error: failed to verify the signature of the shared attribute JWT");
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                LOGGER.error("Failed to decrypt the JWT", e);
                throw new Exception("Error: Failed to decrypt the JWT");
            }
        } else {
            throw new Exception("Error: missing 'request' query parameter");
        }
    }

    private String getSharedAttributes(JWTClaimsSet claimsSet) {
        String sharedAttributesJson;
        try {
            Map<String, Object> sharedAttributes = claimsSet.getJSONObjectClaim(SHARED_CLAIMS);
            if (sharedAttributes == null) {
                LOGGER.error("shared_claims not found in JWT");
                return "shared_claims not found in JWT";
            }
            sharedAttributesJson = gson.toJson(sharedAttributes);
            return sharedAttributesJson;
        } catch (ParseException e) {
            LOGGER.error("Failed to parse something: {}", e.getMessage());
            return String.format("Error: failed to parse something: %s", e.getMessage());
        }
    }

    private String getEvidenceRequested(JWTClaimsSet claimsSet) {
        String evidenceRequestedJson;
        try {
            Map<String, Object> evidenceRequested =
                    claimsSet.getJSONObjectClaim(EVIDENCE_REQUESTED);
            if (evidenceRequested == null) {
                LOGGER.error("evidence_requested not found in JWT");
                return "evidence_requested not found in JWT";
            }
            evidenceRequestedJson = gson.toJson(evidenceRequested);
            return evidenceRequestedJson;
        } catch (ParseException e) {
            LOGGER.error("Failed to parse something: {}", e.getMessage());
            return String.format("Error: failed to parse something: %s", e.getMessage());
        }
    }

    private JWEObject getJweObject(String requestParam, PrivateKey encryptionPrivateKey)
            throws ParseException, JOSEException {
        JWEObject encryptedJweObject = JWEObject.parse(requestParam);
        RSADecrypter rsaDecrypter = new RSADecrypter(encryptionPrivateKey);
        encryptedJweObject.decrypt(rsaDecrypter);
        return encryptedJweObject;
    }

    private AuthorizationErrorResponse handleRequestedError(AuthRequest authRequest)
            throws NoSuchAlgorithmException, InvalidKeySpecException, ParseException {
        return requestedErrorResponseService.getRequestedAuthErrorResponse(authRequest);
    }
}
