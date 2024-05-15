package uk.gov.di.ipv.stub.cred.domain;

import spark.QueryParamsMap;

import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.F2F_SEND_ERROR_QUEUE;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.F2F_SEND_VC_QUEUE;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.F2F_STUB_QUEUE_NAME;

public record F2fDetails(boolean sendVcToQueue, boolean sendErrorToQueue, String queueName) {
    private static final String CHECKED = "checked";

    public static F2fDetails fromQueryMap(QueryParamsMap paramsMap) {
        return new F2fDetails(
                CHECKED.equals(paramsMap.value(F2F_SEND_VC_QUEUE)),
                CHECKED.equals(paramsMap.value(F2F_SEND_ERROR_QUEUE)),
                paramsMap.value(F2F_STUB_QUEUE_NAME));
    }
}
