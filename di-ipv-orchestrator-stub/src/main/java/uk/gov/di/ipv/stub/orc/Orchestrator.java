package uk.gov.di.ipv.stub.orc;

import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinMustache;
import uk.gov.di.ipv.stub.orc.config.OrchestratorConfig;
import uk.gov.di.ipv.stub.orc.handlers.BasicAuthHandler;
import uk.gov.di.ipv.stub.orc.handlers.HomeHandler;
import uk.gov.di.ipv.stub.orc.handlers.IpvHandler;
import uk.gov.di.ipv.stub.orc.utils.EvcsAccessTokenGenerator;

public class Orchestrator {

    private final IpvHandler ipvHandler;

    public Orchestrator() {
        ipvHandler = new IpvHandler(new EvcsAccessTokenGenerator());

        var app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.staticFiles.add("/public");
            config.fileRenderer(new JavalinMustache());
        });
        initRoutes(app);
        app.start(Integer.parseInt(OrchestratorConfig.PORT));
    }

    public void initRoutes(Javalin app) {
        if (OrchestratorConfig.BASIC_AUTH_ENABLE) {
            BasicAuthHandler basicAuthHandler = new BasicAuthHandler();
            app.before(basicAuthHandler::authFilter);
        }
        app.get("/", HomeHandler::serveHomePage);
        app.get("/authorize", ipvHandler::doAuthorize);
        app.get("/authorize-error", ipvHandler::doAuthorizeError);
        app.get("/callback", ipvHandler::doCallback);

        app.error(500,
                ctx -> ctx.html("<html><body><h1>Waaargh!!! Da Orc Boss sez we'ze got some gremlinz in da gearz.</h1></body></html>"));
    }
}
