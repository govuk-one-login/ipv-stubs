package uk.gov.di.ipv.stub.experian;

import java.util.Objects;
import java.util.Optional;

public class Config {
    public static final String CORE_STUB_PORT = getConfigValue("CORE_STUB_PORT", "8090");

    private static String getConfigValue(String key, String defaultValue) {
        return Optional.ofNullable(System.getenv(Objects.requireNonNull(key, "no key")))
                .orElse(Objects.requireNonNull(defaultValue, "no default value"));
    }
}
