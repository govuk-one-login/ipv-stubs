package uk.gov.di.ipv.core.library.config;

public enum ConfigurationVariable {
    CIMIT_STUB_SESSION_TTL("/stubs/core/cimit/cimitStubTtl");

    private final String path;

    ConfigurationVariable(String path) {

        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
