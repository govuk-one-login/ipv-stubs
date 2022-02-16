package uk.gov.di.ipv.stub.cred.config;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class CredentialIssuerConfig {
    public static final String PORT = getConfigValue("CREDENTIAL_ISSUER_PORT","8084");
    public static final String NAME = getConfigValue("CREDENTIAL_ISSUER_NAME","Credential Issuer Stub");

    public static final String EVIDENCE_STRENGTH_PARAM = "strength";
    public static final String EVIDENCE_VALIDITY_PARAM = "validity";
    public static final String ACTIVITY_PARAM = "activity";
    public static final String FRAUD_PARAM = "fraud";
    public static final String VERIFICATION_PARAM = "verification";

    public static Map<String, ClientConfig> CLIENT_CONFIGS;

    private static final Gson gson = new Gson();
    private static final String CLIENT_CONFIG = getConfigValue("CLIENT_CONFIG",null);
    private static final String CREDENTIAL_ISSUER_TYPE_VAR = "CREDENTIAL_ISSUER_TYPE";

    private CredentialIssuerConfig() {}

    public static CriType getCriType() {
        return CriType.fromValue(getConfigValue(CREDENTIAL_ISSUER_TYPE_VAR, CriType.EVIDENCE_CRI_TYPE.value));
    }

    public static ClientConfig getClientConfig(String clientId) {
        if (CLIENT_CONFIGS == null) {
            CLIENT_CONFIGS = parseClientConfigs();
        }
        return CLIENT_CONFIGS.get(clientId);
    }

    private static String getConfigValue(String key, String defaultValue){
        var envValue = System.getenv(key);
        if (envValue == null) {
            return defaultValue;
        }

        return envValue;
    }

    private static Map<String, ClientConfig> parseClientConfigs() {
        if (CLIENT_CONFIG == null) {
            return new HashMap<>();
        }

        String clientConfigJson =
                new String(Base64.getDecoder()
                        .decode(CLIENT_CONFIG));
        Type type = new TypeToken<Map<String, ClientConfig>>() {}.getType();

        return gson.fromJson(clientConfigJson, type);
    }
}
