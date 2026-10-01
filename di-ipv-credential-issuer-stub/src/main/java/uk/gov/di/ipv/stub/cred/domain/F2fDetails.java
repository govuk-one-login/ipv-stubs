package uk.gov.di.ipv.stub.cred.domain;

import io.javalin.http.Context;

import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.F2F_SEND_ERROR_QUEUE;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.F2F_SEND_VC_QUEUE;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.F2F_STUB_QUEUE_NAME;

public record F2fDetails(
        boolean sendVcToQueue, boolean sendErrorToQueue, String queueName, Integer delaySeconds) {
    private static final String CHECKED = "checked";

    public static F2fDetails fromFormContext(Context ctx) {
        return new F2fDetails(
                CHECKED.equals(ctx.formParam(F2F_SEND_VC_QUEUE)),
                CHECKED.equals(ctx.formParam(F2F_SEND_ERROR_QUEUE)),
                ctx.formParam(F2F_STUB_QUEUE_NAME),
                null);
    }
}
