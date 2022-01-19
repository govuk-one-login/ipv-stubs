package uk.gov.di.ipv.stub.cred.config;

public class CredentialIssuerConfig {
    public static final String PORT = getConfigValue("CREDENTIAL_ISSUER_PORT","8084");

    public static final String EVIDENCE_STRENGTH_PARAM = "strength";
    public static final String EVIDENCE_VALIDITY_PARAM = "validity";
    public static final String ACTIVITY_PARAM = "activity";
    public static final String FRAUD_PARAM = "fraud";
    public static final String VERIFICATION_PARAM = "verification";

    private static final String CREDENTIAL_ISSUER_TYPE_VAR = "CREDENTIAL_ISSUER_TYPE";

    private CredentialIssuerConfig() {}

    private static String getConfigValue(String key, String defaultValue){
        var envValue = System.getenv(key);
        if (envValue == null) {
            return defaultValue;
        }

        return envValue;
    }

    public static CriType getCriType() {
        return CriType.fromValue(getConfigValue(CREDENTIAL_ISSUER_TYPE_VAR, CriType.EVIDENCE_CRI_TYPE.value));
    }
}
