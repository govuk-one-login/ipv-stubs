package uk.gov.di.ipv.stub.cred.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import org.eclipse.jetty.http.HttpHeader;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.service.TokenService;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;
import uk.gov.di.ipv.stub.cred.validation.Validator;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Objects;

public class CredentialHandler {

    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "application/json;charset=UTF-8";

    private CredentialService credentialService;
    private TokenService tokenService;
    private ObjectMapper objectMapper;

    public CredentialHandler(CredentialService credentialService, TokenService tokenService, ObjectMapper objectMapper) {
        this.credentialService = credentialService;
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    public Route getResource = (Request request, Response response) -> {
        String accessTokenString = request.headers(HttpHeader.AUTHORIZATION.toString());

        ValidationResult validationResult = validateAccessToken(accessTokenString);

        if (!validationResult.isValid()) {
            response.status(validationResult.getError().getHTTPStatusCode());
            return validationResult.getError().getDescription();
        }

        response.type(DEFAULT_RESPONSE_CONTENT_TYPE);
        response.status(HttpServletResponse.SC_OK);

        String resourceId = tokenService.getPayload(accessTokenString);
        Map<String, Object> protectedResource = credentialService.getCredential(resourceId);

        tokenService.revoke(accessTokenString);

        return objectMapper.writeValueAsString(protectedResource);
    };

    private ValidationResult validateAccessToken(String accessTokenString) {
        if (Validator.isNullBlankOrEmpty(accessTokenString)) {
            return new ValidationResult(false, OAuth2Error.INVALID_REQUEST);
        }

        if (Objects.isNull(this.tokenService.getPayload(accessTokenString))) {
            return new ValidationResult(false, OAuth2Error.INVALID_CLIENT);
        }

        try {
            AccessToken.parse(accessTokenString);
        } catch (ParseException e) {
            return new ValidationResult(false, OAuth2Error.INVALID_REQUEST);
        }

        return ValidationResult.createValidResult();
    }
}
