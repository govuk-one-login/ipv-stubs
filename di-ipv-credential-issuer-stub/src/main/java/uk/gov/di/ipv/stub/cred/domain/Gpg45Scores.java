package uk.gov.di.ipv.stub.cred.domain;

import io.javalin.http.Context;

import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.ACTIVITY_HISTORY;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.BIOMETRIC_VERIFICATION;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.FRAUD;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.STRENGTH;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VALIDITY;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VERIFICATION;

public record Gpg45Scores(
        String strength,
        String validity,
        String activityHistory,
        String fraud,
        String verification,
        String biometricVerification) {
    public static Gpg45Scores fromFormContext(Context ctx) {
        return new Gpg45Scores(
                ctx.formParam(STRENGTH),
                ctx.formParam(VALIDITY),
                ctx.formParam(ACTIVITY_HISTORY),
                ctx.formParam(FRAUD),
                ctx.formParam(VERIFICATION),
                ctx.formParam(BIOMETRIC_VERIFICATION));
    }
}
