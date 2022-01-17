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
import uk.gov.di.ipv.stub.cred.service.AuthCodeService;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.utils.ViewHelper;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;
import uk.gov.di.ipv.stub.cred.validation.Validator;

import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;


public class AuthorizeHandler {

    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String ERROR_CODE_INVALID_REDIRECT_URI = "invalid_request_redirect_uri";
    private static final String ACTIVITY_HISTORY_PARAM_NAME = "activityHistory";
    private static final String IDENTITY_FRAUD_PARAM_NAME = "identityFraud";
    private static final String VERIFICATION_PARAM_NAME = "verification";


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

        return viewHelper.render(
                Map.of("resource-id", UUID.randomUUID().toString()),
                "authorize.mustache");
    };

    public Route generateAuthCode = (Request request, Response response) -> {
        QueryParamsMap queryParamsMap = request.queryMap();

        Map<String, Object> jsonMap = verifyJsonPayload(queryParamsMap.value("jsonPayload"));
        Map<String, String> gpgAttributes = verifyGpgAttributes(
                queryParamsMap.value(ACTIVITY_HISTORY_PARAM_NAME),
                queryParamsMap.value(IDENTITY_FRAUD_PARAM_NAME),
                queryParamsMap.value(VERIFICATION_PARAM_NAME)
        );

        jsonMap.putAll(gpgAttributes);

        AuthorizationCode authorizationCode = new AuthorizationCode();

        AuthorizationSuccessResponse successResponse = new AuthorizationSuccessResponse(
                URI.create(queryParamsMap.value(RequestParamConstants.REDIRECT_URI)),
                authorizationCode,
                null,
                State.parse(queryParamsMap.value(RequestParamConstants.STATE)),
                ResponseMode.QUERY
        );

        response.type(DEFAULT_RESPONSE_CONTENT_TYPE);
        response.redirect(successResponse.toURI().toString());

        String resourceId = queryParamsMap.value(RequestParamConstants.RESOURCE_ID);
        this.authCodeService.persist(authorizationCode, resourceId);
        this.credentialService.persist(jsonMap, resourceId);

        // No content required in response
        return null;
    };

    private Map<String, Object> verifyJsonPayload(String payload) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(payload, Map.class);
    }

    private Map<String, String> verifyGpgAttributes(String activityHistory, String identityFraud, String verification) {
        Map<String, String> gpgMap = new HashMap<>();
        gpgMap.put("activityHistory", activityHistory);
        gpgMap.put("identityFraud", identityFraud);
        gpgMap.put("verification", verification);

        return gpgMap;
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
