package uk.gov.di.ipv.stub.cred.config;

public class CredentialIssuerConfig {
    public static final String PORT = getConfigValue("CREDENTIAL_ISSUER_PORT", "8084");
    public static final String NAME =
            getConfigValue("CREDENTIAL_ISSUER_NAME", "Credential Issuer Stub");
    public static final String VC_DEFAULT_TTL = "300";

    public static String CLIENT_AUDIENCE = getConfigValue("CLIENT_AUDIENCE", null);
    public static final String DEV_DOMAIN =
            getConfigValue("DEV_DOMAIN", ".dev.identity.account.gov.uk");
    public static final String F2F_STUB_QUEUE_URL = getConfigValue("F2F_STUB_QUEUE_URL", null);
    public static final String F2F_STUB_QUEUE_NAME = getConfigValue("F2F_STUB_QUEUE_NAME", null);

    public static final String EVIDENCE_TYPE_PARAM = "type";
    public static final String EVIDENCE_TYPE_IDENTITY_CHECK = "IdentityCheck";
    public static final String EVIDENCE_TXN_PARAM = "txn";
    public static final String EVIDENCE_STRENGTH_PARAM = "strengthScore";
    public static final String EVIDENCE_VALIDITY_PARAM = "validityScore";
    public static final String ACTIVITY_PARAM = "activityHistoryScore";
    public static final String FRAUD_PARAM = "identityFraudScore";
    public static final String VERIFICATION_PARAM = "verificationScore";
    public static final String CHECK_DETAILS_PARAM = "checkDetails";
    public static final String FAILED_CHECK_DETAILS_PARAM = "failedCheckDetails";
    public static final String BIOMETRICK_VERIFICATION_PARAM = "biometricVerificationScore";
    public static final String EVIDENCE_CONTRAINDICATOR_PARAM = "ci";
    public static final String MITIGATED_CONTRAINDICATORS_PARAM = "ciMitigated";
    public static final String BASE_STUB_MANAGED_POST_URL_PARAM = "ciMitiBaseUrl";
    public static final String STUB_MANAGEMENT_API_KEY_PARAM = "ciMitiApiKey";
    public static final String CRI_MITIGATION_ENABLED = "MITIGATION_ENABLED";
    public static final String EXPIRY_FLAG_CHK_BOX_VALUE = "on";
    public static final String EXPIRY_FLAG = "vcExpiryFlg";
    public static final String EXPIRY_HOURS = "expHours";
    public static final String EXPIRY_MINUTES = "expMinutes";
    public static final String EXPIRY_SECONDS = "expSeconds";

    private static final String CREDENTIAL_ISSUER_TYPE_VAR = "CREDENTIAL_ISSUER_TYPE";

    private CredentialIssuerConfig() {}

    public static CriType getCriType() {
        return CriType.fromValue(
                getConfigValue(CREDENTIAL_ISSUER_TYPE_VAR, CriType.EVIDENCE_CRI_TYPE.value));
    }

    public static String getVerifiableCredentialIssuer() {
        return getConfigValue("VC_ISSUER", null);
    }

    public static String getVerifiableCredentialSigningKey() {
        return getConfigValue("VC_SIGNING_KEY", null);
    }

    public static Long getVerifiableCredentialTtlSeconds() {
        return Long.parseLong(getConfigValue("VC_TTL_SECONDS", VC_DEFAULT_TTL));
    }

    private static String getConfigValue(String key, String defaultValue) {
        var envValue = System.getenv(key);
        if (envValue == null) {
            return defaultValue;
        }

        return envValue;
    }

    public static boolean isEnabled(String key, String defaultValue) {
        return Boolean.parseBoolean(getConfigValue(key, defaultValue));
    }
}
