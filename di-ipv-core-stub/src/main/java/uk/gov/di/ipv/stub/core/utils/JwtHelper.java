package uk.gov.di.ipv.stub.core.utils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

public class JwtHelper {
    private JwtHelper() {
        // Utility Class
    }

    public static SignedJWT createSignedJwt(
            JWTClaimsSet claimsSet, JWSSigner signer, String kid, JWSAlgorithm algorithm)
            throws JOSEException {
        JWSHeader jwsHeader = generateHeader(kid, algorithm);
        SignedJWT signedJWT = new SignedJWT(jwsHeader, claimsSet);
        signedJWT.sign(signer);
        return signedJWT;
    }

    private static JWSHeader generateHeader(String kid, JWSAlgorithm algorithm) {
        return new JWSHeader.Builder(algorithm).type(JOSEObjectType.JWT).keyID(kid).build();
    }
}
