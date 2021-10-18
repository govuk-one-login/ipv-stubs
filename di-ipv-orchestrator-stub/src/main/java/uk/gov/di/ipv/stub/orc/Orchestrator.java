package uk.gov.di.ipv.stub.orc;

import spark.Spark;
import uk.gov.di.ipv.stub.orc.config.OrchestratorConfig;
import uk.gov.di.ipv.stub.orc.handlers.HomeHandler;

public class Orchestrator {

    public Orchestrator(){
        Spark.staticFileLocation("/public");
        Spark.port(Integer.parseInt(OrchestratorConfig.PORT));

        initRoutes();
    }

    public void initRoutes(){
        Spark.get("/", HomeHandler.serveHomePage);

        Spark.internalServerError("<html><body><h1>Waaargh!!! Da Orc Boss sez we'ze got some gremlinz in da gearz.</h1></body></html>");
    }

}
