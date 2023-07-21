package uk.gov.di.ipv.core.library.service;

import software.amazon.lambda.powertools.parameters.ParamManager;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import uk.gov.di.ipv.core.library.config.EnvironmentVariable;

import static java.time.temporal.ChronoUnit.MINUTES;
import static uk.gov.di.ipv.core.library.config.EnvironmentVariable.CIMIT_SIGNING_KEY_PATH;

public class ConfigService {

    private final SSMProvider ssmProvider;

    public ConfigService(SSMProvider ssmProvider) {
        this.ssmProvider = ssmProvider;
    }

    public ConfigService() {
        this(ParamManager.getSsmProvider().defaultMaxAge(3, MINUTES));
    }

    public SSMProvider getSsmProvider() {
        return ssmProvider;
    }

    public String getEnvironmentVariable(EnvironmentVariable environmentVariable) {
        return System.getenv(environmentVariable.name());
    }

    public String getCimitSigningKey() {
        String cimitSigningKeyPath = getEnvironmentVariable(CIMIT_SIGNING_KEY_PATH);
        return getSsmParameter(cimitSigningKeyPath);
    }

    private String getSsmParameter(String ssmParamKey, String... pathProperties) {
        return ssmProvider.get(resolvePath(ssmParamKey, pathProperties));
    }

    protected String resolvePath(String path, String... pathProperties) {
        return String.format(path, (Object[]) pathProperties);
    }
}
