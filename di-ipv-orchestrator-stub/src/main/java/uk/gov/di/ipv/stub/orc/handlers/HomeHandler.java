package uk.gov.di.ipv.stub.orc.handlers;

import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.orc.utils.ViewHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HomeHandler {
    public static final String APP_JOURNEY_USER_ID_PREFIX = "urn:uuid:app-journey-user-";
    public static final String NON_APP_JOURNEY_USER_ID_PREFIX = "urn:uuid:";
    public static Route serveHomePage =
            (Request request, Response response) -> {
                Map<String, Object> moustacheDataModel = new HashMap<>();
                String uuid = NON_APP_JOURNEY_USER_ID_PREFIX + UUID.randomUUID();
                moustacheDataModel.put("uuid", uuid);
                return ViewHelper.render(moustacheDataModel, "home.mustache");
            };
}
