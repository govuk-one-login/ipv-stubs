package uk.gov.di.ipv.stub.fraud;

import java.util.Objects;
import java.util.Optional;

public class Config {
    public static final String PORT = getConfigValue("PORT", "8080");
    public static final String[] CI1 = getConfigValue("CI1");
    public static final String[] CI2 = getConfigValue("CI2");
    public static final String[] CI3 = getConfigValue("CI3");
    public static final String[] CI4 = getConfigValue("CI4");
    public static final String[] CI5 = getConfigValue("CI5");

    private static String getConfigValue(String key, String defaultValue) {
        return Optional.ofNullable(
                        System.getenv(Objects.requireNonNull(key, "no env var specified")))
                .orElse(Objects.requireNonNull(defaultValue, "no default value"));
    }

    private static String[] getConfigValue(String key) {
        return Optional.of(
                        System.getenv(Objects.requireNonNull(key, "no env var specified"))
                                .split(","))
                .orElse(new String[] {});
    }
}
