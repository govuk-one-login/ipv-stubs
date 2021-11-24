package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ResponseMode;
import com.nimbusds.oauth2.sdk.id.State;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.cred.utils.ViewHelper;
import uk.gov.di.ipv.stub.cred.validation.ErrorMessageKeys;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;
import uk.gov.di.ipv.stub.cred.validation.Validator;

import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.ResourceBundle;

public class AuthorizeHandler {

    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String ERROR_CODE_INVALID_REQUEST = "invalid_request";
    private static final String ERROR_CODE_INVALID_RESPONSE_TYPE = "unsupported_response_type";
    private static final String ERROR_CODE_INVALID_REDIRECT_URI = "invalid_request_redirect_uri";

    private ViewHelper viewHelper;

    public AuthorizeHandler(ViewHelper viewHelper) {
        Objects.requireNonNull(viewHelper);
        this.viewHelper = viewHelper;
    }

    public Route doAuthorize = (Request request, Response response) -> {
        QueryParamsMap queryParamsMap = request.queryMap();

        ValidationResult validationResult = validateQueryParams(queryParamsMap);

        if (!validationResult.isValid()) {
            if (validationResult.getErrorCode().equals(ERROR_CODE_INVALID_REDIRECT_URI)) {
                response.status(HttpServletResponse.SC_BAD_REQUEST);
                return validationResult.getErrorDescription();
            }

            ErrorObject errorObject = new ErrorObject(
                    validationResult.getErrorCode(),
                    validationResult.getErrorDescription()
            );
            AuthorizationErrorResponse errorResponse = new AuthorizationErrorResponse(
                    URI.create(queryParamsMap.value(QueryStringParamConstants.QUERY_STRING_PARAM_REDIRECT_URI)),
                    errorObject,
                    State.parse(queryParamsMap.value(QueryStringParamConstants.QUERY_STRING_PARAM_STATE)),
                    ResponseMode.QUERY
            );

            response.type(DEFAULT_RESPONSE_CONTENT_TYPE);
            response.redirect(errorResponse.toURI().toString());
            // No content required in response
            return null;
        }

        return viewHelper.render(Collections.emptyMap(), "authorize.mustache");
    };


    public Route generateAuthCode = (Request request, Response response) -> {
        QueryParamsMap queryParamsMap = request.queryMap();

        AuthorizationCode authorizationCode = new AuthorizationCode();

        AuthorizationSuccessResponse successResponse = new AuthorizationSuccessResponse(
                URI.create(queryParamsMap.value(QueryStringParamConstants.QUERY_STRING_PARAM_REDIRECT_URI)),
                authorizationCode,
                null,
                State.parse(queryParamsMap.value(QueryStringParamConstants.QUERY_STRING_PARAM_STATE)),
                ResponseMode.QUERY
        );

        response.type(DEFAULT_RESPONSE_CONTENT_TYPE);
        response.redirect(successResponse.toURI().toString());
        // No content required in response
        return null;
    };

    private ValidationResult validateQueryParams(QueryParamsMap queryParams) {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("ErrorMessages");
        String redirectUriValue = queryParams.value(QueryStringParamConstants.QUERY_STRING_PARAM_REDIRECT_URI);
        if (Validator.isNullBlankOrEmpty(redirectUriValue)) {
            return new ValidationResult(false, ERROR_CODE_INVALID_REDIRECT_URI, resourceBundle.getString(ErrorMessageKeys.MISSING_REDIRECT_URI.getKey()));
        }

        String responseTypeValue = queryParams.value(QueryStringParamConstants.QUERY_STRING_PARAM_RESPONSE_TYPE);
        if (Validator.isNullBlankOrEmpty(responseTypeValue)) {
            return new ValidationResult(false, ERROR_CODE_INVALID_REQUEST, resourceBundle.getString(ErrorMessageKeys.MISSING_RESPONSE_TYPE.getKey()));
        }

        if (!responseTypeValue.equalsIgnoreCase(QueryStringParamConstants.PERMITTED_RESPONSE_TYPE)) {
            return new ValidationResult(false, ERROR_CODE_INVALID_RESPONSE_TYPE, resourceBundle.getString(ErrorMessageKeys.INVALID_RESPONSE_TYPE.getKey()));
        }

        String clientIdValue = queryParams.value(QueryStringParamConstants.QUERY_STRING_PARAM_CLIENT_ID);
        if (Validator.isNullBlankOrEmpty(clientIdValue)) {
            return new ValidationResult(false, ERROR_CODE_INVALID_REQUEST, resourceBundle.getString(ErrorMessageKeys.MISSING_CLIENT_ID.getKey()));
        }

        return new ValidationResult(true);
    }
}
