package uk.gov.di.ipv.stub.cred.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import org.eclipse.jetty.http.HttpHeader;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.cred.entity.ProtectedResource;
import uk.gov.di.ipv.stub.cred.service.ProtectedResourceService;
import uk.gov.di.ipv.stub.cred.service.TokenService;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;
import uk.gov.di.ipv.stub.cred.validation.Validator;

import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

public class ResourceHandler {

    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "application/json;charset=UTF-8";

    private ProtectedResourceService protectedResourceService;
    private TokenService tokenService;

    public ResourceHandler(ProtectedResourceService protectedResourceService, TokenService tokenService) {
        this.protectedResourceService = protectedResourceService;
        this.tokenService = tokenService;
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
        ProtectedResource protectedResource = protectedResourceService.getProtectedResource(resourceId);

        tokenService.revoke(accessTokenString);

        ObjectMapper objectMapper = new ObjectMapper();
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
