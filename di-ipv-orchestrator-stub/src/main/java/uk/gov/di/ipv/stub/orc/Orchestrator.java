package uk.gov.di.ipv.stub.orc;

import spark.Spark;
import uk.gov.di.ipv.stub.orc.config.OrchestratorConfig;
import uk.gov.di.ipv.stub.orc.handlers.BasicAuthHandler;
import uk.gov.di.ipv.stub.orc.handlers.HomeHandler;
import uk.gov.di.ipv.stub.orc.handlers.IpvHandler;
import uk.gov.di.ipv.stub.orc.utils.EvcsAccessTokenGenerator;

public class Orchestrator {

    private final IpvHandler ipvHandler;

    public Orchestrator() {
        Spark.staticFileLocation("/public");
        Spark.port(Integer.parseInt(OrchestratorConfig.PORT));

        ipvHandler = new IpvHandler(new EvcsAccessTokenGenerator());

        initRoutes();
    }

    public void initRoutes() {
        if (OrchestratorConfig.BASIC_AUTH_ENABLE) {
            BasicAuthHandler basicAuthHandler = new BasicAuthHandler();
            Spark.before(basicAuthHandler.authFilter);
        }
        Spark.get("/", HomeHandler.serveHomePage);
        Spark.get("/authorize", ipvHandler.doAuthorize);
        Spark.get("/authorize-error", ipvHandler.doAuthorizeError);
        Spark.get("/callback", ipvHandler.doCallback);

        Spark.internalServerError(
                "<html><body><h1>Waaargh!!! Da Orc Boss sez we'ze got some gremlinz in da gearz.</h1></body></html>");
    }
}
