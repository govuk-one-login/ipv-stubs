package uk.gov.di.ipv.stub.cred;

import spark.Spark;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.handlers.AuthorizeHandler;
import uk.gov.di.ipv.stub.cred.handlers.ResourceHandler;
import uk.gov.di.ipv.stub.cred.service.ProtectedResourceService;
import uk.gov.di.ipv.stub.cred.utils.ViewHelper;

public class CredentialIssuer {

    private final AuthorizeHandler authorizeHandler;
    private final ResourceHandler resourceHandler;

    public CredentialIssuer(){
        Spark.staticFileLocation("/public");
        Spark.port(Integer.parseInt(CredentialIssuerConfig.PORT));

        authorizeHandler = new AuthorizeHandler(new ViewHelper());
        resourceHandler = new ResourceHandler(new ProtectedResourceService());

        initRoutes();
    }

    public void initRoutes(){
        Spark.get("/authorize", authorizeHandler.doAuthorize);
        Spark.post("/authorize", authorizeHandler.generateAuthCode);

        Spark.get("/resource", resourceHandler.getResource);

        Spark.internalServerError("<html><body><h1>Error! Something went wrong!</h1></body></html>");
    }
}
