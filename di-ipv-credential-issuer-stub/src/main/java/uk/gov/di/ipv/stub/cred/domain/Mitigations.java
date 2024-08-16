package uk.gov.di.ipv.stub.cred.domain;

import io.javalin.http.Context;
import lombok.Builder;

import java.util.List;

import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.CIMIT_STUB_API_KEY;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.CIMIT_STUB_URL;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.MITIGATED_CI;
import static uk.gov.di.ipv.stub.cred.utils.StringHelper.splitCommaDelimitedStringValue;

@Builder
public record Mitigations(List<String> mitigatedCi, String cimitStubUrl, String cimitStubApiKey) {
    public static Mitigations fromFormContext(Context ctx) {
        return Mitigations.builder()
                .mitigatedCi(splitCommaDelimitedStringValue(ctx.formParam(MITIGATED_CI)))
                .cimitStubUrl(ctx.formParam(CIMIT_STUB_URL))
                .cimitStubApiKey(ctx.formParam(CIMIT_STUB_API_KEY))
                .build();
    }
}
