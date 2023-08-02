import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;

import java.security.KeyFactory;
import java.security.interfaces.ECPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

public class Main {
    public static void main(String[] args) {
        port(8080);
        get(
                "/",
                (req, res) -> {
                    Map<String, Object> model = new HashMap<>();
                    return new MustacheTemplateEngine()
                            .render(new ModelAndView(model, "index.template"));
                });

        post(
                "/",
                (req, res) -> {
                    String publickey = req.queryParams("publickey");
                    String replaced =
                            publickey
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

                    return new MustacheTemplateEngine()
                            .render(new ModelAndView(model, "index.template"));
                });
    }
}
