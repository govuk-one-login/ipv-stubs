package uk.gov.di.ipv.stub.cred.config;

import com.nimbusds.jose.JOSEException;
import org.junit.jupiter.api.Test;
import uk.gov.di.ipv.stub.cred.fixtures.TestFixtures;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClientConfigTest {
    private static final ClientConfig CLIENT_CONFIG =
            TestFixtures.CLIENT_CONFIG.get("clientIdValid");

    @Test
    void getEncryptionPublicKeyJwkReturnsTheCorrectValues()
            throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        assertEquals(
                "{\"kty\":\"RSA\",\"e\":\"AQAB\",\"kid\":\"hHgevr-0hbkWqJ5LBLyamKO3_Sag2qjj5z12Kdmmcnw\",\"n\":\"wFrdS8EQCKiD15ruRA7HWyOGhyVtNZaWrX9EFecIe6OAbBDxGKcPBKmRU03nwx5LziEhM_ce5l4kySk72lX0a-ze5dojjfLztRYpgbI9DaEp3_FLGrZJFjOd-piOIgYABk4a5MvPn9YVxJs6XviQNe8IeSzclLGWMWGW8TENpZ1bpFCqkabES7G_uEM0kdhhgaZgUxi-RYQHPqhm6MOdgRqbiy21P0NHTEVKrikYjvXewSBqmgLUPQi850Ojs1wPdYTThj5BObYwz9hJVmbHTHoPh0H4Fdja1opcS5etoHkNYOy37So8CksV6s6ur7zUqa9FTLMrMVva7joDtsWbWJ8l3jay_OHEwRR9DSoLuabZi-kVzFFSvxdCMNvW2D2cRw3GYmG0i8qs11tljQLLEtKa2YrAdDREyEPYJGSXJ2CQxjldi36-iGb-s6A1YSB74qbWdmW1ZKjpaOfkfrTAgqhqG9QDkwhOJNBnUCQ0ieZFauw1FI3NKG5XFR37JGND_YnLlBKX3W3LHeHcXSaJac618qGo81TWnW061S3LdUEg2XbtIrO--4Ge49ImtRMAkrhTR031w7d5gUrmelBq3siPfRadbbv9C5TCG8n3T35VJK4W2lGnIyYAssOpalr7T9NenO51qBf-h2N5UZ-ST5tNF03k9zzJtfND6DqCrHs\"}",
                CLIENT_CONFIG.getEncryptionPublicKeyJwk().toString());
    }
}
