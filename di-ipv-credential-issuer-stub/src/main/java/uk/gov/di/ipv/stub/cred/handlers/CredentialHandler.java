package uk.gov.di.ipv.stub.cred.handlers;

import org.eclipse.jetty.http.HttpHeader;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.service.TokenService;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;

import javax.servlet.http.HttpServletResponse;

public class CredentialHandler {

    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "application/jwt;charset=UTF-8";

    private CredentialService credentialService;
    private TokenService tokenService;

    public CredentialHandler(CredentialService credentialService, TokenService tokenService) {
        this.credentialService = credentialService;
        this.tokenService = tokenService;
    }

    public Route getResource =
            (Request request, Response response) -> {
                String accessTokenString = request.headers(HttpHeader.AUTHORIZATION.toString());

                ValidationResult validationResult =
                        tokenService.validateAccessToken(accessTokenString);

                if (!validationResult.isValid()) {
                    response.status(validationResult.getError().getHTTPStatusCode());
                    return validationResult.getError().getDescription();
                }

                String resourceId = tokenService.getPayload(accessTokenString);
                String verifiableCredential = credentialService.getCredentialSignedJwt(resourceId);

                tokenService.revoke(accessTokenString);

                response.type(DEFAULT_RESPONSE_CONTENT_TYPE);
                response.status(HttpServletResponse.SC_CREATED);

                return verifiableCredential;
            };
}
