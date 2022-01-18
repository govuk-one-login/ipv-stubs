package uk.gov.di.ipv.stub.cred;

import com.fasterxml.jackson.databind.ObjectMapper;
import spark.Spark;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.handlers.AuthorizeHandler;
import uk.gov.di.ipv.stub.cred.handlers.CredentialHandler;
import uk.gov.di.ipv.stub.cred.handlers.TokenHandler;
import uk.gov.di.ipv.stub.cred.service.AuthCodeService;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.service.TokenService;
import uk.gov.di.ipv.stub.cred.utils.ViewHelper;

public class CredentialIssuer {

    private final AuthorizeHandler authorizeHandler;
    private final TokenHandler tokenHandler;
    private final CredentialHandler credentialHandler;

    public CredentialIssuer(){
        Spark.staticFileLocation("/public");
        Spark.port(Integer.parseInt(CredentialIssuerConfig.PORT));

        ObjectMapper objectMapper = new ObjectMapper();

        AuthCodeService authCodeService = new AuthCodeService();
        TokenService tokenService = new TokenService();
        CredentialService credentialService = new CredentialService();

        authorizeHandler = new AuthorizeHandler(new ViewHelper(), authCodeService, credentialService);
        tokenHandler = new TokenHandler(authCodeService, tokenService);
        credentialHandler = new CredentialHandler(credentialService, tokenService, objectMapper);

        initRoutes();
        initErrorMapping();
    }

    private void initRoutes(){
        Spark.get("/authorize", authorizeHandler.doAuthorize);
        Spark.post("/authorize", authorizeHandler.generateResponse);
        Spark.post("/token", tokenHandler.issueAccessToken);
        Spark.get("/credential", credentialHandler.getResource);
    }

    private void initErrorMapping() {
        Spark.internalServerError("<html><body><h1>Error! Something went wrong!</h1></body></html>");
    }
}
