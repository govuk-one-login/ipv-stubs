package uk.gov.di.ipv.stub.cred.config;

public class CredentialIssuerConfig {
    public static final String PORT = getConfigValue("CREDENTIAL_ISSUER_PORT","8084");
    public static final String NAME = getConfigValue("CREDENTIAL_ISSUER_NAME","Credential Issuer Stub");

    public static final String EVIDENCE_STRENGTH = "strength";
    public static final String EVIDENCE_VALIDITY = "validity";

    private CredentialIssuerConfig() {}

    private static String getConfigValue(String key, String defaultValue){
        var envValue = System.getenv(key);
        if(envValue == null){
            return defaultValue;
        }

        return envValue;
    }

    public static CriType getCriType() {
        return CriType.EVIDENCE_CRI_TYPE;
    }
}
