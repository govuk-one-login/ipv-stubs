package uk.gov.di.ipv.stub.cred.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import spark.QueryParamsMap;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.CI;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.CLIENT_ID;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.EVIDENCE_JSON_PAYLOAD;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.JSON_PAYLOAD;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.REQUEST;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.RESOURCE_ID;
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
        String resourceId,
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

    public static FormAuthRequest fromQueryMap(QueryParamsMap paramsMap) {
        return FormAuthRequest.builder()
                .clientId(paramsMap.value(CLIENT_ID))
                .request(paramsMap.value(REQUEST))
                .credentialSubjectJson(paramsMap.value(JSON_PAYLOAD))
                .evidenceJson(paramsMap.value(EVIDENCE_JSON_PAYLOAD))
                .gpg45Scores(Gpg45Scores.fromQueryMap(paramsMap))
                .ci(splitCommaDelimitedStringValue(paramsMap.value(CI)))
                .mitigations(Mitigations.fromQueryMap(paramsMap))
                .f2f(F2fDetails.fromQueryMap(paramsMap))
                .requestedError(RequestedError.fromQueryMap(paramsMap))
                .nbf(generateNbf(paramsMap))
                .resourceId(paramsMap.value(RESOURCE_ID))
                .build();
    }

    private static Long generateNbf(QueryParamsMap paramsMap) {
        if (ON.equals(paramsMap.value(VC_NOT_BEFORE_FLAG))) {
            try {
                return LocalDateTime.of(
                                Integer.parseInt(paramsMap.value(VC_NOT_BEFORE_YEAR)),
                                Integer.parseInt(paramsMap.value(VC_NOT_BEFORE_MONTH)),
                                Integer.parseInt(paramsMap.value(VC_NOT_BEFORE_DAY)),
                                Integer.parseInt(paramsMap.value(VC_NOT_BEFORE_HOURS)),
                                Integer.parseInt(paramsMap.value(VC_NOT_BEFORE_MINUTES)),
                                Integer.parseInt(paramsMap.value(VC_NOT_BEFORE_SECONDS)))
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
