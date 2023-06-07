package uk.gov.di.ipv.stub.cred.utils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jwt.SignedJWT;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;

public class JwtHelper {
    public static SignedJWT getSignedJWT(String request, PrivateKey encryptionPrivateKey)
            throws java.text.ParseException {
        try {
            JWEObject jweObject = getJweObject(request, encryptionPrivateKey);
            return jweObject.getPayload().toSignedJWT();
        } catch (java.text.ParseException
                | NoSuchAlgorithmException
                | InvalidKeySpecException
                | JOSEException e) {
            return SignedJWT.parse(request);
        }
    }

    public static JWEObject getJweObject(String requestParam, PrivateKey encryptionPrivateKey)
            throws java.text.ParseException, NoSuchAlgorithmException, InvalidKeySpecException,
                    JOSEException {
        JWEObject encryptedJweObject = JWEObject.parse(requestParam);
        RSADecrypter rsaDecrypter = new RSADecrypter(encryptionPrivateKey);
        encryptedJweObject.decrypt(rsaDecrypter);
        return encryptedJweObject;
    }
}
