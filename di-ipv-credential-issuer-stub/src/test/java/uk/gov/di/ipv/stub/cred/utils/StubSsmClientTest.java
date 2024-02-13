package uk.gov.di.ipv.stub.cred.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.di.ipv.stub.cred.fixtures.TestFixtures.CLIENT_CONFIG;

class StubSsmClientTest {

    @BeforeAll
    public static void setUp() {
        StubSsmClient.setClientConfigParams(CLIENT_CONFIG);
    }

    @Test
    void getParametersByPathRequestReturnsAParametersByPathResponse() {
        StubSsmClient stubSsmClient = new StubSsmClient();
        GetParametersByPathResponse response =
                stubSsmClient.getParametersByPath(GetParametersByPathRequest.builder().build());

        assertEquals(4, response.parameters().size());
    }

    @Test
    void getParametersByPathCallCountReturnsNumberOfTimesGetParamsByPathCalled() {
        int currentCount = StubSsmClient.getParametersByPathCallCount();
        int calls = new Random().nextInt(10);

        StubSsmClient stubSsmClient = new StubSsmClient();
        for (int i = 0; i < calls; i++) {
            stubSsmClient.getParametersByPath(GetParametersByPathRequest.builder().build());
        }

        assertEquals(currentCount + calls, StubSsmClient.getParametersByPathCallCount());
    }
}
