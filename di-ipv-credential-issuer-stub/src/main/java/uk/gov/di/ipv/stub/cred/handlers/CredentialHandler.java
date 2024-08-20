package uk.gov.di.ipv.stub.cred.handlers;

import io.javalin.http.Context;
import io.javalin.http.Header;
import io.javalin.http.HttpStatus;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.service.TokenService;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;

public class CredentialHandler {

    private static final String JWT_CONTENT_TYPE = "application/jwt;charset=UTF-8";

    private final CredentialService credentialService;
    private final TokenService tokenService;

    public CredentialHandler(CredentialService credentialService, TokenService tokenService) {
        this.credentialService = credentialService;
        this.tokenService = tokenService;
    }

    public void getResource(Context ctx) throws Exception {
        String accessTokenString = ctx.header(Header.AUTHORIZATION);

        ValidationResult validationResult = tokenService.validateAccessToken(accessTokenString);

        if (!validationResult.isValid()) {
            ctx.status(validationResult.getError().getHTTPStatusCode());
            ctx.result(validationResult.getError().getDescription());
            return;
        }

        String resourceId = tokenService.getPayload(accessTokenString);
        String verifiableCredential = credentialService.getCredentialSignedJwt(resourceId);

        tokenService.revoke(accessTokenString);

        sendResponse(ctx, verifiableCredential);
    }

    protected void sendResponse(Context ctx, String verifiableCredential) throws Exception {
        ctx.contentType(JWT_CONTENT_TYPE);
        ctx.status(HttpStatus.CREATED);
        ctx.result(verifiableCredential);
    }
}
