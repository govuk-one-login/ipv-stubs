package uk.gov.di.ipv.stub.orc.config;

public class OrchestratorConfig {
    public static final String PORT = getConfigValue("ORCHESTRATOR_PORT", "4500");
    public static final String IPV_ENDPOINT =
            getConfigValue("IPV_ENDPOINT", "http://localhost:4501");
    public static final String IPV_BACKCHANNEL_ENDPOINT =
            getConfigValue("IPV_BACKCHANNEL_ENDPOINT", "http://localhost:4502");
    public static final String AUTH_CLIENT_ID = getConfigValue("AUTH_CLIENT_ID", "stubAuth");
    public static final String ORCHESTRATOR_CLIENT_ID =
            getConfigValue("ORCHESTRATOR_CLIENT_ID", "orchStub");
    public static final String ORCHESTRATOR_REDIRECT_URL =
            getConfigValue("ORCHESTRATOR_REDIRECT_URL", "http://localhost:4500/callback");
    public static final String ORCHESTRATOR_SIGNING_JWK =
            getConfigValue(
                    "ORCHESTRATOR_SIGNING_JWK",
                    // This is a test key used for local development
                    "{\"kty\":\"EC\",\"kid\":\"orch-signing-default-FI4xysvMVdRtkt6xmO5gqcaTF4Tf9NKD1zdg3T8y69M\",\"use\":\"sig\",\"d\":\"OXt0P05ZsQcK7eYusgIPsqZdaBCIJiW4imwUtnaAthU\",\"crv\":\"P-256\",\"x\":\"E9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIM\",\"y\":\"KlTMZthHZUkYz5AleTQ8jff0TJiS3q2OB9L5Fw4xA04\"}"); // pragma: allowlist secret
    public static final String AUTH_SIGNING_JWK =
            getConfigValue(
                    "AUTH_SIGNING_JWK",
                    // This is a test key used for local development
                    "{\"kty\":\"EC\",\"kid\":\"auth-signing-default-FI4xysvMVdRtkt6xmO5gqcaTF4Tf9NKD1zdg3T8y69M\",\"use\":\"sig\",\"d\":\"OXt0P05ZsQcK7eYusgIPsqZdaBCIJiW4imwUtnaAthU\",\"crv\":\"P-256\",\"x\":\"E9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIM\",\"y\":\"KlTMZthHZUkYz5AleTQ8jff0TJiS3q2OB9L5Fw4xA04\"}"); // pragma: allowlist secret
    public static final String ORCHESTRATOR_DEFAULT_JAR_ENCRYPTION_PUBLIC_JWK =
            getConfigValue(
                    "ORCHESTRATOR_DEFAULT_JAR_ENCRYPTION_PUBLIC_JWK",
                    // This is a test key used for local development - it's from the core build
                    // environment
                    "{\"kty\":\"RSA\",\"kid\":\"1b5d35fb351ad12f1d34cf10d2a7c080990d3ac39bae6dcec3e2ff2ee45d6550\",\"use\":\"enc\",\"e\":\"AQAB\",\"n\":\"nel7ibmSTaXWhwEAdqKTiEVcxsYgv6CdXaz90aVN7IorlaCeNj0j06OsA4zdmWEjj21wEZULsxPoZo5N_tsQ7NtOnOkcnDc-g_Nbpt0jelzJSbFRkx3kwXy8YIYKR_myNbiHNTTc7S6GkQRg0N1MPWtzoEKYJs41AN4onrsvUzgpCypWwPy2-ppsaDvms_11YA7A7x3zHj9oKCPJ_uk_0MV3vZAxCxbiPb9ABGWcoGQ5QKGfv40ylBsEdOhE3w-3SAAQIrrHyMRGGiPxcNO161XVL-lOnYt93FgEe16LgpfE22UdENfHnG0UQaTgph1Dm24oqn7qpPTY2DfER5HCKQ\"}"); // pragma: allowlist secret
    public static final String ORCHESTRATOR_DEV01_JAR_ENCRYPTION_PUBLIC_JWK =
            getConfigValue(
                    "ORCHESTRATOR_DEV01_JAR_ENCRYPTION_PUBLIC_JWK", "missing-dev01-encryption-key");
    public static final String ORCHESTRATOR_DEV02_JAR_ENCRYPTION_PUBLIC_JWK =
            getConfigValue(
                    "ORCHESTRATOR_DEV02_JAR_ENCRYPTION_PUBLIC_JWK", "missing-dev02-encryption-key");
    public static final String ORCHESTRATOR_BUILD_JAR_ENCRYPTION_PUBLIC_JWK =
            getConfigValue(
                    "ORCHESTRATOR_BUILD_JAR_ENCRYPTION_PUBLIC_JWK", "missing-build-encryption-key");
    public static final String ORCHESTRATOR_STAGING_JAR_ENCRYPTION_PUBLIC_JWK =
            getConfigValue(
                    "ORCHESTRATOR_STAGING_JAR_ENCRYPTION_PUBLIC_JWK",
                    "missing-staging-encryption-key");
    public static final String ORCHESTRATOR_INTEGRATION_JAR_ENCRYPTION_PUBLIC_JWK =
            getConfigValue(
                    "ORCHESTRATOR_INTEGRATION_JAR_ENCRYPTION_PUBLIC_JWK",
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

    public static final String INHERITED_IDENTITY_JWT_SIGNING_JWK =
            getConfigValue(
                    "INHERITED_IDENTITY_JWT_SIGNING_JWK",
                    // This is a test key used for local development
                    "{\"kty\":\"EC\",\"kid\":\"inherited-identity-signing-default-FI4xysvMVdRtkt6xmO5gqcaTF4Tf9NKD1zdg3T8y69M\",\"use\":\"sig\",\"d\":\"OXt0P05ZsQcK7eYusgIPsqZdaBCIJiW4imwUtnaAthU\",\"crv\":\"P-256\",\"x\":\"E9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIM\",\"y\":\"KlTMZthHZUkYz5AleTQ8jff0TJiS3q2OB9L5Fw4xA04\"}"); // pragma: allowlist secret
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
    public static final String EVCS_ACCESS_TOKEN_SIGNING_JWK =
            getConfigValue(
                    "EVCS_ACCESS_TOKEN_SIGNING_JWK",
                    // This is a test key used for local development
                    "{\"kty\":\"EC\",\"kid\":\"evcs-token-signing-default-FI4xysvMVdRtkt6xmO5gqcaTF4Tf9NKD1zdg3T8y69M\",\"use\":\"sig\",\"d\":\"OXt0P05ZsQcK7eYusgIPsqZdaBCIJiW4imwUtnaAthU\",\"crv\":\"P-256\",\"x\":\"E9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIM\",\"y\":\"KlTMZthHZUkYz5AleTQ8jff0TJiS3q2OB9L5Fw4xA04\"}"); // pragma: allowlist secret

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
