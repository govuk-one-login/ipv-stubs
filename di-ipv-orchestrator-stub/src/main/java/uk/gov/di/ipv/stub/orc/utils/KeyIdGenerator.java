package uk.gov.di.ipv.stub.orc.utils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;

import java.text.ParseException;

public class KeyIdGenerator {
    private KeyIdGenerator() {
        // Utility class
    }

    public static String generate(String keyMaterial, String componentIdentifier)
            throws ParseException, JOSEException {
        // Including componentIdentifier in kid to differentiate the same key used for different
        // signing functions
        return String.format(
                "%s-%s",
                componentIdentifier, ECKey.parse(keyMaterial).toPublicJWK().computeThumbprint());
    }
}
