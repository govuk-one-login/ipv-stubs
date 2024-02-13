package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.cred.service.ConfigService;

import java.util.List;

public class JwksHandler {

    private static final String RESPONSE_TYPE = "application/json;charset=UTF-8";

    public JwksHandler() {}

    public Route getResource =
            (Request request, Response response) -> {
                var docAppClientConfig = ConfigService.getClientConfig("orch-build");
                var signingJWK = JWK.parse(docAppClientConfig.getSigningPublicJwk());
                var encryptionJWK = docAppClientConfig.getEncryptionPublicKeyJwk();
                var jwkSet = new JWKSet(List.of(signingJWK, encryptionJWK));

                response.type(RESPONSE_TYPE);
                return jwkSet.toString(true);
            };
}
