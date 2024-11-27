package uk.gov.di.ipv.stub.core.handlers;

import io.javalin.http.Handler;
import io.javalin.http.Header;
import io.javalin.http.UnauthorizedResponse;
import uk.gov.di.ipv.stub.core.config.CoreStubConfig;

import java.util.Base64;

public class BasicAuthHandler {
    private static final int NUMBER_OF_AUTHENTICATION_FIELDS = 2;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String AUTHORIZATION_TYPE = "Basic";

    public BasicAuthHandler() {
        CoreStubConfig.getUserAuth();
    }

    public Handler authFilter =
            ctx -> {
                String authHeader = ctx.header(AUTHORIZATION_HEADER);
                if (authHeader == null || !authenticated(authHeader)) {
                    ctx.header(Header.WWW_AUTHENTICATE, AUTHORIZATION_TYPE);
                    throw new UnauthorizedResponse();
                }
            };

    private static Boolean authenticated(String authHeader) {
        int authTypeIndex = authHeader.indexOf(AUTHORIZATION_TYPE);

        if (authTypeIndex < 0) {
            return false;
        }

        String encodedHeader =
                authHeader.substring(authTypeIndex + AUTHORIZATION_TYPE.length() + 1).trim();
        String[] submittedCredentials = extractCredentials(encodedHeader);

        if (submittedCredentials.length == NUMBER_OF_AUTHENTICATION_FIELDS) {
            String submittedUsername = submittedCredentials[0];
            String submittedPassword = submittedCredentials[1];
            String username = CoreStubConfig.CORE_STUB_BASIC_AUTH.getUsername();
            String password = CoreStubConfig.CORE_STUB_BASIC_AUTH.getPassword();
            return submittedUsername.equals(username) && submittedPassword.equals(password);
        }
        return false;
    }

    private static String[] extractCredentials(String encodedHeader) {
        String decodedHeader = new String(Base64.getDecoder().decode(encodedHeader));
        return decodedHeader.split(":");
    }
}
