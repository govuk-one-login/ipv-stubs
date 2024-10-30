package uk.gov.di.ipv.stub.orc.handlers;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import io.javalin.http.Context;
import uk.gov.di.ipv.stub.orc.utils.KeyIdGenerator;

import java.util.List;

import static com.nimbusds.jose.jwk.KeyUse.SIGNATURE;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_CLIENT_ID;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_SIGNING_JWK;

public class JwksHandler {

    public JwksHandler() {}

    public void getResource(Context ctx) throws Exception {

        ctx.json(
                new JWKSet(
                                List.of(
                                        new ECKey.Builder(
                                                        ECKey.parse(ORCHESTRATOR_SIGNING_JWK)
                                                                .toPublicJWK())
                                                .keyID(
                                                        KeyIdGenerator.generate(
                                                                ORCHESTRATOR_SIGNING_JWK,
                                                                ORCHESTRATOR_CLIENT_ID))
                                                .keyUse(SIGNATURE)
                                                .build()))
                        .toJSONObject(true));
    }
}
