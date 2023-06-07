package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import org.eclipse.jetty.http.HttpHeader;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.cred.config.ClientConfig;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.service.TokenService;
import uk.gov.di.ipv.stub.cred.utils.JwtHelper;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;
import uk.gov.di.ipv.stub.cred.validation.Validator;

import javax.servlet.http.HttpServletResponse;

import java.util.Objects;

public class F2FHandler {

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

                tokenService.revoke(accessTokenString);

                QueryParamsMap queryParamsMap = request.queryMap();
                String requestValue = queryParamsMap.value(RequestParamConstants.REQUEST);
                String clientIdValue = queryParamsMap.value(RequestParamConstants.CLIENT_ID);
                ClientConfig clientConfig = CredentialIssuerConfig.getClientConfig(clientIdValue);
                SignedJWT signedJWT =
                        JwtHelper.getSignedJWT(
                                requestValue, clientConfig.getEncryptionPrivateKey());

                String subject = signedJWT.getJWTClaimsSet().getSubject();

                response.type(JSON_RESPONSE_TYPE);
                response.status(HttpServletResponse.SC_ACCEPTED);

                var userInfo = new UserInfo(new Subject(subject));
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
