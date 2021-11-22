package uk.gov.di.ipv.stub.cred;

import spark.Spark;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.handlers.AuthorizeHandler;

public class CredentialIssuer {

    private final AuthorizeHandler authorizeHandler;

    public CredentialIssuer(){
        Spark.staticFileLocation("/public");
        Spark.port(Integer.parseInt(CredentialIssuerConfig.PORT));

        authorizeHandler = new AuthorizeHandler();

        initRoutes();
    }

    public void initRoutes(){
        Spark.get("/authorize", authorizeHandler.doAuthorize);
        Spark.post("/auth-code", authorizeHandler.generateAuthCode);

        Spark.internalServerError("<html><body><h1>Error! Something went wrong!</h1></body></html>");
    }
}
