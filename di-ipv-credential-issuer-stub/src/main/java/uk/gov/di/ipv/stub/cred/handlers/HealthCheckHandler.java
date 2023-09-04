package uk.gov.di.ipv.stub.cred.handlers;

import org.eclipse.jetty.http.HttpStatus;
import spark.Request;
import spark.Response;
import spark.Route;

public class HealthCheckHandler {

    public Route healthy =
            (Request request, Response response) -> {
                response.status(HttpStatus.OK_200);
                return "{\"OK?\":\"yep\"}";
            };
}
