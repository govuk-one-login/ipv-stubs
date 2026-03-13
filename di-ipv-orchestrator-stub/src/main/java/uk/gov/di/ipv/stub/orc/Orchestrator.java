package uk.gov.di.ipv.stub.orc;

import io.javalin.Javalin;
import io.javalin.config.RoutesConfig;
import io.javalin.rendering.template.JavalinMustache;
import uk.gov.di.ipv.stub.orc.config.OrchestratorConfig;
import uk.gov.di.ipv.stub.orc.handlers.BasicAuthHandler;
import uk.gov.di.ipv.stub.orc.handlers.HomeHandler;
import uk.gov.di.ipv.stub.orc.handlers.IpvHandler;
import uk.gov.di.ipv.stub.orc.handlers.JwksHandler;
import uk.gov.di.ipv.stub.orc.utils.EvcsAccessTokenGenerator;

public class Orchestrator {

    private final IpvHandler ipvHandler;
    private final JwksHandler jwksHandler;

    public Orchestrator() {
        ipvHandler = new IpvHandler(new EvcsAccessTokenGenerator());
        jwksHandler = new JwksHandler();

        var app =
                Javalin.create(
                        config -> {
                            config.startup.showJavalinBanner = false;
                            config.staticFiles.add("/public");
                            config.fileRenderer(new JavalinMustache());

                            initRoutes(config.routes);
                        });
        app.start(Integer.parseInt(OrchestratorConfig.PORT));
    }

    public void initRoutes(RoutesConfig routesConfig) {
        if (OrchestratorConfig.BASIC_AUTH_ENABLE) {
            BasicAuthHandler basicAuthHandler = new BasicAuthHandler();
            routesConfig.before(basicAuthHandler::authFilter);
        }
        routesConfig.get("/", HomeHandler::serveHomePage);
        routesConfig.get("/authorize", ipvHandler::doAuthorize);
        routesConfig.get("/authorize-error", ipvHandler::doAuthorizeError);
        routesConfig.get("/callback", ipvHandler::doCallback);
        routesConfig.get("/.well-known/jwks.json", jwksHandler::getResource);

        routesConfig.error(
                500,
                ctx ->
                        ctx.html(
                                "<html><body><h1>Waaargh!!! Da Orc Boss sez we'ze got some gremlinz in da gearz.</h1></body></html>"));
    }
}
