package uk.gov.di.ipv.stub.cred.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.oauth2.sdk.auth.verifier.Context;
import com.nimbusds.oauth2.sdk.auth.verifier.InvalidClientException;
import com.nimbusds.oauth2.sdk.id.ClientID;
import org.junit.jupiter.api.Test;
import uk.gov.di.ipv.stub.cred.config.ClientConfig;
import uk.gov.di.ipv.stub.cred.fixtures.TestFixtures;

import java.security.PublicKey;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import static com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod.PRIVATE_KEY_JWT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CriConfigPublicKeySelectorTest {
    @Test
    void selectClientSecretsThrowsUnsupportedOperationException() {
        CriConfigPublicKeySelector keySelector = new CriConfigPublicKeySelector();
        assertThrows(
                UnsupportedOperationException.class,
                () -> {
                    keySelector.selectClientSecrets(
                            new ClientID(), PRIVATE_KEY_JWT, new Context<>());
                });
    }

    @Test
    void registerClientsAllowsSelectionOfTheirKey() throws Exception {
        ClientConfig clientConfig1 = new ClientConfig();
        Map<String, String> jwtAuthConfig1 =
                Map.of(
                        "id", "clientConfig1",
                        "issuer", "clientConfig1",
                        "subject", "clientConfig1",
                        "signingPublicJwk", TestFixtures.EC_PUBLIC_JWK_1,
                        "validRedirectUrls", "https://example.com",
                        "authenticationMethod", "jwt");
        clientConfig1.setJwtAuthentication(jwtAuthConfig1);

        ClientConfig clientConfig2 = new ClientConfig();
        Map<String, String> jwtAuthConfig2 =
                Map.of(
                        "id", "clientConfig2",
                        "issuer", "clientConfig2",
                        "subject", "clientConfig2",
                        "signingPublicJwk", TestFixtures.EC_PUBLIC_JWK_2,
                        "validRedirectUrls", "https://example.com",
                        "authenticationMethod", "jwt");
        clientConfig2.setJwtAuthentication(jwtAuthConfig2);

        ClientConfig clientConfig3 = new ClientConfig();
        Map<String, String> jwtAuthConfig3 =
                Map.of(
                        "id", "clientConfig3",
                        "issuer", "clientConfig3",
                        "subject", "clientConfig3",
                        "signingPublicJwk", TestFixtures.EC_PUBLIC_JWK_3,
                        "validRedirectUrls", "https://example.com",
                        "authenticationMethod", "jwt");
        clientConfig3.setJwtAuthentication(jwtAuthConfig3);

        Map<String, ClientConfig> clientConfigs =
                Map.of(
                        "clientConfig1", clientConfig1,
                        "clientConfig2", clientConfig2,
                        "clientConfig3", clientConfig3);

        CriConfigPublicKeySelector keySelector = new CriConfigPublicKeySelector();
        keySelector.registerClients(clientConfigs);

        List<? extends PublicKey> client3PublicKeys =
                keySelector.selectPublicKeys(
                        new ClientID("clientConfig3"), null, null, false, null);
        List<? extends PublicKey> client1PublicKeys =
                keySelector.selectPublicKeys(
                        new ClientID("clientConfig1"), null, null, false, null);
        List<? extends PublicKey> client2PublicKeys =
                keySelector.selectPublicKeys(
                        new ClientID("clientConfig2"), null, null, false, null);

        assertEquals(1, client1PublicKeys.size());
        assertEquals(1, client2PublicKeys.size());
        assertEquals(1, client3PublicKeys.size());
        assertEquals(extractPublicKey(TestFixtures.EC_PUBLIC_JWK_1), client1PublicKeys.get(0));
        assertEquals(extractPublicKey(TestFixtures.EC_PUBLIC_JWK_2), client2PublicKeys.get(0));
        assertEquals(extractPublicKey(TestFixtures.EC_PUBLIC_JWK_3), client3PublicKeys.get(0));
    }

    @Test
    void onlyThrowsForBadCertsWhenRetrievingPublicKeys() throws Exception {
        ClientConfig clientConfig1 = new ClientConfig();
        Map<String, String> jwtAuthConfig1 =
                Map.of(
                        "id", "clientConfig1",
                        "issuer", "clientConfig1",
                        "subject", "clientConfig1",
                        "signingPublicJwk", "{\"valid_json\": \"but_not_a_jwk\"}",
                        "validRedirectUrls", "https://example.com",
                        "authenticationMethod", "jwt");
        clientConfig1.setJwtAuthentication(jwtAuthConfig1);

        ClientConfig clientConfig2 = new ClientConfig();
        Map<String, String> jwtAuthConfig2 =
                Map.of(
                        "id", "clientConfig2",
                        "issuer", "clientConfig2",
                        "subject", "clientConfig2",
                        "signingPublicJwk", TestFixtures.EC_PUBLIC_JWK_1,
                        "validRedirectUrls", "https://example.com",
                        "authenticationMethod", "jwt");
        clientConfig2.setJwtAuthentication(jwtAuthConfig2);

        ClientConfig clientConfig3 = new ClientConfig();
        Map<String, String> jwtAuthConfig3 =
                Map.of(
                        "id", "clientConfig3",
                        "issuer", "clientConfig3",
                        "subject", "clientConfig3",
                        "signingPublicJwk", "Not even json",
                        "validRedirectUrls", "https://example.com",
                        "authenticationMethod", "jwt");
        clientConfig3.setJwtAuthentication(jwtAuthConfig3);

        Map<String, ClientConfig> clientConfigs =
                Map.of(
                        "clientConfig1", clientConfig1,
                        "clientConfig2", clientConfig2,
                        "clientConfig3", clientConfig3);

        CriConfigPublicKeySelector keySelector = new CriConfigPublicKeySelector();

        assertDoesNotThrow(
                () -> {
                    keySelector.registerClients(clientConfigs);
                });

        InvalidClientException client1exception =
                assertThrows(
                        InvalidClientException.class,
                        () -> {
                            List<? extends PublicKey> client1PublicKeys =
                                    keySelector.selectPublicKeys(
                                            new ClientID("clientConfig1"), null, null, false, null);
                        });

        InvalidClientException client3exception =
                assertThrows(
                        InvalidClientException.class,
                        () -> {
                            List<? extends PublicKey> client3PublicKeys =
                                    keySelector.selectPublicKeys(
                                            new ClientID("clientConfig3"), null, null, false, null);
                        });

        List<? extends PublicKey> client2PublicKeys =
                keySelector.selectPublicKeys(
                        new ClientID("clientConfig2"), null, null, false, null);

        assertEquals(
                "No public keys found for clientId 'clientConfig1'", client1exception.getMessage());
        assertEquals(
                "No public keys found for clientId 'clientConfig3'", client3exception.getMessage());
        assertEquals(extractPublicKey(TestFixtures.EC_PUBLIC_JWK_1), client2PublicKeys.get(0));
    }

    private PublicKey extractPublicKey(String jwk) throws ParseException, JOSEException {
        return ECKey.parse(jwk).toECPublicKey();
    }
}
