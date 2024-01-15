package uk.gov.di.ipv.stub.orc.handlers;

import com.nimbusds.jose.shaded.json.JSONObject;
import com.nimbusds.jose.shaded.json.parser.JSONParser;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.orc.utils.ViewHelper;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.nimbusds.jose.shaded.json.parser.JSONParser.MODE_JSON_SIMPLE;

public class HomeHandler {
    public static final String NON_APP_JOURNEY_USER_ID_PREFIX = "urn:uuid:";
    public static Route serveHomePage =
            (Request request, Response response) -> {
                Map<String, Object> moustacheDataModel = new HashMap<>();

                String journeyId = UUID.randomUUID().toString();
                String userId = NON_APP_JOURNEY_USER_ID_PREFIX + UUID.randomUUID();

                moustacheDataModel.put("signInJourneyId", journeyId);
                moustacheDataModel.put("uuid", userId);
                moustacheDataModel.put(
                        "credentialSubjects", getData("/data/inheritedJWTCredentialSubjects.json"));
                moustacheDataModel.put("evidences", getData("/data/inheritedJWTEvidences.json"));
                return ViewHelper.render(moustacheDataModel, "home.mustache");
            };

    private static Object getData(String jsonPath)
            throws UnsupportedEncodingException,
                    com.nimbusds.jose.shaded.json.parser.ParseException {
        JSONObject js =
                (JSONObject)
                        new JSONParser(MODE_JSON_SIMPLE)
                                .parse(HomeHandler.class.getResourceAsStream(jsonPath));

        return js.get("data");
    }
}
