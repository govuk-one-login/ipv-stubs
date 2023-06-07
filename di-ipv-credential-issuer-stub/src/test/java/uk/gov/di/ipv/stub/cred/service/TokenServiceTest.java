package uk.gov.di.ipv.stub.cred.service;

import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TokenServiceTest {
    private static final String PAYLOAD = "test-payload";
    private TokenService tokenService;

    @BeforeEach
    void setup() {
        tokenService = new TokenService();
    }

    @Test
    void shouldCreateAccessToken() {
        AccessToken accessToken = tokenService.createBearerAccessToken();
        assertNotNull(accessToken);
        assertEquals(3600, accessToken.getLifetime());
    }

    @Test
    void shouldPersistAndGetAccessToken() {
        BearerAccessToken accessToken = new BearerAccessToken();

        tokenService.persist(accessToken, PAYLOAD);

        String resultantPayload = tokenService.getPayload(accessToken.toAuthorizationHeader());
        assertNotNull(resultantPayload);
        assertEquals(PAYLOAD, resultantPayload);
    }

    @Test
    void shouldRevokeAuthCode() {
        BearerAccessToken accessToken = new BearerAccessToken();

        tokenService.persist(accessToken, PAYLOAD);
        tokenService.revoke(accessToken.toAuthorizationHeader());

        assertNull(tokenService.getPayload(accessToken.toAuthorizationHeader()));
    }

    @Test
    void shouldReturnInvalidRequestIfTokenNull() {
        ValidationResult testResult = tokenService.validateAccessToken(null);
        assertFalse(testResult.isValid());
        assertEquals(OAuth2Error.INVALID_REQUEST, testResult.getError());
    }

    @Test
    void shouldReturnInvalidClientIfPayloadNull() {
        ValidationResult testResult = tokenService.validateAccessToken("test");
        assertFalse(testResult.isValid());
        assertEquals(OAuth2Error.INVALID_CLIENT, testResult.getError());
    }

    @Test
    void shouldReturnValidRequestIfTokenValid() {}
}
