package uk.gov.di.ipv.stub.experian;

import java.util.Objects;
import java.util.Optional;

public class Config {
    public static final String PORT = getConfigValue("PORT", "8080");

    private static String getConfigValue(String key, String defaultValue) {
        return Optional.ofNullable(
                        System.getenv(Objects.requireNonNull(key, "no env var specified")))
                .orElse(Objects.requireNonNull(defaultValue, "no default value"));
    }
}
