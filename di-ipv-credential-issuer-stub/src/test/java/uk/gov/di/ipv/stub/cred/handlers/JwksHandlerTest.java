package uk.gov.di.ipv.stub.cred.handlers;

import io.javalin.http.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static uk.gov.di.ipv.stub.cred.fixtures.TestFixtures.RSA_PRIVATE_KEY_JWK;

@ExtendWith({SystemStubsExtension.class, MockitoExtension.class})
class JwksHandlerTest {

    @SystemStub
    private static final EnvironmentVariables ENVIRONMENT_VARIABLES =
            new EnvironmentVariables("PRIVATE_ENCRYPTION_KEY_JWK", RSA_PRIVATE_KEY_JWK);

    @Mock Context mockContext;
    @Captor ArgumentCaptor<Map<String, Object>> jsonObjectCaptor;

    @Test
    void jwksEndpointShouldReturnPublicEncryptionKey() throws Exception {
        new JwksHandler().getResource(mockContext);

        verify(mockContext).json(jsonObjectCaptor.capture());

        var keys = (ArrayList<Map<String, String>>) jsonObjectCaptor.getValue().get("keys");
        assertEquals(1, keys.size());
        assertEquals("RSA", keys.get(0).get("kty"));
        assertEquals("AQAB", keys.get(0).get("e"));
        assertEquals("enc", keys.get(0).get("use"));
        assertTrue(keys.get(0).get("n").startsWith("wFrd"));
        assertEquals(
                "a-key-identifier", // pragma: allowlist secret
                keys.get(0).get("kid"));
    }
}
