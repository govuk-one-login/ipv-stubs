package uk.gov.di.ipv.stub.cred.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CredentialServiceTest {
    private CredentialService credentialService;

    @BeforeEach
    void setup() {
        credentialService = new CredentialService();
    }

    @Test
    void shouldPersistAndGetPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("test", "test-value");
        payload.put(RequestParamConstants.ACTIVITY_HISTORY, "0");
        payload.put(RequestParamConstants.IDENTITY_FRAUD, "2");
        payload.put(RequestParamConstants.VERIFICATION, "4");

        credentialService.persist(payload, "1234");

        Map<String, Object> resultantCredential = credentialService.getCredential("1234");
        assertNotNull(resultantCredential);
        assertEquals(payload, resultantCredential);
    }
}
