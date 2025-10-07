package uk.gov.di.ipv.stub.cred.domain;

import io.javalin.http.Context;

import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.REQUESTED_API_ERROR;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.REQUESTED_OAUTH_ERROR;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.REQUESTED_OAUTH_ERROR_DESCRIPTION;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.REQUESTED_OAUTH_ERROR_ENDPOINT;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.REQUESTED_USERINFO_ERROR;

public record RequestedError(
        String error, String description, String endpoint, String userInfoError, String apiError) {
    public static RequestedError fromFormContext(Context ctx) {
        return new RequestedError(
                ctx.formParam(REQUESTED_OAUTH_ERROR),
                ctx.formParam(REQUESTED_OAUTH_ERROR_DESCRIPTION),
                ctx.formParam(REQUESTED_OAUTH_ERROR_ENDPOINT),
                ctx.formParam(REQUESTED_USERINFO_ERROR),
                ctx.formParam(REQUESTED_API_ERROR));
    }
}
