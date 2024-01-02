package uk.gov.di.ipv.stub.cred.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.di.ipv.stub.cred.config.ClientConfig;
import uk.gov.di.ipv.stub.cred.utils.StubSsmClient;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.di.ipv.stub.cred.fixtures.TestFixtures.CLIENT_CONFIG;

@ExtendWith(SystemStubsExtension.class)
class ConfigServiceTest {

    @SystemStub
    private final EnvironmentVariables environmentVariables =
            new EnvironmentVariables("ENVIRONMENT", "TEST");

    @BeforeAll
    public static void setUp() {
        StubSsmClient.setClientConfigParams(CLIENT_CONFIG);
    }

    @Test
    void getClientConfigReturnsCorrectConfig() {
        ClientConfig clientConfig = ConfigService.getClientConfig("clientIdValid");

        assertEquals("https://example.com/audience", clientConfig.getAudienceForVcJwt());
    }

    @Test
    void getClientConfigReturnsNullIfNoConfigFound() {
        assertNull(ConfigService.getClientConfig("🧨"));
    }

    @Test
    void getClientConfigsReturnsAllClientConfigs() {
        Map<String, ClientConfig> clientConfigs = ConfigService.getClientConfigs();

        assertEquals(
                Set.of(
                        "noAuthenticationClient",
                        "clientIdValidMultipleUri",
                        "clientIdNonRegistered",
                        "clientIdValid"),
                clientConfigs.keySet());
    }

    @Test
    void getClientConfigUsesCachedValuesIfCachedNotExpired() {
        ConfigService.getClientConfig("clientIdValid");
        int initialCallCount = StubSsmClient.getParametersByPathCallCount();
        ConfigService.getClientConfig("clientIdValid");

        assertEquals(initialCallCount, StubSsmClient.getParametersByPathCallCount());
    }

    @Test
    void getClientConfigCallsParamStoreIfCacheExpired() {
        environmentVariables.set("CONFIG_CACHE_SECONDS", "0");

        ConfigService.getClientConfig("clientIdValid");
        int initialCallCount = StubSsmClient.getParametersByPathCallCount();
        ConfigService.getClientConfig("clientIdValid");

        assertEquals(initialCallCount + 1, StubSsmClient.getParametersByPathCallCount());
    }
}
