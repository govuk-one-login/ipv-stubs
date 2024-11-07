package uk.gov.di.ipv.stub.orc.handlers;

import io.javalin.http.Context;
import io.javalin.http.Header;
import io.javalin.http.UnauthorizedResponse;
import uk.gov.di.ipv.stub.orc.config.OrchestratorConfig;

import java.util.Base64;

public class BasicAuthHandler {
    private static final int NUMBER_OF_AUTHENTICATION_FIELDS = 2;
    private static final String AUTHORIZATION_TYPE = "Basic";
    private static final String JWKS_PATH = "/.well-known/jwks.json";

    public void authFilter(Context ctx) {
        if (ctx.path().equals(JWKS_PATH)) {
            // We don't need basic auth on the jwks endpoint
            return;
        }

        var authHeader = ctx.header(Header.AUTHORIZATION);
        if (authHeader == null || !authenticated(authHeader)) {
            ctx.header(Header.WWW_AUTHENTICATE, AUTHORIZATION_TYPE);
            throw new UnauthorizedResponse("Not authenticated");
        }
    }

    private Boolean authenticated(String authHeader) {
        int authTypeIndex = authHeader == null ? -1 : authHeader.indexOf(AUTHORIZATION_TYPE);

        if (authTypeIndex < 0) {
            return false;
        }

        String encodedHeader =
                authHeader.substring(authTypeIndex + AUTHORIZATION_TYPE.length() + 1).trim();
        String[] submittedCredentials = extractCredentials(encodedHeader);

        if (submittedCredentials.length == NUMBER_OF_AUTHENTICATION_FIELDS) {
            String submittedUsername = submittedCredentials[0];
            String submittedPassword = submittedCredentials[1];
            String username = OrchestratorConfig.BASIC_AUTH_USERNAME;
            String password = OrchestratorConfig.BASIC_AUTH_PASSWORD;
            return submittedUsername.equals(username) && submittedPassword.equals(password);
        }
        return false;
    }

    private String[] extractCredentials(String encodedHeader) {
        String decodedHeader = new String(Base64.getDecoder().decode(encodedHeader));
        return decodedHeader.split(":");
    }
}
