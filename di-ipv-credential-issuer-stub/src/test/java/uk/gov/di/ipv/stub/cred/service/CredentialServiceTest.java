package uk.gov.di.ipv.stub.cred.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.di.ipv.stub.cred.domain.Credential;

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
        Credential credential =
                new Credential(Map.of("an", "attribute"), Map.of("a", "gpg45Score"), "user-id");

        credentialService.persist(credential, "1234");

        Credential resultantCredential = credentialService.getCredential("1234");
        assertNotNull(resultantCredential);
        assertEquals(credential, resultantCredential);
    }
}
