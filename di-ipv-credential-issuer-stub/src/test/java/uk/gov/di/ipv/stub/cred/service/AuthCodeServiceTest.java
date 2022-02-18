package uk.gov.di.ipv.stub.cred.service;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AuthCodeServiceTest {
    private static final String REDIRECT_URL = "https://example.com";
    private static final String PAYLOAD = "test-payload";
    private AuthCodeService authCodeService;

    @BeforeEach
    void setup() {
        authCodeService = new AuthCodeService();
    }

    @Test
    void shouldPersistAndGetAuthCodeAndRedirectUrl() {
        AuthorizationCode authCode = new AuthorizationCode();

        authCodeService.persist(authCode, PAYLOAD, REDIRECT_URL);

        String resultantPayload = authCodeService.getPayload(authCode.getValue());
        String resultantRedirectUrl = authCodeService.getRedirectUrl(authCode.getValue());

        assertNotNull(resultantPayload);
        assertEquals(PAYLOAD, resultantPayload);
        assertEquals(REDIRECT_URL, resultantRedirectUrl);
    }

    @Test
    void shouldRevokeAuthCode() {
        AuthorizationCode authCode = new AuthorizationCode();

        authCodeService.persist(authCode, PAYLOAD, REDIRECT_URL);
        authCodeService.revoke(authCode.getValue());

        assertNull(authCodeService.getPayload(authCode.getValue()));
    }
}
