package uk.gov.di.ipv.stub.cred.domain;

import lombok.Builder;
import spark.QueryParamsMap;

import java.util.List;

import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.CIMIT_STUB_API_KEY;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.CIMIT_STUB_URL;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.MITIGATED_CI;
import static uk.gov.di.ipv.stub.cred.utils.StringHelper.splitCommaDelimitedStringValue;

@Builder
public record Mitigations(List<String> mitigatedCi, String cimitStubUrl, String cimitStubApiKey) {
    public static Mitigations fromQueryMap(QueryParamsMap paramsMap) {
        return Mitigations.builder()
                .mitigatedCi(splitCommaDelimitedStringValue(paramsMap.value(MITIGATED_CI)))
                .cimitStubUrl(paramsMap.value(CIMIT_STUB_URL))
                .cimitStubApiKey(paramsMap.value(CIMIT_STUB_API_KEY))
                .build();
    }
}
