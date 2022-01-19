package uk.gov.di.ipv.stub.cred.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ResponseMode;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.id.State;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.config.CriType;
import uk.gov.di.ipv.stub.cred.error.CriStubException;
import uk.gov.di.ipv.stub.cred.service.AuthCodeService;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.utils.ViewHelper;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;
import uk.gov.di.ipv.stub.cred.validation.Validator;

import javax.servlet.http.HttpServletResponse;
import java.io.ObjectInputFilter;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class AuthorizeHandler {

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

    private AuthCodeService authCodeService;
    private CredentialService credentialService;
    private ViewHelper viewHelper;

    public AuthorizeHandler(ViewHelper viewHelper, AuthCodeService authCodeService, CredentialService credentialService) {
        Objects.requireNonNull(viewHelper);
        this.viewHelper = viewHelper;
        this.authCodeService = authCodeService;
        this.credentialService = credentialService;
    }

    public Route doAuthorize = (Request request, Response response) -> {
        QueryParamsMap queryParamsMap = request.queryMap();

        ValidationResult validationResult = validateQueryParams(queryParamsMap);

        if (!validationResult.isValid()) {
            if (validationResult.getError().getCode().equals(ERROR_CODE_INVALID_REDIRECT_URI)) {
                response.status(HttpServletResponse.SC_BAD_REQUEST);
                return validationResult.getError().getDescription();
            }

            AuthorizationErrorResponse errorResponse = new AuthorizationErrorResponse(
                    URI.create(queryParamsMap.value(RequestParamConstants.REDIRECT_URI)),
                    validationResult.getError(),
                    State.parse(queryParamsMap.value(RequestParamConstants.STATE)),
                    ResponseMode.QUERY
            );

            response.type(DEFAULT_RESPONSE_CONTENT_TYPE);
            response.redirect(errorResponse.toURI().toString());
            // No content required in response
            return null;
        }

        CriType criType = CredentialIssuerConfig.getCriType();

        Map<String, Object> frontendParams = new HashMap<>();
        frontendParams.put(RESOURCE_ID_PARAM, UUID.randomUUID().toString());
        frontendParams.put(IS_EVIDENCE_TYPE_PARAM, criType.equals(CriType.EVIDENCE_CRI_TYPE));
        frontendParams.put(IS_ACTIVITY_TYPE_PARAM, criType.equals(CriType.ACTIVITY_CRI_TYPE));
        frontendParams.put(IS_FRAUD_TYPE_PARAM, criType.equals(CriType.FRAUD_CRI_TYPE));
        frontendParams.put(IS_VERIFICATION_TYPE_PARAM, criType.equals(CriType.VERIFICATION_CRI_TYPE));

        String error = request.attribute(ERROR_PARAM);
        boolean hasError = error != null;
        frontendParams.put(HAS_ERROR_PARAM, hasError);
        if (hasError) {
            frontendParams.put(ERROR_PARAM, error);
        }

        frontendParams.put(CRI_NAME_PARAM, CredentialIssuerConfig.NAME);

        return viewHelper.render(
                frontendParams,
                "authorize.mustache");
    };

    public Route generateResponse = (Request request, Response response) -> {
        QueryParamsMap queryParamsMap = request.queryMap();
        try {
            Map<String, Object> attributesMap = generateJsonPayload(queryParamsMap.value(JSON_PAYLOAD_PARAM));

            Map<String, Object> gpgMap = generateGpg45Score(
                    CredentialIssuerConfig.getCriType(),
                    queryParamsMap.value(CredentialIssuerConfig.EVIDENCE_STRENGTH),
                    queryParamsMap.value(CredentialIssuerConfig.EVIDENCE_VALIDITY),
                    queryParamsMap.value(CriType.ACTIVITY_CRI_TYPE.value),
                    queryParamsMap.value(CriType.FRAUD_CRI_TYPE.value),
                    queryParamsMap.value(CriType.VERIFICATION_CRI_TYPE.value)
            );

            Map<String, Object> credential = new HashMap<>();
            credential.put("attributes", attributesMap);
            credential.put("gpg45Score", gpgMap);

            AuthorizationSuccessResponse successResponse = generateAuthCode(queryParamsMap);

            persistData(queryParamsMap, successResponse.getAuthorizationCode(), credential);

            response.type(DEFAULT_RESPONSE_CONTENT_TYPE);
            response.redirect(successResponse.toURI().toString());
        } catch (CriStubException e) {
            request.attribute(ERROR_PARAM, e.getMessage());
            return doAuthorize.handle(request, response);
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
                ResponseMode.QUERY
        );
    }

    private void persistData(QueryParamsMap queryParamsMap, AuthorizationCode authorizationCode, Map<String, Object> credential) {
        String resourceId = queryParamsMap.value(RequestParamConstants.RESOURCE_ID);
        this.authCodeService.persist(authorizationCode, resourceId);
        this.credentialService.persist(credential, resourceId);
    }

    private Map<String, Object> generateJsonPayload(String payload) throws CriStubException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(payload, Map.class);
        } catch(JsonProcessingException e) {
            throw new CriStubException("Invalid JSON", e);
        }
    }

    private Map<String, Object> generateGpg45Score(CriType criType, String strengthValue, String validityValue, String activityValue, String fraudValue, String verificationValue) throws CriStubException {
        ValidationResult validationResult = Validator.verifyGpg45(criType, strengthValue, validityValue, activityValue, fraudValue, verificationValue);

        if (!validationResult.isValid()) {
            throw new CriStubException(validationResult.getError().getDescription());
        }

        Map<String, Object> gpg45Score = new HashMap<>();
        switch (criType) {
            case EVIDENCE_CRI_TYPE -> {
                Map<String, Object> evidence = new HashMap<>();
                evidence.put(CredentialIssuerConfig.EVIDENCE_STRENGTH, Integer.parseInt(strengthValue));
                evidence.put(CredentialIssuerConfig.EVIDENCE_VALIDITY, Integer.parseInt(validityValue));
                gpg45Score.put(criType.value, evidence);
            }
            case ACTIVITY_CRI_TYPE -> gpg45Score.put(criType.value, Integer.parseInt(activityValue));
            case FRAUD_CRI_TYPE -> gpg45Score.put(criType.value, Integer.parseInt(fraudValue));
            case VERIFICATION_CRI_TYPE -> gpg45Score.put(criType.value, Integer.parseInt(verificationValue));
        }
        return gpg45Score;
    }

    private ValidationResult validateQueryParams(QueryParamsMap queryParams) {
        String redirectUriValue = queryParams.value(RequestParamConstants.REDIRECT_URI);
        if (Validator.isNullBlankOrEmpty(redirectUriValue)) {
            return new ValidationResult(false, new ErrorObject(
                    ERROR_CODE_INVALID_REDIRECT_URI,
                    "redirect_uri param must be provided",
                    HttpServletResponse.SC_BAD_REQUEST));
        }

        String responseTypeValue = queryParams.value(RequestParamConstants.RESPONSE_TYPE);
        if (Validator.isNullBlankOrEmpty(responseTypeValue) || !responseTypeValue.equals(ResponseType.Value.CODE.getValue())) {
            return new ValidationResult(false, OAuth2Error.UNSUPPORTED_RESPONSE_TYPE);
        }

        String clientIdValue = queryParams.value(RequestParamConstants.CLIENT_ID);
        if (Validator.isNullBlankOrEmpty(clientIdValue)) {
            return new ValidationResult(false, OAuth2Error.INVALID_CLIENT);
        }

        return ValidationResult.createValidResult();
    }
}
