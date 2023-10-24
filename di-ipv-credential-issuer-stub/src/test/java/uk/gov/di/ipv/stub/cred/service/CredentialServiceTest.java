package uk.gov.di.ipv.stub.cred.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.di.ipv.stub.cred.fixtures.TestFixtures.DCMAW_VC;

public class CredentialServiceTest {
    private CredentialService credentialService;

    @BeforeEach
    void setup() {
        credentialService = new CredentialService();
    }

    @Test
    void shouldPersistAndGetPayload() {
        credentialService.persist(DCMAW_VC, "1234");

        String resultantCredential = credentialService.getCredentialSignedJwt("1234");
        assertEquals(DCMAW_VC, resultantCredential);
    }
}
