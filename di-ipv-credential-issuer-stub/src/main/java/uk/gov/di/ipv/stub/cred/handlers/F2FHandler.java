package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import org.eclipse.jetty.http.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.cred.service.TokenService;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;
import uk.gov.di.ipv.stub.cred.validation.Validator;

import javax.servlet.http.HttpServletResponse;

import java.util.Objects;
import java.util.UUID;

public class F2FHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(F2FHandler.class);
    private static final String JSON_RESPONSE_TYPE = "application/json;charset=UTF-8";
    private TokenService tokenService;

    public F2FHandler(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public Route getResource =
            (Request request, Response response) -> {
                String accessTokenString = request.headers(HttpHeader.AUTHORIZATION.toString());
                ValidationResult validationResult = validateAccessToken(accessTokenString);

                if (!validationResult.isValid()) {
                    response.status(validationResult.getError().getHTTPStatusCode());
                    return validationResult.getError().getDescription();
                }

                response.type(JSON_RESPONSE_TYPE);
                response.status(HttpServletResponse.SC_ACCEPTED);

                var userInfo =
                        new UserInfo(new Subject("urn:fdc:gov.uk:2022:" + UUID.randomUUID()));
                userInfo.setClaim("https://vocab.account.gov.uk/v1/credentialStatus", "pending");

                return userInfo.toJSONString();
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
