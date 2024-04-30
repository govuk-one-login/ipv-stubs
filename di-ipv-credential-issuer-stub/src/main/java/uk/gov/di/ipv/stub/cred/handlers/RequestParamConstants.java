package uk.gov.di.ipv.stub.cred.handlers;

public class RequestParamConstants {
    public static final String AUDIENCE = "aud";
    public static final String AUTH_CODE = "code";
    public static final String CLIENT_ASSERTION = "client_assertion";
    public static final String CLIENT_ASSERTION_TYPE = "client_assertion_type";
    public static final String CLIENT_ID = "client_id";
    public static final String EVIDENCE_JSON_PAYLOAD = "evidenceJsonPayload";
    public static final String GRANT_TYPE = "grant_type";
    public static final String ISSUER = "iss";
    public static final String JSON_PAYLOAD = "jsonPayload";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String REQUEST = "request";
    public static final String RESOURCE_ID = "resourceId";
    public static final String RESPONSE_TYPE = "response_type";
    public static final String STATE = "state";

    public static final String STRENGTH = "strengthScore";
    public static final String VALIDITY = "validityScore";
    public static final String ACTIVITY_HISTORY = "activityHistoryScore";
    public static final String FRAUD = "identityFraudScore";
    public static final String VERIFICATION = "verificationScore";
    public static final String BIOMETRIC_VERIFICATION = "biometricVerificationScore";

    public static final String REQUESTED_OAUTH_ERROR = "requested_oauth_error";
    public static final String REQUESTED_OAUTH_ERROR_ENDPOINT = "requested_oauth_error_endpoint";
    public static final String REQUESTED_OAUTH_ERROR_DESCRIPTION =
            "requested_oauth_error_description";
    public static final String REQUESTED_USERINFO_ERROR = "requested_userinfo_error";

    public static final String F2F_STUB_QUEUE_NAME = "f2f_stub_queue_name";
    public static final String F2F_SEND_VC_QUEUE = "f2f_send_vc_queue";
    public static final String F2F_SEND_ERROR_QUEUE = "f2f_send_error_queue";

    public static final String VC_NOT_BEFORE_FLAG = "vcNotBeforeFlg";
    public static final String VC_NOT_BEFORE_DAY = "vcNotBeforeDay";
    public static final String VC_NOT_BEFORE_MONTH = "vcNotBeforeMonth";
    public static final String VC_NOT_BEFORE_YEAR = "vcNotBeforeYear";
    public static final String VC_NOT_BEFORE_HOURS = "vcNotBeforeHours";
    public static final String VC_NOT_BEFORE_MINUTES = "vcNotBeforeMinutes";
    public static final String VC_NOT_BEFORE_SECONDS = "vcNotBeforeSeconds";

    public static final String CI = "ci";
    public static final String MITIGATED_CIS = "ciMitigated";
    public static final String CIMIT_STUB_URL = "ciMitiBaseUrl";
    public static final String CIMIT_STUB_API_KEY = "ciMitiApiKey";
}
