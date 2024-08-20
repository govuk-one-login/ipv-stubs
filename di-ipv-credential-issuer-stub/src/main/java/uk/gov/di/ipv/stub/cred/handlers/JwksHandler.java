package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import io.javalin.http.Context;
import uk.gov.di.ipv.stub.cred.service.ConfigService;

import java.util.List;

public class JwksHandler {

    public JwksHandler() {}

    public void getResource(Context ctx) throws Exception {
        var docAppClientConfig = ConfigService.getClientConfig("orch-build");
        var signingJWK = JWK.parse(docAppClientConfig.getSigningPublicJwk());
        var encryptionJWK = docAppClientConfig.getEncryptionPublicKeyJwk();
        var jwkSet = new JWKSet(List.of(signingJWK, encryptionJWK));

        ctx.json(jwkSet.toJSONObject(true));
    }
}
