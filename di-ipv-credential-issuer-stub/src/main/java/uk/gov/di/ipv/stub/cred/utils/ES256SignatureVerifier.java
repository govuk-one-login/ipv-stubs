package uk.gov.di.ipv.stub.cred.utils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.impl.ECDSA;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;

import static com.nimbusds.jose.JWSAlgorithm.ES256;

public class ES256SignatureVerifier {
    public boolean valid(SignedJWT signedJWT, String publicJwk)
            throws JOSEException, ParseException {
        ECKey signingPublicJwk = ECKey.parse(publicJwk);
        ECDSAVerifier ecdsaVerifier = new ECDSAVerifier(signingPublicJwk);
        if (signatureIsDerFormat(signedJWT)) {
            return transcodeSignature(signedJWT).verify(ecdsaVerifier);
        }
        return signedJWT.verify(ecdsaVerifier);
    }

    public SignedJWT transcodeSignature(SignedJWT signedJWT)
            throws JOSEException, java.text.ParseException {
        Base64URL transcodedSignatureBase64 =
                Base64URL.encode(
                        ECDSA.transcodeSignatureToConcat(
                                signedJWT.getSignature().decode(),
                                ECDSA.getSignatureByteArrayLength(ES256)));
        String[] jwtParts = signedJWT.serialize().split("\\.");
        return SignedJWT.parse(
                String.format("%s.%s.%s", jwtParts[0], jwtParts[1], transcodedSignatureBase64));
    }

    public boolean signatureIsDerFormat(SignedJWT signedJWT) throws JOSEException {
        return signedJWT.getSignature().decode().length != ECDSA.getSignatureByteArrayLength(ES256);
    }
}
