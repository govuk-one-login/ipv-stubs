package uk.gov.di.ipv.stub.cred.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import org.eclipse.jetty.http.HttpHeader;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.cred.entity.ProtectedResource;
import uk.gov.di.ipv.stub.cred.service.ProtectedResourceService;
import uk.gov.di.ipv.stub.cred.validation.ErrorMessageKeys;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;
import uk.gov.di.ipv.stub.cred.validation.Validator;

import javax.servlet.http.HttpServletResponse;
import java.util.ResourceBundle;

public class ResourceHandler {

    private static final String SESSION_ACCESS_TOKEN_KEY = "accessToken";
    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "application/json";
    private static final String ERROR_CODE_INVALID_ACCESS_TOKEN = "invalid_token";
    private static final String ERROR_CODE_MISSING_REQUEST_TOKEN = "missing_request_token";
    private static final String ERROR_CODE_MISSING_SESSION_TOKEN = "missing_session_token";
    private static final String ERROR_CODE_UNEXPECTED_ACCESS_TOKEN = "unexpected_access_token";

    private ProtectedResourceService protectedResourceService;

    public ResourceHandler(ProtectedResourceService protectedResourceService) {
        this.protectedResourceService = protectedResourceService;
    }

    public Route getResource = (Request request, Response response) -> {
        String accessTokenString = request.headers(HttpHeader.AUTHORIZATION.toString());
        String sessionTokenString = request.session().attribute(SESSION_ACCESS_TOKEN_KEY);

        ValidationResult validationResult = validateAccessToken(accessTokenString, sessionTokenString);

        if (!validationResult.isValid()) {
            response.status(HttpServletResponse.SC_BAD_REQUEST);
            return validationResult.getErrorDescription();
        }

        response.type(DEFAULT_RESPONSE_CONTENT_TYPE);
        response.status(HttpServletResponse.SC_OK);

        ProtectedResource protectedResource = protectedResourceService.getProtectedResource();
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(protectedResource);
    };



    private ValidationResult validateAccessToken(String accessTokenString, String sessionTokenString) {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("ErrorMessages");

        if (Validator.isNullBlankOrEmpty(accessTokenString)) {
            return new ValidationResult(false, ERROR_CODE_MISSING_REQUEST_TOKEN, resourceBundle.getString(ErrorMessageKeys.MISSING_REQUEST_TOKEN.getKey()));
        }

        if (Validator.isNullBlankOrEmpty(sessionTokenString)) {
            return new ValidationResult(false, ERROR_CODE_MISSING_SESSION_TOKEN, resourceBundle.getString(ErrorMessageKeys.MISSING_SESSION_TOKEN.getKey()));
        }

        if (!accessTokenString.equals(sessionTokenString)) {
            return new ValidationResult(false, ERROR_CODE_UNEXPECTED_ACCESS_TOKEN, resourceBundle.getString(ErrorMessageKeys.UNEXPECTED_ACCESS_TOKEN.getKey()));
        }

        try {
            AccessToken.parse(accessTokenString);
        } catch (ParseException e) {
            return new ValidationResult(false, ERROR_CODE_INVALID_ACCESS_TOKEN, resourceBundle.getString(ErrorMessageKeys.INVALID_ACCESS_TOKEN.getKey()));
        }

        return new ValidationResult(true);
    }
}
