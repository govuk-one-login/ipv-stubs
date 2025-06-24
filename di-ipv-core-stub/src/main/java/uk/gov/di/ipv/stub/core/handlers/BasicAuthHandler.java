package uk.gov.di.ipv.stub.core.handlers;

import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Spark;
import uk.gov.di.ipv.stub.core.config.CoreStubConfig;

import java.util.Base64;

public class BasicAuthHandler {
    private static final int NUMBER_OF_AUTHENTICATION_FIELDS = 2;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String AUTHORIZATION_TYPE = "Basic";
    private static final String JWKS_PATH = "/.well-known/jwks.json";

    public BasicAuthHandler() {
        CoreStubConfig.getUserAuth();
    }

    public Filter authFilter =
            (Request request, Response response) -> {
                if (request.pathInfo().equals(JWKS_PATH)) {
                    // basic auth not required on the jwks endpoint
                    return;
                }

                if (!request.headers().contains(AUTHORIZATION_HEADER) || !authenticated(request)) {
                    response.header("WWW-Authenticate", AUTHORIZATION_TYPE);
                    Spark.halt(401, "Not Authenticated");
                }
            };

    private Boolean authenticated(Request request) {
        String authHeader = request.headers(AUTHORIZATION_HEADER);
        int authTypeIndex = authHeader.indexOf(AUTHORIZATION_TYPE);

        if (authHeader == null || authTypeIndex < 0) {
            return false;
        }

        String encodedHeader =
                authHeader.substring(authTypeIndex + AUTHORIZATION_TYPE.length() + 1).trim();
        String[] submittedCredentials = extractCredentials(encodedHeader);

        if (submittedCredentials != null
                && submittedCredentials.length == NUMBER_OF_AUTHENTICATION_FIELDS) {
            String submittedUsername = submittedCredentials[0];
            String submittedPassword = submittedCredentials[1];
            String username = CoreStubConfig.CORE_STUB_BASIC_AUTH.getUsername();
            String password = CoreStubConfig.CORE_STUB_BASIC_AUTH.getPassword();
            return submittedUsername.equals(username) && submittedPassword.equals(password);
        }
        return false;
    }

    private String[] extractCredentials(String encodedHeader) {
        String decodedHeader = new String(Base64.getDecoder().decode(encodedHeader));
        return decodedHeader.split(":");
    }
}
