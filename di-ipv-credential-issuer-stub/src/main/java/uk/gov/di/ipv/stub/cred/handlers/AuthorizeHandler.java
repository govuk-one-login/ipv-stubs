package uk.gov.di.ipv.stub.cred.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ResponseMode;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.util.MapUtils;
import com.nimbusds.oauth2.sdk.util.StringUtils;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;
import uk.gov.di.ipv.stub.cred.validation.Validator;
import uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialGenerator;

import java.io.IOException;
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
import java.util.UUID;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.nimbusds.oauth2.sdk.util.CollectionUtils.isEmpty;
import static com.nimbusds.oauth2.sdk.util.StringUtils.isBlank;
import static java.util.Objects.requireNonNullElse;
import static uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig.EVIDENCE_TXN_PARAM;
import static uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig.F2F_STUB_QUEUE_NAME_DEFAULT;
import static uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig.getConfigValue;
import static uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig.getCriType;
import static uk.gov.di.ipv.stub.cred.config.CriType.DOC_CHECK_APP_CRI_TYPE;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.ACTIVITY_HISTORY;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.CI;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.F2F_STUB_QUEUE_NAME;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.FRAUD;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.STRENGTH;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VALIDITY;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VERIFICATION;

public class AuthorizeHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizeHandler.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(INDENT_OUTPUT);

    public static final String REQUEST_SCOPE = "scope";
    public static final String REQUEST_CONTEXT = "context";
    public static final String SHARED_CLAIMS = "shared_claims";
    public static final String EVIDENCE_REQUESTED = "evidence_requested";

    public static final String CRI_STUB_DATA = "cri_stub_data";
    public static final String CRI_STUB_EVIDENCE_PAYLOADS = "cri_stub_evidence_payloads";
    public static final String CRI_MITIGATION_ENABLED_PARAM = "isCriMitigationEnabled";

    private static final String ERROR_CODE_INVALID_REQUEST_JWT = "invalid_request_jwt";

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
    private static final String APPLICATION_JSON = "application/json";
    private static final String F2F_STUB_QUEUE_URL = "F2F_STUB_QUEUE_URL";
    private static final String F2F_STUB_QUEUE_API_KEY =
            "F2F_STUB_QUEUE_API_KEY"; // pragma: allowlist secret
    private static final int F2F_DEFAULT_DELAY_SECONDS = 10;
    private static final String X_API_KEY = "x-api-key";

    private final AuthCodeService authCodeService;
    private final CredentialService credentialService;
    private final RequestedErrorResponseService requestedErrorResponseService;
    private final ES256SignatureVerifier es256SignatureVerifier = new ES256SignatureVerifier();
    private final VerifiableCredentialGenerator verifiableCredentialGenerator;
    private final HttpClient httpClient;

    public AuthorizeHandler(
            AuthCodeService authCodeService,
            CredentialService credentialService,
            RequestedErrorResponseService requestedErrorResponseService,
            VerifiableCredentialGenerator verifiableCredentialGenerator,
            HttpClient httpClient) {
        this.authCodeService = authCodeService;
        this.credentialService = credentialService;
        this.requestedErrorResponseService = requestedErrorResponseService;
        this.verifiableCredentialGenerator = verifiableCredentialGenerator;
        this.httpClient = httpClient;
    }

    public void doAuthorize(Context ctx) throws Exception {
        ValidationResult validationResult = validateQueryParams(ctx);

        if (!validationResult.isValid()) {
            if (validationResult.getError().getCode().equals(ERROR_CODE_INVALID_REQUEST_JWT)) {
                throw new BadRequestResponse(validationResult.getError().getDescription());
            }

            String clientIdValue = ctx.queryParam(RequestParamConstants.CLIENT_ID);
            String requestValue = ctx.queryParam(RequestParamConstants.REQUEST);

            ClientConfig clientConfig = ConfigService.getClientConfig(clientIdValue);

            if (clientConfig == null) {
                throw new BadRequestResponse(
                        "Error: Could not find client configuration details for: " + clientIdValue);
            }

            SignedJWT signedJWT =
                    getSignedJWT(
                            requestValue,
                            CredentialIssuerConfig.getPrivateEncryptionKey().toPrivateKey());

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

            ctx.redirect(errorResponse.toURI().toString());
            return;
        }

        var criStubData = getCriStubData();
        var criStubEvidencePayloads = getCriStubEvidencePayloads();

        String sharedAttributesJson;
        String evidenceRequestedJson;
        String requestScope;
        String requestContext;
        try {
            JWTClaimsSet claimsSet = getJwtClaimsSet(ctx);
            requestScope = claimsSet.getStringClaim(REQUEST_SCOPE);
            requestScope = requestScope == null ? "No scope provided in request" : requestScope;
            requestContext = claimsSet.getStringClaim(REQUEST_CONTEXT);
            requestContext =
                    requestContext == null ? "No context provided in request" : requestContext;
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
        frontendParams.put(
                IS_EVIDENCE_TYPE_PARAM,
                criType.equals(CriType.EVIDENCE_CRI_TYPE)
                        || criType.equals(CriType.EVIDENCE_DRIVING_LICENCE_CRI_TYPE));
        frontendParams.put(
                IS_EVIDENCE_DRIVING_LICENCE_TYPE_PARAM,
                criType.equals(CriType.EVIDENCE_DRIVING_LICENCE_CRI_TYPE));
        frontendParams.put(IS_ACTIVITY_TYPE_PARAM, criType.equals(CriType.ACTIVITY_CRI_TYPE));
        frontendParams.put(IS_FRAUD_TYPE_PARAM, criType.equals(CriType.FRAUD_CRI_TYPE));
        frontendParams.put(
                IS_VERIFICATION_TYPE_PARAM, criType.equals(CriType.VERIFICATION_CRI_TYPE));
        frontendParams.put(IS_DOC_CHECKING_TYPE_PARAM, criType.equals(DOC_CHECK_APP_CRI_TYPE));
        frontendParams.put(IS_F2F_TYPE, criType.equals(CriType.F2F_CRI_TYPE));
        frontendParams.put(IS_NINO_TYPE, criType.equals(CriType.NINO_CRI_TYPE));
        frontendParams.put(
                CRI_MITIGATION_ENABLED_PARAM,
                CredentialIssuerConfig.isEnabled(
                        CredentialIssuerConfig.CRI_MITIGATION_ENABLED, "false"));
        frontendParams.put(IS_USER_ASSERTED_TYPE, !criType.isIdentityCheck());
        frontendParams.put(REQUEST_SCOPE, requestScope);
        frontendParams.put(REQUEST_CONTEXT, requestContext);
        if (!criType.equals(DOC_CHECK_APP_CRI_TYPE)) {
            frontendParams.put(SHARED_CLAIMS, sharedAttributesJson);
        }
        frontendParams.put(EVIDENCE_REQUESTED, evidenceRequestedJson);
        frontendParams.put(CRI_STUB_DATA, criStubData);
        frontendParams.put(CRI_STUB_EVIDENCE_PAYLOADS, criStubEvidencePayloads);
        frontendParams.put(F2F_STUB_QUEUE_NAME, F2F_STUB_QUEUE_NAME_DEFAULT);

        String error = ctx.attribute(ERROR_PARAM);
        boolean hasError = error != null;
        frontendParams.put(HAS_ERROR_PARAM, hasError);
        if (hasError) {
            frontendParams.put(ERROR_PARAM, error);
        }

        frontendParams.put(CRI_NAME_PARAM, CredentialIssuerConfig.NAME);

        ctx.render("templates/authorize.mustache", frontendParams);
    }

    public void apiAuthorize(Context ctx) throws Exception {
        ApiAuthRequest apiAuthRequest = ctx.bodyAsClass(ApiAuthRequest.class);
        JWTClaimsSet claimsSet = getClaimsSet(apiAuthRequest);
        String redirectUri = generateResponseRedirect(apiAuthRequest, claimsSet);

        ctx.status(200);
        ctx.json(Map.of("redirectUri", redirectUri, "jarPayload", claimsSet.toJSONObject()));
    }

    public void formAuthorize(Context ctx) throws Exception {
        FormAuthRequest formAuthRequest = FormAuthRequest.fromFormContext(ctx);
        JWTClaimsSet claimsSet = getClaimsSet(formAuthRequest);
        ctx.redirect(generateResponseRedirect(formAuthRequest, claimsSet));
    }

    private String generateResponseRedirect(AuthRequest authRequest, JWTClaimsSet claimsSet)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, JOSEException,
                    ParseException {
        AuthorizationErrorResponse requestedAuthErrorResponse = handleRequestedError(authRequest);
        if (requestedAuthErrorResponse != null) {
            return requestedAuthErrorResponse.toURI().toString();
        }

        String clientIdValue = authRequest.clientId();
        String redirectUri = claimsSet.getClaim(RequestParamConstants.REDIRECT_URI).toString();
        String userId = claimsSet.getSubject();
        String state = claimsSet.getClaim(RequestParamConstants.STATE).toString();

        try {
            var credentialAttributesMap = jsonStringToMap(authRequest.credentialSubjectJson());

            Long nbf =
                    authRequest.nbf() != null ? authRequest.nbf() : Instant.now().getEpochSecond();

            String signedVcJwt =
                    verifiableCredentialGenerator
                            .generate(
                                    new Credential(
                                            credentialAttributesMap,
                                            generateEvidenceMap(authRequest),
                                            userId,
                                            clientIdValue,
                                            nbf))
                            .serialize();

            if (CredentialIssuerConfig.isEnabled(
                    CredentialIssuerConfig.CRI_MITIGATION_ENABLED, "false")) {
                processMitigatedCIs(userId, authRequest, signedVcJwt);
            }

            handleF2fRequests(authRequest.f2f(), userId, state, signedVcJwt);

            AuthorizationSuccessResponse successResponse = generateAuthCode(state, redirectUri);
            persistData(
                    authRequest, successResponse.getAuthorizationCode(), signedVcJwt, redirectUri);

            return successResponse.toURI().toString();
        } catch (CriStubException e) {
            AuthorizationErrorResponse errorResponse = generateErrorResponse(e, redirectUri);
            return errorResponse.toURI().toString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            AuthorizationErrorResponse errorResponse =
                    generateErrorResponse(
                            new CriStubException(
                                    "f2f_queue_exception", "Failed to send VC to F2F queue", e),
                            redirectUri);
            return errorResponse.toURI().toString();
        }
    }

    private JWTClaimsSet getClaimsSet(AuthRequest authRequest)
            throws ParseException, JOSEException {
        var clientIdValue = authRequest.clientId();
        var jar = authRequest.request();

        ClientConfig clientConfig = ConfigService.getClientConfig(clientIdValue);

        if (clientConfig == null) {
            throw new BadRequestResponse(
                    "Error: Could not find client configuration details for: " + clientIdValue);
        }

        SignedJWT signedJWT =
                getSignedJWT(jar, CredentialIssuerConfig.getPrivateEncryptionKey().toPrivateKey());

        return signedJWT.getJWTClaimsSet();
    }

    private Map<String, Object> generateEvidenceMap(AuthRequest authRequest)
            throws CriStubException {
        Map<String, Object> evidenceMap = jsonStringToMap(authRequest.evidenceJson());

        if (authRequest instanceof FormAuthRequest formAuthRequest) {
            if (MapUtils.isEmpty(evidenceMap)) {
                evidenceMap.putAll(generateGpg45Score(getCriType(), formAuthRequest.gpg45Scores()));
            }

            if (!isEmpty(formAuthRequest.ci())) {
                evidenceMap.put(CI, formAuthRequest.ci());
            }
        }
        if (evidenceMap.get(EVIDENCE_TXN_PARAM) == null) {
            evidenceMap.put(EVIDENCE_TXN_PARAM, UUID.randomUUID().toString());
        }

        return evidenceMap;
    }

    private void processMitigatedCIs(String userId, AuthRequest authRequest, String signedVcJwt)
            throws CriStubException {
        var mitigations = authRequest.mitigations();
        if (mitigations == null) {
            return;
        }
        if (!isEmpty(mitigations.mitigatedCi())) {
            LOGGER.info("Processing mitigated CI's");
            String cimitStubUrl = mitigations.cimitStubUrl();
            String cimitStubApikey = mitigations.cimitStubApiKey();
            String postUrlTemplate = "/user/%s/mitigations/%s";
            for (String ciCode : mitigations.mitigatedCi()) {
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
                                    .header("Content-Type", APPLICATION_JSON)
                                    .header("x-api-key", cimitStubApikey)
                                    .POST(
                                            HttpRequest.BodyPublishers.ofString(
                                                    String.format(
                                                            "{\"mitigations\":[\"M01\"],\"vcJti\":\"%s\"}",
                                                            jwtId)))
                                    .build();

                    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    int responseStatusCode = response.statusCode();
                    LOGGER.info("Processed mitigated CI's with response: {}", responseStatusCode);
                    if (responseStatusCode != 200) {
                        String msg = "Failed to post CI mitigation to management stub api.";
                        LOGGER.error(msg);
                        LOGGER.error(response.body());
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
        String resourceId = UUID.randomUUID().toString();
        this.authCodeService.persist(authorizationCode, resourceId, redirectUri);
        this.credentialService.persist(signedVcJwt, resourceId);
        this.requestedErrorResponseService.persist(
                authorizationCode.getValue(), authRequest.requestedError());
    }

    private Map<String, Object> jsonStringToMap(String payload) throws CriStubException {
        if (isBlank(payload)) payload = "{}";
        try {
            return OBJECT_MAPPER.readValue(payload, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new CriStubException("invalid_json", "Unable to generate valid JSON Payload", e);
        }
    }

    private Map<String, Object> generateGpg45Score(CriType criType, Gpg45Scores gpg45Scores)
            throws CriStubException {

        if (!criType.isIdentityCheck()) {
            // VCs from user asserted CRIs, like address, do not have GPG45 scores
            return Map.of();
        }

        ValidationResult validationResult =
                Validator.verifyGpg45(
                        criType,
                        gpg45Scores.strength(),
                        gpg45Scores.validity(),
                        gpg45Scores.activityHistory(),
                        gpg45Scores.fraud(),
                        gpg45Scores.verification());

        if (!validationResult.isValid()) {
            throw new CriStubException(
                    "invalid_request", validationResult.getError().getDescription());
        }

        Map<String, Object> gpg45Score = new HashMap<>();
        gpg45Score.put(
                CredentialIssuerConfig.EVIDENCE_TYPE_PARAM,
                CredentialIssuerConfig.EVIDENCE_TYPE_IDENTITY_CHECK);
        gpg45Score.put(EVIDENCE_TXN_PARAM, UUID.randomUUID().toString());
        switch (criType) {
            case EVIDENCE_CRI_TYPE -> {
                gpg45Score.put(STRENGTH, Integer.parseInt(gpg45Scores.strength()));
                gpg45Score.put(VALIDITY, Integer.parseInt(gpg45Scores.validity()));
            }
            case ACTIVITY_CRI_TYPE -> gpg45Score.put(
                    ACTIVITY_HISTORY, Integer.parseInt(gpg45Scores.activityHistory()));
            case FRAUD_CRI_TYPE -> {
                gpg45Score.put(FRAUD, Integer.parseInt(gpg45Scores.fraud()));
                if (StringUtils.isNotBlank(gpg45Scores.activityHistory())) {
                    gpg45Score.put(
                            ACTIVITY_HISTORY, Integer.parseInt(gpg45Scores.activityHistory()));
                }
            }
            case VERIFICATION_CRI_TYPE -> gpg45Score.put(
                    VERIFICATION, Integer.parseInt(gpg45Scores.verification()));
            case DOC_CHECK_APP_CRI_TYPE -> {
                int strengthNum =
                        StringUtils.isNotBlank(gpg45Scores.validity())
                                ? Integer.parseInt(gpg45Scores.strength())
                                : 3;
                gpg45Score.put(STRENGTH, strengthNum);
                int validityNum =
                        StringUtils.isNotBlank(gpg45Scores.validity())
                                ? Integer.parseInt(gpg45Scores.validity())
                                : 2;
                gpg45Score.put(VALIDITY, validityNum);

                if (StringUtils.isNotBlank(gpg45Scores.activityHistory())) {
                    gpg45Score.put(
                            ACTIVITY_HISTORY, Integer.parseInt(gpg45Scores.activityHistory()));
                }

                List<Map<String, Object>> checkDetailsValue = new ArrayList<>();
                checkDetailsValue.add(Map.of("checkMethod", "vri"));
                int biometricVerificationNum =
                        StringUtils.isNotBlank(gpg45Scores.biometricVerification())
                                ? Integer.parseInt(gpg45Scores.biometricVerification())
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
                        StringUtils.isNotBlank(gpg45Scores.validity())
                                ? Integer.parseInt(gpg45Scores.strength())
                                : 3;
                gpg45Score.put(STRENGTH, strengthNum);
                int validityNum =
                        StringUtils.isNotBlank(gpg45Scores.validity())
                                ? Integer.parseInt(gpg45Scores.validity())
                                : 2;
                gpg45Score.put(VALIDITY, validityNum);

                if (StringUtils.isNotBlank(gpg45Scores.activityHistory())) {
                    gpg45Score.put(
                            ACTIVITY_HISTORY, Integer.parseInt(gpg45Scores.activityHistory()));
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
                        StringUtils.isNotBlank(gpg45Scores.validity())
                                ? Integer.parseInt(gpg45Scores.strength())
                                : 3;
                gpg45Score.put(STRENGTH, strengthNum);
                int validityNum =
                        StringUtils.isNotBlank(gpg45Scores.validity())
                                ? Integer.parseInt(gpg45Scores.validity())
                                : 2;
                gpg45Score.put(VALIDITY, validityNum);

                if (StringUtils.isNotBlank(gpg45Scores.verification())) {
                    gpg45Score.put(VERIFICATION, Integer.parseInt(gpg45Scores.verification()));
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

    private ValidationResult validateQueryParams(Context ctx) {
        String clientIdValue = ctx.queryParam(RequestParamConstants.CLIENT_ID);
        if (Validator.isNullBlankOrEmpty(clientIdValue)
                || ConfigService.getClientConfig(clientIdValue) == null) {
            return new ValidationResult(false, OAuth2Error.INVALID_CLIENT);
        }

        String request = ctx.queryParam(RequestParamConstants.REQUEST);
        if (Validator.isNullBlankOrEmpty(request)) {
            return new ValidationResult(
                    false,
                    new ErrorObject(
                            ERROR_CODE_INVALID_REQUEST_JWT,
                            "request param must be provided",
                            HttpStatus.BAD_REQUEST.getCode()));
        }

        ValidationResult validationResult = validateRequestClaims(ctx);
        if (validationResult != null) return validationResult;

        return ValidationResult.createValidResult();
    }

    private ValidationResult validateRequestClaims(Context ctx) {
        String clientIdValue = ctx.queryParam(RequestParamConstants.CLIENT_ID);
        ClientConfig clientConfig = ConfigService.getClientConfig(clientIdValue);

        try {
            SignedJWT signedJWT =
                    getSignedJWT(
                            ctx.queryParam(RequestParamConstants.REQUEST),
                            CredentialIssuerConfig.getPrivateEncryptionKey().toPrivateKey());
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
                                HttpStatus.BAD_REQUEST.getCode()));
            }

            if (Validator.redirectUrlIsInvalid(
                    clientIdValue,
                    jwtClaimsSet.getClaim(RequestParamConstants.REDIRECT_URI).toString())) {
                return new ValidationResult(
                        false,
                        new ErrorObject(
                                ERROR_CODE_INVALID_REQUEST_JWT,
                                "redirect_uri param provided does not match any of the redirect_uri values configured",
                                HttpStatus.BAD_REQUEST.getCode()));
            }

            if (Validator.isNullBlankOrEmpty(jwtClaimsSet.getClaim(RequestParamConstants.ISSUER))) {
                return new ValidationResult(
                        false,
                        new ErrorObject(
                                "invalid_issuer",
                                "issuer param must be provided",
                                HttpStatus.BAD_REQUEST.getCode()));
            }

            if (Validator.isNullBlankOrEmpty(
                    jwtClaimsSet.getClaim(RequestParamConstants.AUDIENCE).toString())) {
                return new ValidationResult(
                        false,
                        new ErrorObject(
                                "invalid_issuer",
                                "issuer param must be provided",
                                HttpStatus.BAD_REQUEST.getCode()));
            }

        } catch (ParseException | JOSEException e) {
            return new ValidationResult(
                    false,
                    new ErrorObject(
                            "unable_to_parse_request_jwt",
                            "unable to parse queryParams jwt into signed jwt object",
                            HttpStatus.BAD_REQUEST.getCode()));
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

    private String getCriStubData() throws IOException {
        return OBJECT_MAPPER
                .readTree(AuthorizeHandler.class.getResourceAsStream("/data/criStubData.json"))
                .get("data")
                .toString();
    }

    private String getCriStubEvidencePayloads() throws IOException {
        return OBJECT_MAPPER
                .readTree(
                        AuthorizeHandler.class.getResourceAsStream(
                                "/data/criStubEvidencePayloads.json"))
                .get("data")
                .toString();
    }

    private JWTClaimsSet getJwtClaimsSet(Context ctx) throws Exception {
        String requestParam = ctx.queryParam(RequestParamConstants.REQUEST);
        String clientIdParam = ctx.queryParam(RequestParamConstants.CLIENT_ID);

        ClientConfig clientConfig = ConfigService.getClientConfig(clientIdParam);

        if (clientConfig == null) {
            throw new Exception(
                    "Error: Could not find client configuration details for: " + clientIdParam);
        }

        if (!Validator.isNullBlankOrEmpty(requestParam)) {
            try {
                SignedJWT signedJWT =
                        getSignedJWT(
                                requestParam,
                                CredentialIssuerConfig.getPrivateEncryptionKey().toPrivateKey());
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
        try {
            Map<String, Object> sharedAttributes = claimsSet.getJSONObjectClaim(SHARED_CLAIMS);
            if (sharedAttributes == null) {
                LOGGER.error("shared_claims not found in JWT");
                return "shared_claims not found in JWT";
            }
            return OBJECT_MAPPER.writeValueAsString(sharedAttributes);
        } catch (ParseException | JsonProcessingException e) {
            LOGGER.error("Failed to parse something: {}", e.getMessage());
            return String.format("Error: failed to parse something: %s", e.getMessage());
        }
    }

    private String getEvidenceRequested(JWTClaimsSet claimsSet) {
        try {
            Map<String, Object> evidenceRequested =
                    claimsSet.getJSONObjectClaim(EVIDENCE_REQUESTED);
            if (evidenceRequested == null) {
                LOGGER.error("evidence_requested not found in JWT");
                return "evidence_requested not found in JWT";
            }
            return OBJECT_MAPPER.writeValueAsString(evidenceRequested);
        } catch (ParseException | JsonProcessingException e) {
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
            throws ParseException, JOSEException {
        return requestedErrorResponseService.getRequestedAuthErrorResponse(authRequest);
    }

    private void handleF2fRequests(
            F2fDetails f2fDetails, String userId, String state, String signedVcJwt)
            throws IOException, InterruptedException {
        if (f2fDetails == null) {
            return;
        }
        if (f2fDetails.sendVcToQueue() && !f2fDetails.sendErrorToQueue()) {
            F2FEnqueueLambdaRequest enqueueLambdaRequest =
                    new F2FEnqueueLambdaRequest(
                            f2fDetails.queueName(),
                            new CriResponseQueueEvent(userId, state, List.of(signedVcJwt)),
                            requireNonNullElse(
                                    f2fDetails.delaySeconds(), F2F_DEFAULT_DELAY_SECONDS));

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(getConfigValue(F2F_STUB_QUEUE_URL)))
                            .header(X_API_KEY, getConfigValue(F2F_STUB_QUEUE_API_KEY))
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            OBJECT_MAPPER.writeValueAsString(enqueueLambdaRequest)))
                            .build();

            var responseStatusCode =
                    httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (responseStatusCode < 200 || responseStatusCode > 299) {
                LOGGER.warn(
                        String.format(
                                "failed to send VC to F2F queue - status code: %d",
                                responseStatusCode));
            }
        }

        if (f2fDetails.sendErrorToQueue()) {
            F2FErrorEnqueueLambdaRequest enqueueLambdaRequest =
                    new F2FErrorEnqueueLambdaRequest(
                            f2fDetails.queueName(),
                            new CriResponseQueueErrorEvent(
                                    userId, state, "access_denied", "Something went wrong"),
                            requireNonNullElse(
                                    f2fDetails.delaySeconds(), F2F_DEFAULT_DELAY_SECONDS));

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(getConfigValue(F2F_STUB_QUEUE_URL)))
                            .header(X_API_KEY, getConfigValue(F2F_STUB_QUEUE_API_KEY))
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            OBJECT_MAPPER.writeValueAsString(enqueueLambdaRequest)))
                            .build();

            var responseStatusCode =
                    httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (responseStatusCode < 200 || responseStatusCode > 299) {
                LOGGER.warn(
                        String.format(
                                "failed to send error to F2F queue - status code: %d",
                                responseStatusCode));
            }
        }
    }
}
