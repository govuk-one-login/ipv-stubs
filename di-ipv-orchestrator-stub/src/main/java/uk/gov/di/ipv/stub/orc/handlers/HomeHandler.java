package uk.gov.di.ipv.stub.orc.handlers;

import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.orc.utils.ViewHelper;

import java.util.HashMap;

public class HomeHandler {
    public static Route serveHomePage =
            (Request request, Response response) -> {
                var modelMap = new HashMap<String, Object>();
                modelMap.put("welcome", "Hello world");

                return ViewHelper.render(modelMap, "home.mustache");
            };
}
