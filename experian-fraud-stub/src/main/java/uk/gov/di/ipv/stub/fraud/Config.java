package uk.gov.di.ipv.stub.fraud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Config {
    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryDataStore.class);

    public static final String PORT = getConfigValue("PORT", "8080");
    public static final Map<String, String[]> ciMap;
    public static final Map<String, String[]> pepMap;

    static {
        ciMap = parseEnvString("CONTIND");
        pepMap = parseEnvString("PEPS");
    }

    private static String getConfigValue(String key, String defaultValue) {
        return Optional.ofNullable(System.getenv(key))
                .orElse(Objects.requireNonNull(defaultValue, "no default value"));
    }

    private static String getConfigValue(String key) {
        return System.getenv(key);
    }

    private static Map<String, String[]> parseEnvString(String configKey) {

        LOGGER.info(String.format("Parsing environment variable \"%s\"...", configKey));
        String configValue = getConfigValue(configKey);
        if (configValue == null) {
            LOGGER.info(String.format("Environment variable \"%s\" does not exist.", configKey));
            return new HashMap<>();
        } else {
            Map<String, String[]> envMap = new HashMap<>();
            Arrays.stream(configValue.split("\\|\\|"))
                    .forEach(
                            desc -> {
                                List<String> envPair = Arrays.asList(desc.split(":"));
                                String[] envValues = envPair.get(0).split(",");
                                String envKey = envPair.get(1);

                                envMap.put(envKey, envValues);
                            });

            return envMap;
        }
    }
}
