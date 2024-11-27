package uk.gov.di.ipv.stub.orc.handlers;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import io.javalin.http.Context;

import java.util.List;

import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.AUTH_SIGNING_JWK;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_SIGNING_JWK;

public class JwksHandler {

    public JwksHandler() {}

    public void getResource(Context ctx) throws Exception {

        ctx.json(
                new JWKSet(
                                List.of(
                                        ECKey.parse(ORCHESTRATOR_SIGNING_JWK).toPublicJWK(),
                                        ECKey.parse(AUTH_SIGNING_JWK).toPublicJWK()))
                        .toJSONObject(true));
    }
}
