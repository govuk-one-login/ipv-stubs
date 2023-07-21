package uk.gov.di.ipv.core.library.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringMapMessage;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.lambda.powertools.parameters.ParamManager;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import uk.gov.di.ipv.core.library.config.EnvironmentVariable;

import java.net.URI;

import static uk.gov.di.ipv.core.library.config.EnvironmentVariable.CIMIT_SIGNING_KEY_PATH;

public class ConfigService {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CORE_BASE_PATH = "/%s/core/";
    private final SSMProvider ssmProvider;
    public static final int LOCALHOST_PORT = 4567;
    private static final String LOCALHOST_URI = "http://localhost:" + LOCALHOST_PORT;

    public ConfigService(SSMProvider ssmProvider) {
        this.ssmProvider = ssmProvider;
    }

    public ConfigService() {
        this(
                //                ParamManager.getSsmProvider(
                //                                SsmClient.builder()
                //
                // .httpClient(UrlConnectionHttpClient.create())
                //                                        .build())
                //                        .defaultMaxAge(3, MINUTES));

                ParamManager.getSsmProvider(
                        SsmClient.builder()
                                .endpointOverride(URI.create(LOCALHOST_URI))
                                .httpClient(UrlConnectionHttpClient.create())
                                .region(Region.EU_WEST_2)
                                .build()));
    }

    public SSMProvider getSsmProvider() {
        return ssmProvider;
    }

    public String getEnvironmentVariable(EnvironmentVariable environmentVariable) {
        LOGGER.info(new StringMapMessage().with("AT:", "getEnvironmentVariable"));

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
