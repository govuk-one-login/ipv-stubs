package uk.gov.di.ipv.stub.cred.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import uk.gov.di.ipv.stub.cred.config.ClientConfig;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StubSsmClient implements SsmClient {
    // Only for use during testing

    private static final Gson GSON = new Gson();
    private static final Type clientConfigsMapType =
            new TypeToken<Map<String, ClientConfig>>() {}.getType();

    private static Map<String, ClientConfig> clientConfigs;
    private static int getParametersByPathCallCount = 0;

    @Override
    public String serviceName() {
        return null;
    }

    @Override
    public void close() {}

    @Override
    public GetParametersByPathResponse getParametersByPath(
            GetParametersByPathRequest getParametersByPathRequest) {
        getParametersByPathCallCount++;
        List<Parameter> params = new ArrayList<>();
        clientConfigs.forEach(
                (clientId, config) ->
                        params.add(
                                Parameter.builder()
                                        .name(
                                                String.format(
                                                        "/stubs/credential-issuer-stub-clients/%s",
                                                        clientId))
                                        .value(GSON.toJson(config))
                                        .build()));
        return GetParametersByPathResponse.builder().parameters(params).build();
    }

    public static void setClientConfigParams(String requiredClientConfigs) {
        clientConfigs = GSON.fromJson(requiredClientConfigs, clientConfigsMapType);
    }

    public static int getParametersByPathCallCount() {
        return getParametersByPathCallCount;
    }
}
