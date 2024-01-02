package uk.gov.di.ipv.stub.cred.service;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import uk.gov.di.ipv.stub.cred.config.ClientConfig;
import uk.gov.di.ipv.stub.cred.utils.StubSsmClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ConfigService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigService.class);
    private static final long DEFAULT_CONFIG_CACHE_SECONDS = 60;
    private static final Gson GSON = new Gson();
    private static final SsmClient SSM_CLIENT = getSsmClient();
    private static final Map<String, ClientConfig> CLIENT_CONFIGS = new HashMap<>();
    private static final String CLIENT_CONFIG_BASE_PATH = "/stubs/credential-issuer-stub-clients";
    private static final String ENVIRONMENT_ENV_VAR = "ENVIRONMENT";
    private static final String TEST = "TEST";
    private static final String CONFIG_CACHE_SECONDS_ENV_VAR = "CONFIG_CACHE_SECONDS";
    private static Instant lastRefresh = null;

    public static ClientConfig getClientConfig(String clientId) {
        refreshClientConfigsIfRequired();

        return CLIENT_CONFIGS.get(clientId);
    }

    public static Map<String, ClientConfig> getClientConfigs() {
        refreshClientConfigsIfRequired();

        return CLIENT_CONFIGS;
    }

    private static void refreshClientConfigsIfRequired() {
        if (lastRefresh == null
                || lastRefresh.isBefore(Instant.now().minusSeconds(getCacheDurationSeconds()))) {
            LOGGER.info("Refreshing client configs");
            getAllClientConfigs(null);
            lastRefresh = Instant.now();
        }
    }

    private static void getAllClientConfigs(String nextToken) {
        GetParametersByPathRequest.Builder requestBuilder =
                GetParametersByPathRequest.builder().path(CLIENT_CONFIG_BASE_PATH).recursive(true);
        if (nextToken != null) {
            requestBuilder.nextToken(nextToken);
        }

        GetParametersByPathResponse response =
                SSM_CLIENT.getParametersByPath(requestBuilder.build());

        for (Parameter param : response.parameters()) {
            String[] nameParts = param.name().split("/");
            CLIENT_CONFIGS.put(
                    nameParts[nameParts.length - 1],
                    GSON.fromJson(param.value(), ClientConfig.class));
        }

        if (response.nextToken() != null) {
            LOGGER.info("Next token found - fetching more client configs");
            getAllClientConfigs(response.nextToken());
        }
    }

    private static SsmClient getSsmClient() {
        if (TEST.equals(System.getenv(ENVIRONMENT_ENV_VAR))) {
            return new StubSsmClient();
        } else {
            return SsmClient.create();
        }
    }

    private static long getCacheDurationSeconds() {
        String configCacheSeconds = System.getenv(CONFIG_CACHE_SECONDS_ENV_VAR);
        return configCacheSeconds == null
                ? DEFAULT_CONFIG_CACHE_SECONDS
                : Long.parseLong(configCacheSeconds);
    }
}
