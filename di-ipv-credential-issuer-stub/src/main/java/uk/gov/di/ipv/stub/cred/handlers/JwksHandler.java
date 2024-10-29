package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.javalin.http.Context;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;

import java.util.List;

import static com.nimbusds.jose.jwk.KeyUse.ENCRYPTION;

public class JwksHandler {

    public JwksHandler() {}

    public void getResource(Context ctx) throws Exception {
        // Including issuer in thumbprint to differentiate the same key used by different CRI stubs
        RSAKey publicJWK = CredentialIssuerConfig.getPrivateEncryptionKey().toPublicJWK();
        var encPublicKey =
                new RSAKey.Builder(publicJWK)
                        .keyID(
                                String.format(
                                        "%s-%s",
                                        CredentialIssuerConfig.getVerifiableCredentialIssuer(),
                                        publicJWK.computeThumbprint()))
                        .keyUse(ENCRYPTION)
                        .build();
        var jwkSet = new JWKSet(List.of(encPublicKey));

        ctx.json(jwkSet.toJSONObject(true));
    }
}
