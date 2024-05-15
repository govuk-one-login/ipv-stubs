package uk.gov.di.ipv.stub.cred.domain;

import spark.QueryParamsMap;

import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.REQUESTED_OAUTH_ERROR;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.REQUESTED_OAUTH_ERROR_DESCRIPTION;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.REQUESTED_OAUTH_ERROR_ENDPOINT;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.REQUESTED_USERINFO_ERROR;

public record RequestedError(
        String error, String description, String endpoint, String userInfoError) {
    public static RequestedError fromQueryMap(QueryParamsMap paramsMap) {
        return new RequestedError(
                paramsMap.value(REQUESTED_OAUTH_ERROR),
                paramsMap.value(REQUESTED_OAUTH_ERROR_DESCRIPTION),
                paramsMap.value(REQUESTED_OAUTH_ERROR_ENDPOINT),
                paramsMap.value(REQUESTED_USERINFO_ERROR));
    }
}
