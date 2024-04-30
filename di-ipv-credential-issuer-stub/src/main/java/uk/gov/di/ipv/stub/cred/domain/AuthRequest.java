package uk.gov.di.ipv.stub.cred.domain;

import lombok.Builder;
import lombok.Getter;
import spark.QueryParamsMap;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.ACTIVITY_HISTORY;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.BIOMETRIC_VERIFICATION;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.CI;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.CIMIT_STUB_API_KEY;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.CIMIT_STUB_URL;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.CLIENT_ID;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.EVIDENCE_JSON_PAYLOAD;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.F2F_SEND_ERROR_QUEUE;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.F2F_SEND_VC_QUEUE;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.F2F_STUB_QUEUE_NAME;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.FRAUD;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.JSON_PAYLOAD;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.MITIGATED_CIS;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.REQUEST;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.REQUESTED_OAUTH_ERROR;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.REQUESTED_OAUTH_ERROR_DESCRIPTION;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.REQUESTED_OAUTH_ERROR_ENDPOINT;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.REQUESTED_USERINFO_ERROR;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.RESOURCE_ID;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.STRENGTH;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VALIDITY;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VC_NOT_BEFORE_DAY;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VC_NOT_BEFORE_FLAG;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VC_NOT_BEFORE_HOURS;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VC_NOT_BEFORE_MINUTES;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VC_NOT_BEFORE_MONTH;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VC_NOT_BEFORE_SECONDS;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VC_NOT_BEFORE_YEAR;
import static uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants.VERIFICATION;

@Builder
@Getter
public class AuthRequest {

    private String clientId;
    private String jar; // REQUEST

    private String credentialSubject; // JSON_PAYLOAD

    private String evidenceScore;
    private String validityScore;
    private String activityScore;
    private String fraudScore;
    private String verificationScore;
    private String biometricVerificationScore;
    private String evidencePayload;

    private List<String> cis;

    private List<String> mitigatedCis;
    private String cimitStubUrl;
    private String cimitStubApikey;

    private boolean sendF2fVcToQueue;
    private boolean sendF2fErrorToQueue;
    private String f2fQueueName;

    private boolean nbfFlag;
    private Long nbf;

    private String resourceId;

    private String requestedErrorEndpoint;
    private String requestedError;
    private String requestedErrorDescription;
    private String requestedUserInfoError;

    public static AuthRequest fromQueryMap(QueryParamsMap paramsMap) {
        return AuthRequest.builder()
                .clientId(paramsMap.value(CLIENT_ID))
                .jar(paramsMap.value(REQUEST))
                .credentialSubject(paramsMap.value(JSON_PAYLOAD))
                .evidenceScore(paramsMap.value(STRENGTH))
                .validityScore(paramsMap.value(VALIDITY))
                .activityScore(paramsMap.value(ACTIVITY_HISTORY))
                .fraudScore(paramsMap.value(FRAUD))
                .verificationScore(paramsMap.value(VERIFICATION))
                .biometricVerificationScore(paramsMap.value(BIOMETRIC_VERIFICATION))
                .evidencePayload(paramsMap.value(EVIDENCE_JSON_PAYLOAD))
                .cis(splitCommaDelimitedStringValue(paramsMap.value(CI)))
                .mitigatedCis(splitCommaDelimitedStringValue(paramsMap.value(MITIGATED_CIS)))
                .cimitStubUrl(paramsMap.value(CIMIT_STUB_URL))
                .cimitStubApikey(paramsMap.value(CIMIT_STUB_API_KEY))
                .sendF2fVcToQueue("checked".equals(paramsMap.value(F2F_SEND_VC_QUEUE)))
                .sendF2fErrorToQueue("checked".equals(paramsMap.value(F2F_SEND_ERROR_QUEUE)))
                .f2fQueueName(paramsMap.value(F2F_STUB_QUEUE_NAME))
                .nbfFlag("on".equals(paramsMap.value(VC_NOT_BEFORE_FLAG)))
                .nbf(getNbf(paramsMap))
                .resourceId(paramsMap.value(RESOURCE_ID))
                .requestedErrorEndpoint(paramsMap.value(REQUESTED_OAUTH_ERROR_ENDPOINT))
                .requestedError(paramsMap.value(REQUESTED_OAUTH_ERROR))
                .requestedErrorDescription(paramsMap.value(REQUESTED_OAUTH_ERROR_DESCRIPTION))
                .requestedUserInfoError(paramsMap.value(REQUESTED_USERINFO_ERROR))
                .build();
    }

    private static Long getNbf(QueryParamsMap paramsMap) {
        try {
            return LocalDateTime.of(
                            Integer.parseInt(paramsMap.value(VC_NOT_BEFORE_YEAR)),
                            Integer.parseInt(paramsMap.value(VC_NOT_BEFORE_MONTH)),
                            Integer.parseInt(paramsMap.value(VC_NOT_BEFORE_DAY)),
                            Integer.parseInt(paramsMap.value(VC_NOT_BEFORE_HOURS)),
                            Integer.parseInt(paramsMap.value(VC_NOT_BEFORE_MINUTES)),
                            Integer.parseInt(paramsMap.value(VC_NOT_BEFORE_SECONDS)))
                    .atZone(ZoneId.of("UTC"))
                    .toInstant()
                    .getEpochSecond();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<String> splitCommaDelimitedStringValue(String toSplit) {
        return Stream.ofNullable(toSplit)
                .flatMap(cisString -> Arrays.stream(cisString.split(",", -1)))
                .map(String::strip)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
