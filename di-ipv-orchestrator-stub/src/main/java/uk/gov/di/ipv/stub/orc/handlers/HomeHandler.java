package uk.gov.di.ipv.stub.orc.handlers;

import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.orc.utils.ViewHelper;

public class HomeHandler {
    public static final String APP_JOURNEY_USER_ID_PREFIX = "urn:uuid:app-journey-user-";
    public static final String NON_APP_JOURNEY_USER_ID_PREFIX = "urn:uuid:";
    public static Route serveHomePage =
            (Request request, Response response) -> {
                return ViewHelper.render(null, "home.mustache");
            };
}
