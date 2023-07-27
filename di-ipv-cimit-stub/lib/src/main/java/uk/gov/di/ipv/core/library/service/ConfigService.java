package uk.gov.di.ipv.core.library.service;

import software.amazon.lambda.powertools.parameters.ParamManager;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import uk.gov.di.ipv.core.library.config.EnvironmentVariable;

import static java.time.temporal.ChronoUnit.MINUTES;
import static uk.gov.di.ipv.core.library.config.EnvironmentVariable.CIMIT_PARAM_BASE_PATH;
import static uk.gov.di.ipv.core.library.config.EnvironmentVariable.IS_LOCAL;

public class ConfigService {

    public static final String CIMIT_SIGNING_KEY_PARAM = "signingKey";
    public static final String CIMIT_COMPONENT_ID_PARAM = "componentId";
    private final SSMProvider ssmProvider;

    public ConfigService(SSMProvider ssmProvider) {
        this.ssmProvider = ssmProvider;
    }

    public ConfigService() {
        this(ParamManager.getSsmProvider().defaultMaxAge(3, MINUTES));
    }

    public String getEnvironmentVariable(EnvironmentVariable environmentVariable) {
        return System.getenv(environmentVariable.name());
    }

    public String getCimitComponentId() {
        String cimitParamBasePath = getEnvironmentVariable(CIMIT_PARAM_BASE_PATH);
        return getSsmParameter(cimitParamBasePath + CIMIT_COMPONENT_ID_PARAM);
    }

    public String getCimitSigningKey() {
        String cimitParamBasePath = getEnvironmentVariable(CIMIT_PARAM_BASE_PATH);
        return getSsmParameter(cimitParamBasePath + CIMIT_SIGNING_KEY_PARAM);
    }

    private String getSsmParameter(String ssmParamKey, String... pathProperties) {
        return ssmProvider.get(resolvePath(ssmParamKey, pathProperties));
    }

    protected String resolvePath(String path, String... pathProperties) {
        return String.format(path, (Object[]) pathProperties);
    }

    public boolean isRunningLocally() {
        return Boolean.parseBoolean(getEnvironmentVariable(IS_LOCAL));
    }
}
