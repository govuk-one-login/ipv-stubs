package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.cred.service.AuthCodeService;
import uk.gov.di.ipv.stub.cred.service.TokenService;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;
import uk.gov.di.ipv.stub.cred.validation.Validator;

import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

public class TokenHandler {

    private TokenService tokenService;
    private AuthCodeService authCodeService;

    public TokenHandler(AuthCodeService authCodeService, TokenService tokenService) {
        this.authCodeService = authCodeService;
        this.tokenService = tokenService;
    }

    public Route issueAccessToken = (Request request, Response response) -> {
        QueryParamsMap requestParams = request.queryMap();
        ValidationResult validationResult = validateTokenRequest(requestParams);

        response.type("application/json;charset=UTF-8");

        if (!validationResult.isValid()) {
            TokenErrorResponse errorResponse = new TokenErrorResponse(validationResult.getError());
            response.status(validationResult.getError().getHTTPStatusCode());

            return errorResponse.toJSONObject().toJSONString();
        }

        AccessToken accessToken = tokenService.createBearerAccessToken();
        AccessTokenResponse tokenResponse = new AccessTokenResponse(new Tokens(accessToken, new RefreshToken()));

        String code = requestParams.value(RequestParamConstants.AUTH_CODE);
        String payloadAssociatedWithCode = authCodeService.getPayload(code);
        authCodeService.revoke(code);
        tokenService.persist(accessToken, payloadAssociatedWithCode);

        response.status(HttpServletResponse.SC_OK);
        return tokenResponse.toJSONObject().toJSONString();
    };

    private ValidationResult validateTokenRequest(QueryParamsMap requestParams) {
        String grantTypeValue = requestParams.value(RequestParamConstants.GRANT_TYPE);
        if (Validator.isNullBlankOrEmpty(grantTypeValue) || !grantTypeValue.equalsIgnoreCase(GrantType.AUTHORIZATION_CODE.getValue())) {
            return new ValidationResult(false, OAuth2Error.UNSUPPORTED_GRANT_TYPE);
        }

        String authCodeValue = requestParams.value(RequestParamConstants.AUTH_CODE);
        if (Validator.isNullBlankOrEmpty(authCodeValue)) {
            return new ValidationResult(false, OAuth2Error.INVALID_GRANT);
        }
        if (Objects.isNull(this.authCodeService.getPayload(authCodeValue))) {
            return new ValidationResult(false, OAuth2Error.INVALID_GRANT);
        }

        String redirectUriValue = requestParams.value(RequestParamConstants.REDIRECT_URI);
        if (Validator.isNullBlankOrEmpty(redirectUriValue)) {
            return new ValidationResult(false, OAuth2Error.INVALID_REQUEST);
        }

        String clientIdValue = requestParams.value(RequestParamConstants.CLIENT_ID);
        if (Validator.isNullBlankOrEmpty(clientIdValue)) {
            return new ValidationResult(false, OAuth2Error.INVALID_CLIENT);
        }
        return ValidationResult.createValidResult();
    }
}
