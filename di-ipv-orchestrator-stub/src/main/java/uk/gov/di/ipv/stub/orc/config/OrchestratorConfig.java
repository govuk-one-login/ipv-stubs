package uk.gov.di.ipv.stub.orc.config;

public class OrchestratorConfig {
    public static final String PORT = getConfigValue("ORCHESTRATOR_PORT", "8083");
    public static final String IPV_ENDPOINT =
            getConfigValue("IPV_ENDPOINT", "https://di-ipv-core-front.london.cloudapps.digital/");
    public static final String IPV_BACKCHANNEL_ENDPOINT =
            getConfigValue(
                    "IPV_BACKCHANNEL_ENDPOINT",
                    "https://ea8lfzcdq0.execute-api.eu-west-2.amazonaws.com/");
    public static final String IPV_BACKCHANNEL_TOKEN_PATH =
            getConfigValue("IPV_BACKCHANNEL_TOKEN_PATH", "/dev/token");
    public static final String IPV_BACKCHANNEL_USER_IDENTITY_PATH =
            getConfigValue("IPV_BACKCHANNEL_USER_IDENTITY_PATH", "/dev/user-identity");
    public static final String ORCHESTRATOR_CLIENT_ID =
            getConfigValue("ORCHESTRATOR_CLIENT_ID", "di-ipv-orchestrator-stub");
    public static final String ORCHESTRATOR_REDIRECT_URL =
            getConfigValue("ORCHESTRATOR_REDIRECT_URL", "http://localhost:8083/callback");
    public static final String ORCHESTRATOR_CLIENT_SIGNING_KEY =
            getConfigValue("ORCHESTRATOR_CLIENT_SIGNING_KEY", "missing-key");
    public static final String ORCHESTRATOR_JAR_ENCRYPTION_PUBLIC_KEY =
            getConfigValue("ORCHESTRATOR_JAR_ENCRYPTION_PUBLIC_KEY", "missing-encryption-key");
    public static final String ORCHESTRATOR_DEFAULT_JAR_ENCRYPTION_PUBLIC_KEY =
            getConfigValue(
                    "ORCHESTRATOR_DEFAULT_JAR_ENCRYPTION_PUBLIC_KEY", "missing-encryption-key");
    public static final String ORCHESTRATOR_BUILD_JAR_ENCRYPTION_PUBLIC_KEY =
            getConfigValue(
                    "ORCHESTRATOR_BUILD_JAR_ENCRYPTION_PUBLIC_KEY", "missing-build-encryption-key");
    public static final String ORCHESTRATOR_STAGING_JAR_ENCRYPTION_PUBLIC_KEY =
            getConfigValue(
                    "ORCHESTRATOR_STAGING_JAR_ENCRYPTION_PUBLIC_KEY",
                    "missing-staging-encryption-key");
    public static final String ORCHESTRATOR_INTEGRATION_JAR_ENCRYPTION_PUBLIC_KEY =
            getConfigValue(
                    "ORCHESTRATOR_INTEGRATION_JAR_ENCRYPTION_PUBLIC_KEY",
                    "missing-integration-encryption-key");
    public static final String ORCHESTRATOR_CLIENT_JWT_TTL =
            getConfigValue("ORCHESTRATOR_CLIENT_JWT_TTL", "900");
    public static final String IPV_CORE_AUDIENCE =
            getConfigValue(
                    "IPV_CORE_AUDIENCE",
                    "https://build-di-ipv-cri-uk-passport-front.london.cloudapps.digital");
    public static final boolean BASIC_AUTH_ENABLE =
            Boolean.parseBoolean(getConfigValue("ORCHESTRATOR_BASIC_AUTH_ENABLE", "false"));
    public static final String BASIC_AUTH_USERNAME =
            getConfigValue("ORCHESTRATOR_BASIC_AUTH_USERNAME", null);
    public static final String BASIC_AUTH_PASSWORD =
            getConfigValue("ORCHESTRATOR_BASIC_AUTH_PASSWORD", null);

    public static final String INHERITED_IDENTITY_JWT_SIGNING_KEY =
            getConfigValue("INHERITED_IDENTITY_JWT_SIGNING_KEY", null);
    public static final String INHERITED_IDENTITY_JWT_ISSUER =
            getConfigValue(
                    "INHERITED_IDENTITY_JWT_ISSUER",
                    "https://orch.stubs.account.gov.uk/migration/v1");
    public static final String INHERITED_IDENTITY_JWT_VTM =
            getConfigValue("INHERITED_IDENTITY_JWT_VTM", "https://hmrc.gov.uk/trustmark");
    public static final String INHERITED_IDENTITY_JWT_TTL =
            getConfigValue("INHERITED_IDENTITY_JWT_TTL", "900");

    private static String getConfigValue(String key, String defaultValue) {
        var envValue = System.getenv(key);
        if (envValue == null) {
            return defaultValue;
        }

        return envValue;
    }
}
