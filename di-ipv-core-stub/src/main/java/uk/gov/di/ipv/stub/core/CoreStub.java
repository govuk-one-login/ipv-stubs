package uk.gov.di.ipv.stub.core;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ExceptionHandler;
import spark.Spark;
import uk.gov.di.ipv.stub.core.config.CoreStubConfig;
import uk.gov.di.ipv.stub.core.handlers.CoreStubHandler;
import uk.gov.di.ipv.stub.core.utils.HandlerHelper;
import uk.gov.di.ipv.stub.core.utils.ViewHelper;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.text.ParseException;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

public class CoreStub {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreStub.class);

    public CoreStub() throws Exception {
        CoreStubConfig.initUATUsers();
        CoreStubConfig.initCRIS();
        Spark.staticFileLocation("/public");
        Spark.port(Integer.parseInt(CoreStubConfig.CORE_STUB_PORT));
        initRoutes();
    }

    private void initRoutes() throws Exception {
        CoreStubHandler coreStubHandler =
                new CoreStubHandler(new HandlerHelper(getSigningKeystore(), getEcPrivateKey()));
        Spark.get("/", coreStubHandler.serveHomePage);
        Spark.get("/credential-issuers", coreStubHandler.showCredentialIssuer);
        Spark.get("/credential-issuer", coreStubHandler.handleCredentialIssuerRequest);
        Spark.get("/authorize", coreStubHandler.authorize);
        Spark.get("/user-search", coreStubHandler.userSearch);
        Spark.get("/callback", coreStubHandler.doCallback);
        Spark.exception(Exception.class, exceptionHandler());
    }

    private ExceptionHandler exceptionHandler() {
        return (e, req, res) -> {
            LOGGER.error(e.getMessage(), e);
            res.status(500);
            res.body(ViewHelper.render(Map.of("error", e.getMessage()), "error.mustache"));
        };
    }

    private RSAKey getSigningKeystore() throws Exception {
        KeyStore keystore = KeyStore.getInstance("pkcs12");
        final char[] keyStorePassword = CoreStubConfig.CORE_STUB_KEYSTORE_PASSWORD.toCharArray();
        try (ByteArrayInputStream inputStream =
                new ByteArrayInputStream(
                        Base64.getDecoder().decode(CoreStubConfig.CORE_STUB_KEYSTORE_BASE64))) {
            keystore.load(inputStream, keyStorePassword);
        }
        return Objects.requireNonNull(
                RSAKey.load(keystore, CoreStubConfig.CORE_STUB_KEYSTORE_ALIAS, keyStorePassword));
    }

    private ECKey getEcPrivateKey() throws ParseException {
        return ECKey.parse(
                new String(
                        Base64.getDecoder()
                                .decode(CoreStubConfig.CORE_STUB_SIGNING_PRIVATE_KEY_JWK_BASE64)));
    }
}
