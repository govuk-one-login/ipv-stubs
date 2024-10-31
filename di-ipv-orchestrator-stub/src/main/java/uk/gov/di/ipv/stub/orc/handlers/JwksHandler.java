package uk.gov.di.ipv.stub.orc.handlers;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import io.javalin.http.Context;

import java.util.List;

import static com.nimbusds.jose.jwk.KeyUse.SIGNATURE;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_CLIENT_ID;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_SIGNING_KEY_JWK;

public class JwksHandler {

    public JwksHandler() {}

    public void getResource(Context ctx) throws Exception {
        // Including client ID in kid to differentiate the same key used for different signing functions
        var publicJwk = ECKey.parse(ORCHESTRATOR_SIGNING_KEY_JWK).toPublicJWK();
        ctx.json(
                new JWKSet(
                                List.of(
                                        new ECKey.Builder(publicJwk)
                                                .keyID(
                                                        String.format(
                                                                "%s-%s",
                                                                ORCHESTRATOR_CLIENT_ID,
                                                                publicJwk.computeThumbprint()))
                                                .keyUse(SIGNATURE)
                                                .build()))
                        .toJSONObject(true));
    }
}
