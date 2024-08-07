package uk.gov.di.ipv.stub.orc.config;

public class OrchestratorConfig {
    public static final String PORT = getConfigValue("ORCHESTRATOR_PORT", "4500");
    public static final String IPV_ENDPOINT =
            getConfigValue("IPV_ENDPOINT", "http://localhost:4501");
    public static final String IPV_BACKCHANNEL_ENDPOINT =
            getConfigValue("IPV_BACKCHANNEL_ENDPOINT", "http://localhost:4502");
    public static final String AUTH_CLIENT_ID = getConfigValue("AUTH_CLIENT_ID", "stubAuth");
    public static final String ORCHESTRATOR_CLIENT_ID =
            getConfigValue("ORCHESTRATOR_CLIENT_ID", "orchestrator");
    public static final String ORCHESTRATOR_REDIRECT_URL =
            getConfigValue("ORCHESTRATOR_REDIRECT_URL", "http://localhost:4500/callback");
    public static final String ORCHESTRATOR_CLIENT_SIGNING_KEY =
            getConfigValue(
                    "ORCHESTRATOR_CLIENT_SIGNING_KEY",
                    // This is a test key used for local development
                    "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgOXt0P05ZsQcK7eYusgIPsqZdaBCIJiW4imwUtnaAthWhRANCAAQT1nO46ipxVTilUH2umZPN7OPI49GU6Y8YkcqLxFKUgypUzGbYR2VJGM+QJXk0PI339EyYkt6tjgfS+RcOMQNO"); // pragma: allowlist secret
    public static final String ORCHESTRATOR_DEFAULT_JAR_ENCRYPTION_PUBLIC_KEY =
            getConfigValue(
                    "ORCHESTRATOR_DEFAULT_JAR_ENCRYPTION_PUBLIC_KEY",
                    // This is a test key used for local development
                    "eyJrdHkiOiAiUlNBIiwibiI6ICIwNDY1cUp3bzhuQ2tDMnR2VjRuaXVXRjZJTTZwTmptZVlzemhUd0hQWTYwOS1IVkF0TzhQb1JMVXlBODZyelEtUXpiVDdYeGJ6Q2pmeVJYb1JGT0dsZVpxVHV3bGMyNWV6RHhWNThiaGVjUGlXRk1hRllPUzFXN3pJRHNWRm8zN2dqanZ0a2NENk9xSzhQS0F2Nm41dFVwaGpEQ2Nubm1wVE1JeUdBbnptUUNiU2tKV3U2Vl9nYzN0aXJBdWdYb1p1a01Db2h4dzNfLWM2cHJoTU4wc21ETnYwcVdtdmEzb3Fva2FiZVB3ZTFPUzcyRFh5WFItVFBkX0R0ejQtdFJyOWp2WndIdWxYNFpjczFCQmJqQnBJaW0zV05ZOGFzdjl5amxCeGtkdC1uY2toQ01aZWtQdVQ3eFdTVHJ2Y2NCX2ZublNVZ0VRV181aXJMTmRucjVNV1EiLCJlIjogIkFRQUIiLCJhbGciOiAiUlMyNTYifQ=="); // pragma: allowlist secret
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
            getConfigValue("IPV_CORE_AUDIENCE", "https://identity.local.account.gov.uk");
    public static final boolean BASIC_AUTH_ENABLE =
            Boolean.parseBoolean(getConfigValue("ORCHESTRATOR_BASIC_AUTH_ENABLE", "false"));
    public static final String BASIC_AUTH_USERNAME =
            getConfigValue("ORCHESTRATOR_BASIC_AUTH_USERNAME");
    public static final String BASIC_AUTH_PASSWORD =
            getConfigValue("ORCHESTRATOR_BASIC_AUTH_PASSWORD");

    public static final String INHERITED_IDENTITY_JWT_SIGNING_KEY =
            getConfigValue(
                    "INHERITED_IDENTITY_JWT_SIGNING_KEY",
                    // This is a test key used for local development
                    "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgOXt0P05ZsQcK7eYusgIPsqZdaBCIJiW4imwUtnaAthWhRANCAAQT1nO46ipxVTilUH2umZPN7OPI49GU6Y8YkcqLxFKUgypUzGbYR2VJGM+QJXk0PI339EyYkt6tjgfS+RcOMQNO"); // pragma: allowlist secret
    public static final String INHERITED_IDENTITY_JWT_ISSUER =
            getConfigValue(
                    "INHERITED_IDENTITY_JWT_ISSUER",
                    "https://orch.stubs.account.gov.uk/migration/v1");
    public static final String INHERITED_IDENTITY_JWT_VTM =
            getConfigValue("INHERITED_IDENTITY_JWT_VTM", "https://hmrc.gov.uk/trustmark");
    public static final String INHERITED_IDENTITY_JWT_TTL =
            getConfigValue("INHERITED_IDENTITY_JWT_TTL", "900");
    public static final String EVCS_ACCESS_TOKEN_ENDPOINT =
            getConfigValue(
                    "EVCS_ACCESS_TOKEN_ENDPOINT",
                    "https://mock.credential-store.build.account.gov.uk/generate");
    public static final String EVCS_ACCESS_TOKEN_TTL =
            getConfigValue("EVCS_ACCESS_TOKEN_TTL", "60");
    public static final String EVCS_ACCESS_TOKEN_SIGNING_KEY_JWK =
            getConfigValue(
                    "EVCS_ACCESS_TOKEN_SIGNING_KEY_JWK",
                    // This is a test key used for local development
                    "{\"kty\":\"EC\",\"d\":\"OXt0P05ZsQcK7eYusgIPsqZdaBCIJiW4imwUtnaAthU\",\"crv\":\"P-256\",\"x\":\"E9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIM\",\"y\":\"KlTMZthHZUkYz5AleTQ8jff0TJiS3q2OB9L5Fw4xA04\"}"); // pragma: allowlist secret

    private static String getConfigValue(String key) {
        return getConfigValue(key, null);
    }

    private static String getConfigValue(String key, String defaultValue) {
        var envValue = System.getenv(key);
        if (envValue == null) {
            return defaultValue;
        }

        return envValue;
    }
}
