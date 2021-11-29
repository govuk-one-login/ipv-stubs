package uk.gov.di.ipv.stub.cred.service;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AuthCodeServiceTest {
    private static final String PAYLOAD = "test-payload";
    private AuthCodeService authCodeService;

    @BeforeEach
    void setup() {
        authCodeService = new AuthCodeService();
    }

    @Test
    void shouldPersistAndGetAuthCode() {
        AuthorizationCode authCode = new AuthorizationCode();

        authCodeService.persist(authCode, PAYLOAD);

        String resultantPayload = authCodeService.getPayload(authCode.getValue());
        assertNotNull(resultantPayload);
        assertEquals(PAYLOAD, resultantPayload);
    }

    @Test
    void shouldRevokeAuthCode() {
        AuthorizationCode authCode = new AuthorizationCode();

        authCodeService.persist(authCode, PAYLOAD);
        authCodeService.revoke(authCode.getValue());

        assertNull(authCodeService.getPayload(authCode.getValue()));
    }
}
