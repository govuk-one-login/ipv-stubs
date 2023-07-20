package uk.gov.di.ipv.core.getcontraindicatorcredential.config;

public class GetCICredentialConfig {

    public static String getCimitComponentId() {
        return getConfigValue("CIMIT_COMPONENT_ID", null);
    }

    public static String getCimitSigningKey() {
        return getConfigValue("CIMIT_SIGNING_KEY", null);
    }

    private static String getConfigValue(String key, String defaultValue) {
        var envValue = System.getenv(key);
        if (envValue == null) {
            return defaultValue;
        }

        return envValue;
    }
}
