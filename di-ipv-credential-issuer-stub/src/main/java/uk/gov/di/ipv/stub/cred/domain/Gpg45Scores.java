package uk.gov.di.ipv.stub.cred.domain;

import spark.QueryParamsMap;

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
    public static Gpg45Scores fromQueryMap(QueryParamsMap paramsMap) {
        return new Gpg45Scores(
                paramsMap.value(STRENGTH),
                paramsMap.value(VALIDITY),
                paramsMap.value(ACTIVITY_HISTORY),
                paramsMap.value(FRAUD),
                paramsMap.value(VERIFICATION),
                paramsMap.value(BIOMETRIC_VERIFICATION));
    }
}
