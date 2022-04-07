package uk.gov.di.ipv.stub.cred.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nimbusds.jose.JOSEException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.cred.config.ClientConfig;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.config.CriType;
import uk.gov.di.ipv.stub.cred.domain.Credential;
import uk.gov.di.ipv.stub.cred.error.CriStubException;
import uk.gov.di.ipv.stub.cred.service.AuthCodeService;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.utils.ES256SignatureVerifier;
import uk.gov.di.ipv.stub.cred.utils.ViewHelper;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;
import uk.gov.di.ipv.stub.cred.validation.Validator;

import javax.servlet.http.HttpServletResponse;

import java.net.URI;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class AuthorizeHandler {

    public static final String SHARED_CLAIMS = "shared_claims";

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizeHandler.class);

    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String ERROR_CODE_INVALID_REDIRECT_URI = "invalid_request_redirect_uri";

    private static final String RESOURCE_ID_PARAM = "resourceId";
    private static final String JSON_PAYLOAD_PARAM = "jsonPayload";
    private static final String IS_EVIDENCE_TYPE_PARAM = "isEvidenceType";
    private static final String IS_ACTIVITY_TYPE_PARAM = "isActivityType";
    private static final String IS_FRAUD_TYPE_PARAM = "isFraudType";
    private static final String IS_VERIFICATION_TYPE_PARAM = "isVerificationType";
    private static final String HAS_ERROR_PARAM = "hasError";
    private static final String ERROR_PARAM = "error";
    private static final String CRI_NAME_PARAM = "cri-name";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final AuthCodeService authCodeService;
    private final CredentialService credentialService;
    private final ES256SignatureVerifier es256SignatureVerifier = new ES256SignatureVerifier();
    private ViewHelper viewHelper;

    public AuthorizeHandler(
            ViewHelper viewHelper,
            AuthCodeService authCodeService,
            CredentialService credentialService) {
        Objects.requireNonNull(viewHelper);
        this.viewHelper = viewHelper;
        this.authCodeService = authCodeService;
        this.credentialService = credentialService;
    }

    public Route doAuthorize =
            (Request request, Response response) -> {
                QueryParamsMap queryParamsMap = request.queryMap();

                ValidationResult validationResult = validateQueryParams(queryParamsMap);

                if (!validationResult.isValid()) {
                    if (validationResult
                            .getError()
                            .getCode()
                            .equals(ERROR_CODE_INVALID_REDIRECT_URI)) {
                        response.status(HttpServletResponse.SC_BAD_REQUEST);
                        return validationResult.getError().getDescription();
                    }

                    AuthorizationErrorResponse errorResponse =
                            new AuthorizationErrorResponse(
                                    URI.create(
                                            queryParamsMap.value(
                                                    RequestParamConstants.REDIRECT_URI)),
                                    validationResult.getError(),
                                    State.parse(queryParamsMap.value(RequestParamConstants.STATE)),
                                    ResponseMode.QUERY);

                    response.type(DEFAULT_RESPONSE_CONTENT_TYPE);
                    response.redirect(errorResponse.toURI().toString());
                    // No content required in response
                    return null;
                }

                String sharedClaimsJson = getSharedAttributes(queryParamsMap);

                CriType criType = CredentialIssuerConfig.getCriType();

                Map<String, Object> frontendParams = new HashMap<>();
                frontendParams.put(RESOURCE_ID_PARAM, UUID.randomUUID().toString());
                frontendParams.put(
                        IS_EVIDENCE_TYPE_PARAM, criType.equals(CriType.EVIDENCE_CRI_TYPE));
                frontendParams.put(
                        IS_ACTIVITY_TYPE_PARAM, criType.equals(CriType.ACTIVITY_CRI_TYPE));
                frontendParams.put(IS_FRAUD_TYPE_PARAM, criType.equals(CriType.FRAUD_CRI_TYPE));
                frontendParams.put(
                        IS_VERIFICATION_TYPE_PARAM, criType.equals(CriType.VERIFICATION_CRI_TYPE));
                frontendParams.put(SHARED_CLAIMS, sharedClaimsJson);

                String error = request.attribute(ERROR_PARAM);
                boolean hasError = error != null;
                frontendParams.put(HAS_ERROR_PARAM, hasError);
                if (hasError) {
                    frontendParams.put(ERROR_PARAM, error);
                }

                frontendParams.put(CRI_NAME_PARAM, CredentialIssuerConfig.NAME);

                return viewHelper.render(frontendParams, "authorize.mustache");
            };

    public Route generateResponse =
            (Request request, Response response) -> {
                QueryParamsMap queryParamsMap = request.queryMap();

                try {
                    Map<String, Object> attributesMap =
                            generateJsonPayload(queryParamsMap.value(JSON_PAYLOAD_PARAM));

                    Map<String, Object> combinedAttributeJson =
                            generateJsonPayload(getSharedAttributes(queryParamsMap));

                    combinedAttributeJson.putAll(attributesMap);

                    Map<String, Object> gpgMap =
                            generateGpg45Score(
                                    CredentialIssuerConfig.getCriType(),
                                    queryParamsMap.value(
                                            CredentialIssuerConfig.EVIDENCE_STRENGTH_PARAM),
                                    queryParamsMap.value(
                                            CredentialIssuerConfig.EVIDENCE_VALIDITY_PARAM),
                                    queryParamsMap.value(CredentialIssuerConfig.ACTIVITY_PARAM),
                                    queryParamsMap.value(CredentialIssuerConfig.FRAUD_PARAM),
                                    queryParamsMap.value(
                                            CredentialIssuerConfig.VERIFICATION_PARAM));

                    Credential credential = new Credential(combinedAttributeJson, gpgMap);

                    AuthorizationSuccessResponse successResponse = generateAuthCode(queryParamsMap);

                    persistData(queryParamsMap, successResponse.getAuthorizationCode(), credential);

                    response.type(DEFAULT_RESPONSE_CONTENT_TYPE);
                    response.redirect(successResponse.toURI().toString());
                } catch (CriStubException e) {
                    AuthorizationErrorResponse errorResponse =
                            generateErrorResponse(queryParamsMap, e);
                    response.redirect(errorResponse.toURI().toString());
                }
                return null;
            };

    private AuthorizationSuccessResponse generateAuthCode(QueryParamsMap queryParamsMap) {
        AuthorizationCode authorizationCode = new AuthorizationCode();

        return new AuthorizationSuccessResponse(
                URI.create(queryParamsMap.value(RequestParamConstants.REDIRECT_URI)),
                authorizationCode,
                null,
                State.parse(queryParamsMap.value(RequestParamConstants.STATE)),
                ResponseMode.QUERY);
    }

    private AuthorizationErrorResponse generateErrorResponse(
            QueryParamsMap queryParamsMap, CriStubException error) {
        return new AuthorizationErrorResponse(
                URI.create(queryParamsMap.value(RequestParamConstants.REDIRECT_URI)),
                new ErrorObject(error.getMessage(), error.getDescription()),
                null,
                new Issuer(CredentialIssuerConfig.NAME),
                ResponseMode.QUERY);
    }

    private void persistData(
            QueryParamsMap queryParamsMap,
            AuthorizationCode authorizationCode,
            Credential credential) {
        String resourceId = queryParamsMap.value(RequestParamConstants.RESOURCE_ID);
        String redirectUrl = queryParamsMap.value(RequestParamConstants.REDIRECT_URI);
        this.authCodeService.persist(authorizationCode, resourceId, redirectUrl);
        this.credentialService.persist(credential, resourceId);
    }

    private Map<String, Object> generateJsonPayload(String payload) throws CriStubException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(payload, Map.class);
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
            String verificationValue)
            throws CriStubException {
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
        switch (criType) {
            case EVIDENCE_CRI_TYPE -> {
                Map<String, Object> evidence = new HashMap<>();
                evidence.put(
                        CredentialIssuerConfig.EVIDENCE_STRENGTH_PARAM,
                        Integer.parseInt(strengthValue));
                evidence.put(
                        CredentialIssuerConfig.EVIDENCE_VALIDITY_PARAM,
                        Integer.parseInt(validityValue));
                gpg45Score.put(criType.value, evidence);
            }
            case ACTIVITY_CRI_TYPE -> gpg45Score.put(
                    CredentialIssuerConfig.ACTIVITY_PARAM, Integer.parseInt(activityValue));
            case FRAUD_CRI_TYPE -> gpg45Score.put(
                    CredentialIssuerConfig.FRAUD_PARAM, Integer.parseInt(fraudValue));
            case VERIFICATION_CRI_TYPE -> gpg45Score.put(
                    CredentialIssuerConfig.VERIFICATION_PARAM, Integer.parseInt(verificationValue));
        }
        return gpg45Score;
    }

    private ValidationResult validateQueryParams(QueryParamsMap queryParams) {
        String responseTypeValue = queryParams.value(RequestParamConstants.RESPONSE_TYPE);
        if (Validator.isNullBlankOrEmpty(responseTypeValue)
                || !responseTypeValue.equals(ResponseType.Value.CODE.getValue())) {
            return new ValidationResult(false, OAuth2Error.UNSUPPORTED_RESPONSE_TYPE);
        }

        String clientIdValue = queryParams.value(RequestParamConstants.CLIENT_ID);
        if (Validator.isNullBlankOrEmpty(clientIdValue)
                || CredentialIssuerConfig.getClientConfig(clientIdValue) == null) {
            return new ValidationResult(false, OAuth2Error.INVALID_CLIENT);
        }

        String redirectUrl = queryParams.value(RequestParamConstants.REDIRECT_URI);
        if (Validator.isNullBlankOrEmpty(redirectUrl)) {
            return new ValidationResult(
                    false,
                    new ErrorObject(
                            ERROR_CODE_INVALID_REDIRECT_URI,
                            "redirect_uri param must be provided",
                            HttpServletResponse.SC_BAD_REQUEST));
        }

        if (Validator.redirectUrlIsInvalid(queryParams)) {
            return new ValidationResult(
                    false,
                    new ErrorObject(
                            ERROR_CODE_INVALID_REDIRECT_URI,
                            "redirect_uri param provided does not match any of the redirect_uri values configured",
                            HttpServletResponse.SC_BAD_REQUEST));
        }

        return ValidationResult.createValidResult();
    }

    private String getSharedAttributes(QueryParamsMap queryParamsMap) {
        String requestParam = queryParamsMap.value(RequestParamConstants.REQUEST);
        String clientIdParam = queryParamsMap.value(RequestParamConstants.CLIENT_ID);

        if (MapUtils.isEmpty(CredentialIssuerConfig.getClientConfigs())) {
            return "Error: Missing cri stub client configuration env variable";
        }

        ClientConfig clientConfig = CredentialIssuerConfig.getClientConfig(clientIdParam);

        if (clientConfig == null) {
            return "Error: Could not find client configuration details for: " + clientIdParam;
        }

        String sharedAttributesJson;
        if (!Validator.isNullBlankOrEmpty(requestParam)) {
            try {
                SignedJWT signedJWT = SignedJWT.parse(requestParam);

                if (!es256SignatureVerifier.valid(signedJWT, clientConfig.getSigningPublicJwk())) {
                    LOGGER.error("JWT signature is invalid");
                    return "Error: Signature of the shared attribute JWT is not valid";
                }

                JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
                Map<String, Object> sharedClaims = claimsSet.getJSONObjectClaim(SHARED_CLAIMS);

                if (sharedClaims == null) {
                    LOGGER.error("shared_claims not found in JWT");
                    return "Error: shared_claims not found in JWT";
                }

                sharedAttributesJson = gson.toJson(sharedClaims);
            } catch (ParseException e) {
                LOGGER.error("Failed to parse something: {}", e.getMessage());
                sharedAttributesJson =
                        String.format("Error: failed to parse something: %s", e.getMessage());
            } catch (JOSEException e) {
                LOGGER.error("Failed to verify the signature of the JWT", e);
                sharedAttributesJson =
                        "Error: failed to verify the signature of the shared attribute JWT";
            }
        } else {
            sharedAttributesJson = "Error: missing 'request' query parameter";
        }
        return sharedAttributesJson;
    }
}
