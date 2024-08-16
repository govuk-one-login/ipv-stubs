package uk.gov.di.ipv.stub.cred.handlers;

import io.javalin.http.Context;

import java.util.Map;

public class HealthCheckHandler {
    public void healthy(Context ctx) {
        ctx.json(Map.of("healthy", true));
    }
}
