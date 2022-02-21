package uk.gov.di.ipv.stub.orc;

import spark.Spark;
import uk.gov.di.ipv.stub.orc.config.OrchestratorConfig;
import uk.gov.di.ipv.stub.orc.handlers.HomeHandler;
import uk.gov.di.ipv.stub.orc.handlers.IpvHandler;

public class Orchestrator {

    private final IpvHandler ipvHandler;

    public Orchestrator() {
        Spark.staticFileLocation("/public");
        Spark.port(Integer.parseInt(OrchestratorConfig.PORT));

        ipvHandler = new IpvHandler();

        initRoutes();
    }

    public void initRoutes() {
        Spark.get("/", HomeHandler.serveHomePage);
        Spark.get("/authorize", ipvHandler.doAuthorize);
        Spark.get("/callback", ipvHandler.doCallback);

        Spark.internalServerError(
                "<html><body><h1>Waaargh!!! Da Orc Boss sez we'ze got some gremlinz in da gearz.</h1></body></html>");
    }
}
