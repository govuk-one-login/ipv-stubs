package uk.gov.di.ipv.core.getcontraindicatorcredential.factory;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSASigner;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class ECDSASignerFactory {

    public static final String EC_ALGO = "EC";

    public ECDSASigner getSigner(String privateKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        return new ECDSASigner(
                (ECPrivateKey)
                        KeyFactory.getInstance(EC_ALGO)
                                .generatePrivate(
                                        new PKCS8EncodedKeySpec(
                                                Base64.getDecoder().decode(privateKey))));
    }
}
