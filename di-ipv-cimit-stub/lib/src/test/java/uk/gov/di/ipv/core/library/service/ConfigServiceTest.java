package uk.gov.di.ipv.core.library.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import uk.gov.di.ipv.core.library.config.EnvironmentVariable;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.core.library.config.EnvironmentVariable.CIMIT_PARAM_BASE_PATH;
import static uk.gov.di.ipv.core.library.service.ConfigService.CIMIT_COMPONENT_ID_PARAM;
import static uk.gov.di.ipv.core.library.service.ConfigService.CIMIT_SIGNING_KEY_PARAM;

@ExtendWith({SystemStubsExtension.class, MockitoExtension.class})
class ConfigServiceTest {

    private static final String CIMIT_SIGNING_KEY = "wwmTDFmsmsdqewxaSSDmddsds";
    public static final String CIMIT_PARAM_BASE_PATH_VALUE = "/stubs/core/cimit/";
    public static final String CIMIT_COMPONENT_ID_VALUE = "http://cimit.test";

    @SystemStub private EnvironmentVariables environmentVariables;

    @SystemStub private SystemProperties systemProperties;

    @Mock SSMProvider ssmProvider;

    private ConfigService configService;

    @BeforeEach
    void setUp() {
        configService = new ConfigService(ssmProvider);
    }

    @Test
    void getEnvironmentVariable_success() {
        environmentVariables.set(
                EnvironmentVariable.CIMIT_PARAM_BASE_PATH.name(), CIMIT_PARAM_BASE_PATH_VALUE);

        assertEquals(
                CIMIT_PARAM_BASE_PATH_VALUE,
                configService.getEnvironmentVariable(EnvironmentVariable.CIMIT_PARAM_BASE_PATH));
    }

    @Test
    void getCimitComponentID_success() {
        environmentVariables.set(CIMIT_PARAM_BASE_PATH.name(), CIMIT_PARAM_BASE_PATH_VALUE);

        when(ssmProvider.get(CIMIT_PARAM_BASE_PATH_VALUE + CIMIT_COMPONENT_ID_PARAM))
                .thenReturn(CIMIT_COMPONENT_ID_VALUE);

        assertEquals(CIMIT_COMPONENT_ID_VALUE, configService.getCimitComponentId());
    }

    @Test
    void getCimitSigningKey_success() {
        environmentVariables.set(CIMIT_PARAM_BASE_PATH.name(), CIMIT_PARAM_BASE_PATH_VALUE);

        when(ssmProvider.get(CIMIT_PARAM_BASE_PATH_VALUE + CIMIT_SIGNING_KEY_PARAM))
                .thenReturn(CIMIT_SIGNING_KEY);

        assertEquals(CIMIT_SIGNING_KEY, configService.getCimitSigningKey());
    }

    @Test
    void getCimitSigningKey_failure() {
        environmentVariables.set(CIMIT_PARAM_BASE_PATH.name(), CIMIT_PARAM_BASE_PATH_VALUE);

        when(ssmProvider.get(CIMIT_PARAM_BASE_PATH_VALUE)).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> configService.getCimitSigningKey());
    }
}
