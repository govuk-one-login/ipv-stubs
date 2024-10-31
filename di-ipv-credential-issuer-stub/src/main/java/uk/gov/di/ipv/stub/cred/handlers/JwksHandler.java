package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.jose.jwk.JWKSet;
import io.javalin.http.Context;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;

import java.util.List;

public class JwksHandler {

    public JwksHandler() {}

    public void getResource(Context ctx) throws Exception {
        ctx.json(
                new JWKSet(List.of(CredentialIssuerConfig.getPrivateEncryptionKey().toPublicJWK()))
                        .toJSONObject(true));
    }
}
