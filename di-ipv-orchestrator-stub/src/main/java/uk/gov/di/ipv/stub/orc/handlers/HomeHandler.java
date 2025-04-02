package uk.gov.di.ipv.stub.orc.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HomeHandler {
    private static final String NON_APP_JOURNEY_USER_ID_PREFIX = "urn:uuid:";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void serveHomePage(Context ctx) throws IOException {
        Map<String, Object> moustacheDataModel = new HashMap<>();

        String journeyId = UUID.randomUUID().toString();
        String userId = NON_APP_JOURNEY_USER_ID_PREFIX + UUID.randomUUID();

        // Dev stub deployments have to be associated to users (due to the way dev-deploy works). This means we must
        // use the main orch stub. Selenium tests with the @Build tag use the "default" environment option - but since
        // we are on the main orch stub, it defaults as build. We use the orch stub url defaultEnvironment query
        // parameter to override the default, since we set this url for each environment's tests config.
        String defaultEnvironment = ctx.queryParam("defaultEnvironment");
        if (defaultEnvironment == null || defaultEnvironment.isBlank()) {
            defaultEnvironment = "DEFAULT";
        }

        moustacheDataModel.put("signInJourneyId", journeyId);
        moustacheDataModel.put("uuid", userId);
        moustacheDataModel.put("defaultEnvironment", defaultEnvironment);
        moustacheDataModel.put(
                "credentialSubjects", getData("/data/inheritedJWTCredentialSubjects.json"));
        moustacheDataModel.put("evidences", getData("/data/inheritedJWTEvidences.json"));
        ctx.render("templates/home.mustache", moustacheDataModel);
    }

    private static Object getData(String jsonPath) throws IOException {
        try (InputStream inputStream = HomeHandler.class.getResourceAsStream(jsonPath)) {
            JsonNode jsonNode = objectMapper.readTree(inputStream);
            return jsonNode.get("data");
        }
    }
}
