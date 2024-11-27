package uk.gov.di.ipv.stub.core;

import com.nimbusds.jose.jwk.ECKey;
import io.javalin.Javalin;
import io.javalin.http.ExceptionHandler;
import io.javalin.rendering.template.JavalinMustache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.stub.core.config.CoreStubConfig;
import uk.gov.di.ipv.stub.core.handlers.BasicAuthHandler;
import uk.gov.di.ipv.stub.core.handlers.CoreStubHandler;
import uk.gov.di.ipv.stub.core.utils.HandlerHelper;

import java.text.ParseException;
import java.util.Base64;
import java.util.Map;

public class CoreStub {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreStub.class);

    public CoreStub() throws Exception {
        CoreStubConfig.initUATUsers();
        CoreStubConfig.initCRIS();

        var app =
                Javalin.create(
                        config -> {
                            config.staticFiles.add(
                                    staticFiles -> staticFiles.directory = "/public");
                            config.fileRenderer(new JavalinMustache());
                        });

        initRoutes(app);

        app.start(Integer.parseInt(CoreStubConfig.CORE_STUB_PORT));
    }

    private void initRoutes(Javalin app) throws Exception {
        CoreStubHandler coreStubHandler = new CoreStubHandler(new HandlerHelper(getEcPrivateKey()));
        if (CoreStubConfig.ENABLE_BASIC_AUTH) {
            BasicAuthHandler basicAuthHandler = new BasicAuthHandler();
            app.before(basicAuthHandler.authFilter);
        }
        app.get("/", coreStubHandler.serveHomePage);
        app.get("/credential-issuers", coreStubHandler.showCredentialIssuer);
        app.get("/credential-issuer", coreStubHandler.handleCredentialIssuerRequest);
        app.get("/edit-postcode", coreStubHandler.editPostcode);
        app.get("/evidence-request", coreStubHandler.evidenceRequest);
        app.get("/authorize", coreStubHandler.authorize);
        app.get("/user-search", coreStubHandler.userSearch);
        app.post("/user-search", coreStubHandler.sendRawSharedClaim);
        app.get("/edit-user", coreStubHandler.editUser);
        app.post("/edit-user", coreStubHandler.updateUser);
        app.get("/callback", coreStubHandler.doCallback);
        app.get("/answers", coreStubHandler.answers);
        setupBackendRoutes(app, coreStubHandler);
        app.exception(Exception.class, exceptionHandler());
    }

    private void setupBackendRoutes(Javalin app, CoreStubHandler coreStubHandler) {
        if (!CoreStubConfig.CORE_STUB_ENABLE_BACKEND_ROUTES) {
            LOGGER.info("BackendRoutes Disabled.");
            return;
        }

        LOGGER.warn("BackendRoutes Enabled.");

        app.get(
                "/backend/generateInitialClaimsSet",
                coreStubHandler.backendGenerateInitialClaimsSet);
        app.post("/backend/createSessionRequest", coreStubHandler.createBackendSessionRequest);
        app.get(
                "/backend/createTokenRequestPrivateKeyJWT",
                coreStubHandler.createTokenRequestPrivateKeyJWT);
        app.get(
                "/backend/generateInitialClaimsSetPostCode",
                coreStubHandler.backendGenerateInitialClaimsSetPostCode);
    }

    private ExceptionHandler<Exception> exceptionHandler() {
        return (e, ctx) -> {
            LOGGER.error(e.getMessage(), e);
            ctx.status(500);
            ctx.render("error.mustache", Map.of("error", e.getMessage()));
        };
    }

    private ECKey getEcPrivateKey() throws ParseException {
        return ECKey.parse(
                new String(
                        Base64.getDecoder()
                                .decode(CoreStubConfig.CORE_STUB_SIGNING_PRIVATE_KEY_JWK_BASE64)));
    }
}
