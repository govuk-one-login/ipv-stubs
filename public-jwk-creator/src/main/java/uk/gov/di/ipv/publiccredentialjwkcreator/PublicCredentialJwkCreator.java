package uk.gov.di.ipv.publiccredentialjwkcreator;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinMustache;

import java.security.KeyFactory;
import java.security.interfaces.ECPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class PublicCredentialJwkCreator {
    public PublicCredentialJwkCreator() {
        Javalin.create(config -> {
                    config.fileRenderer(new JavalinMustache());

                    config.routes.get("/", ctx -> ctx.render("templates/index.template", new HashMap<>()));

                    config.routes.post("/", ctx -> {
                        String publickey = ctx.formParam("publickey");
                        String replaced = publickey
                                .replaceAll("\\s", "")
                                .replace("-----BEGINPUBLICKEY-----", "")
                                .replace("-----ENDPUBLICKEY-----", "");

                        KeyFactory kf = KeyFactory.getInstance("EC");
                        byte[] decoded = Base64.getDecoder().decode(replaced);
                        EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
                        ECPublicKey publicKey = (ECPublicKey) kf.generatePublic(keySpec);

                        ECKey ecKey = new ECKey.Builder(Curve.P_256, publicKey).build();
                        Map<String, Object> model = new HashMap<>();
                        model.put("jwk", ecKey.toPublicJWK().toJSONString());

                        ctx.render("templates/index.template", model);
                    });
                })
                .start(8080);
    }
}
