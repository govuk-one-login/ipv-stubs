package uk.gov.di.ipv.stub.cred.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.javalin.http.Context;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.CI;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.CLIENT_ID;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.EVIDENCE_JSON_PAYLOAD;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.JSON_PAYLOAD;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.REQUEST;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VC_NOT_BEFORE_DAY;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VC_NOT_BEFORE_FLAG;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VC_NOT_BEFORE_HOURS;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VC_NOT_BEFORE_MINUTES;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VC_NOT_BEFORE_MONTH;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VC_NOT_BEFORE_SECONDS;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VC_NOT_BEFORE_YEAR;
import static uk.gov.di.ipv.stub.cred.utils.StringHelper.splitCommaDelimitedStringValue;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record FormAuthRequest(
        String clientId,
        String request,
        String credentialSubjectJson,
        String evidenceJson,
        Gpg45Scores gpg45Scores,
        List<String> ci,
        Mitigations mitigations,
        F2fDetails f2f,
        RequestedError requestedError,
        Long nbf)
        implements AuthRequest {
    private static final String CHECKED = "checked";
    private static final String ON = "on";
    private static final String UTC = "UTC";

    public static FormAuthRequest fromFormContext(Context ctx) {
        return FormAuthRequest.builder()
                .clientId(ctx.formParam(CLIENT_ID))
                .request(ctx.formParam(REQUEST))
                .credentialSubjectJson(ctx.formParam(JSON_PAYLOAD))
                .evidenceJson(ctx.formParam(EVIDENCE_JSON_PAYLOAD))
                .gpg45Scores(Gpg45Scores.fromFormContext(ctx))
                .ci(splitCommaDelimitedStringValue(ctx.formParam(CI)))
                .mitigations(Mitigations.fromFormContext(ctx))
                .f2f(F2fDetails.fromFormContext(ctx))
                .requestedError(RequestedError.fromFormContext(ctx))
                .nbf(generateNbf(ctx))
                .build();
    }

    private static Long generateNbf(Context ctx) {
        if (ON.equals(ctx.formParam(VC_NOT_BEFORE_FLAG))) {
            try {
                return LocalDateTime.of(
                                Integer.parseInt(ctx.formParam(VC_NOT_BEFORE_YEAR)),
                                Integer.parseInt(ctx.formParam(VC_NOT_BEFORE_MONTH)),
                                Integer.parseInt(ctx.formParam(VC_NOT_BEFORE_DAY)),
                                Integer.parseInt(ctx.formParam(VC_NOT_BEFORE_HOURS)),
                                Integer.parseInt(ctx.formParam(VC_NOT_BEFORE_MINUTES)),
                                Integer.parseInt(ctx.formParam(VC_NOT_BEFORE_SECONDS)))
                        .atZone(ZoneId.of(UTC))
                        .toInstant()
                        .getEpochSecond();
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
