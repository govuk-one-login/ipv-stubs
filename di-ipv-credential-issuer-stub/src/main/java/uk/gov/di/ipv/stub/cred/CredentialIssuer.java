package uk.gov.di.ipv.stub.cred;

import spark.Spark;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.handlers.AuthorizeHandler;
import uk.gov.di.ipv.stub.cred.handlers.ResourceHandler;
import uk.gov.di.ipv.stub.cred.handlers.TokenHandler;
import uk.gov.di.ipv.stub.cred.service.AuthCodeService;
import uk.gov.di.ipv.stub.cred.service.ProtectedResourceService;
import uk.gov.di.ipv.stub.cred.service.TokenService;
import uk.gov.di.ipv.stub.cred.utils.ViewHelper;

public class CredentialIssuer {

    private final AuthorizeHandler authorizeHandler;
    private final TokenHandler tokenHandler;
    private final ResourceHandler resourceHandler;

    public CredentialIssuer(){
        Spark.staticFileLocation("/public");
        Spark.port(Integer.parseInt(CredentialIssuerConfig.PORT));

        AuthCodeService authCodeService = new AuthCodeService();
        TokenService tokenService = new TokenService();
        ProtectedResourceService protectedResourceService = new ProtectedResourceService();

        authorizeHandler = new AuthorizeHandler(new ViewHelper(), authCodeService);
        tokenHandler = new TokenHandler(authCodeService, tokenService);
        resourceHandler = new ResourceHandler(protectedResourceService, tokenService);

        initRoutes();
        initErrorMapping();
    }

    private void initRoutes(){
        Spark.get("/authorize", authorizeHandler.doAuthorize);
        Spark.post("/authorize", authorizeHandler.generateAuthCode);
        Spark.post("/token", tokenHandler.issueAccessToken);
        Spark.get("/credential", resourceHandler.getResource);
    }

    private void initErrorMapping() {
        Spark.internalServerError("<html><body><h1>Error! Something went wrong!</h1></body></html>");
    }
}
