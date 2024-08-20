package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import io.javalin.http.Context;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.stub.cred.auth.ClientJwtVerifier;
import uk.gov.di.ipv.stub.cred.config.ClientConfig;
import uk.gov.di.ipv.stub.cred.config.CriType;
import uk.gov.di.ipv.stub.cred.error.ClientAuthenticationException;
import uk.gov.di.ipv.stub.cred.service.AuthCodeService;
import uk.gov.di.ipv.stub.cred.service.ConfigService;
import uk.gov.di.ipv.stub.cred.service.RequestedErrorResponseService;
import uk.gov.di.ipv.stub.cred.service.TokenService;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;
import uk.gov.di.ipv.stub.cred.validation.Validator;

import static uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig.getCriType;

public class TokenHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenHandler.class);
    private static final String RESPONSE_TYPE = "application/json;charset=UTF-8";
    private static final String AUTHENTICATION_METHOD = "authenticationMethod";
    private static final String NONE_AUTHENTICATION_METHOD = "none";

    private TokenService tokenService;
    private AuthCodeService authCodeService;
    private Validator validator;
    private ClientJwtVerifier clientJwtVerifier;
    private RequestedErrorResponseService requestedErrorResponseService;

    public TokenHandler(
            AuthCodeService authCodeService,
            TokenService tokenService,
            Validator validator,
            ClientJwtVerifier clientJwtVerifier,
            RequestedErrorResponseService requestedErrorResponseService) {
        this.authCodeService = authCodeService;
        this.tokenService = tokenService;
        this.validator = validator;
        this.clientJwtVerifier = clientJwtVerifier;
        this.requestedErrorResponseService = requestedErrorResponseService;
    }

    public void issueAccessToken(Context ctx) {
        TokenErrorResponse requestedTokenErrorResponse =
                handleRequestedError(ctx.formParam(RequestParamConstants.AUTH_CODE));
        if (requestedTokenErrorResponse != null) {
            ctx.status(HttpStatus.BAD_REQUEST_400);
            ctx.json(requestedTokenErrorResponse.toJSONObject());
            return;
        }

        ValidationResult validationResult = validator.validateTokenRequest(ctx);
        if (!validationResult.isValid()) {
            TokenErrorResponse errorResponse = new TokenErrorResponse(validationResult.getError());
            ctx.status(validationResult.getError().getHTTPStatusCode());
            ctx.json(errorResponse.toJSONObject());
            return;
        }

        if (getCriType().equals(CriType.DOC_CHECK_APP_CRI_TYPE)
                || Validator.isNullBlankOrEmpty(ctx.formParam(RequestParamConstants.CLIENT_ID))) {
            try {
                clientJwtVerifier.authenticateClient(ctx);
            } catch (ClientAuthenticationException e) {
                LOGGER.error("Failed client JWT authentication: {}", e.getMessage());
                TokenErrorResponse errorResponse =
                        new TokenErrorResponse(OAuth2Error.INVALID_CLIENT);
                ctx.status(OAuth2Error.INVALID_CLIENT.getHTTPStatusCode());
                ctx.json(errorResponse.toJSONObject());
                return;
            }

        } else {
            ClientConfig clientConfig =
                    ConfigService.getClientConfig(ctx.formParam(RequestParamConstants.CLIENT_ID));
            String authMethod = clientConfig.getJwtAuthentication().getAuthenticationMethod();
            if (!authMethod.equals(NONE_AUTHENTICATION_METHOD)) {
                TokenErrorResponse errorResponse =
                        new TokenErrorResponse(OAuth2Error.INVALID_REQUEST);
                ctx.status(OAuth2Error.INVALID_REQUEST.getHTTPStatusCode());
                ctx.json(errorResponse.toJSONObject());
                return;
            }
        }

        String code = ctx.formParam(RequestParamConstants.AUTH_CODE);
        var redirectValidationResult =
                validator.validateRedirectUrlsMatch(
                        authCodeService.getRedirectUrl(code),
                        ctx.formParam(RequestParamConstants.REDIRECT_URI));

        if (!redirectValidationResult.isValid()) {
            TokenErrorResponse errorResponse =
                    new TokenErrorResponse(redirectValidationResult.getError());
            ctx.status(redirectValidationResult.getError().getHTTPStatusCode());
            ctx.json(errorResponse.toJSONObject());
            return;
        }

        AccessToken accessToken = tokenService.createBearerAccessToken();
        AccessTokenResponse tokenResponse =
                new AccessTokenResponse(new Tokens(accessToken, new RefreshToken()));

        String payloadAssociatedWithCode = authCodeService.getPayload(code);
        authCodeService.revoke(code);
        tokenService.persist(accessToken, payloadAssociatedWithCode);

        requestedErrorResponseService.persistUserInfoErrorAgainstToken(
                code, accessToken.toString());

        ctx.json(tokenResponse.toJSONObject());
    }

    private TokenErrorResponse handleRequestedError(String authCode) {
        if (authCode == null) {
            return null;
        }
        return requestedErrorResponseService.getRequestedAccessTokenErrorResponse(authCode);
    }
}
