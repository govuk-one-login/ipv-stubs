package uk.gov.di.ipv.stub.orc.config;

import com.google.gson.Gson;

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
    public static final String ORCHESTRATOR_CLIENT_JWT_TTL =
            getConfigValue("ORCHESTRATOR_CLIENT_JWT_TTL", "900");
    public static final String IPV_CORE_AUDIENCE =
            getConfigValue(
                    "IPV_CORE_AUDIENCE",
                    "https://build-di-ipv-cri-uk-passport-front.london.cloudapps.digital");
    public static BasicAuthCredentials BASIC_AUTH_CREDENTIALS = parseUserAuth();
    public static final boolean ENABLE_BASIC_AUTH =
            Boolean.parseBoolean(getConfigValue("ORCHESTRATOR_ENABLE_BASIC_AUTH", "false"));

    private static String getConfigValue(String key, String defaultValue) {
        var envValue = System.getenv(key);
        if (envValue == null) {
            return defaultValue;
        }

        return envValue;
    }

    private static BasicAuthCredentials parseUserAuth() {
        String user_auth = getConfigValue("ORCHESTRATOR_BASIC_AUTH_CREDENTIALS", null);
        if (user_auth == null) {
            return null;
        }
        return new Gson().fromJson(user_auth, BasicAuthCredentials.class);
    }
}
