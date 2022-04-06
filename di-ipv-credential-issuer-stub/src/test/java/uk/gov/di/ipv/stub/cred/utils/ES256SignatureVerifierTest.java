package uk.gov.di.ipv.stub.cred.utils;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.impl.ECDSA;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import static com.nimbusds.jwt.JWTClaimNames.SUBJECT;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.di.ipv.stub.cred.fixtures.TestFixtures.EC_PRIVATE_KEY_1;
import static uk.gov.di.ipv.stub.cred.fixtures.TestFixtures.EC_PUBLIC_JWK_1;

public class ES256SignatureVerifierTest {

    private final ES256SignatureVerifier es256SignatureVerifier = new ES256SignatureVerifier();

    @Test
    void validReturnsTrueForGoodSignature() throws Exception {
        assertTrue(es256SignatureVerifier.valid(createSignedJwt(), EC_PUBLIC_JWK_1));
    }

    @Test
    void validReturnsFalseForBadSignature() throws Exception {
        String signedJwt = createSignedJwt().serialize();
        String invalidSignatureJwt = signedJwt.substring(0, signedJwt.length() - 4) + "nope";

        assertFalse(
                es256SignatureVerifier.valid(
                        SignedJWT.parse(invalidSignatureJwt), EC_PUBLIC_JWK_1));
    }

    @Test
    void validReturnsTrueIfSignatureIsDerEncoded() throws Exception {
        SignedJWT signedJwt = createSignedJwt();
        String[] jwtParts = signedJwt.serialize().split("\\.");
        Base64URL derSignature =
                Base64URL.encode(ECDSA.transcodeSignatureToDER(signedJwt.getSignature().decode()));
        SignedJWT derSignatureJwt =
                SignedJWT.parse(String.format("%s.%s.%s", jwtParts[0], jwtParts[1], derSignature));

        assertTrue(es256SignatureVerifier.valid(derSignatureJwt, EC_PUBLIC_JWK_1));
    }

    private SignedJWT createSignedJwt() throws Exception {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().claim(SUBJECT, "Bob").build();

        KeyFactory kf = KeyFactory.getInstance("EC");
        EncodedKeySpec privateKeySpec =
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(EC_PRIVATE_KEY_1));
        ECDSASigner ecdsaSigner =
                new ECDSASigner((ECPrivateKey) kf.generatePrivate(privateKeySpec));

        JWSHeader jwsHeader =
                new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build();

        SignedJWT signedJWT = new SignedJWT(jwsHeader, claimsSet);
        signedJWT.sign(ecdsaSigner);
        return signedJWT;
    }
}
