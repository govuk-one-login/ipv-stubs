package uk.gov.di.ipv.stub.cred.config;

public class CredentialIssuerConfig {
    public static final String PORT = getConfigValue("CREDENTIAL_ISSUER_PORT","8084");

    private static String getConfigValue(String key, String defaultValue){
        var envValue = System.getenv(key);
        if(envValue == null){
            return defaultValue;
        }

        return envValue;
    }
}
