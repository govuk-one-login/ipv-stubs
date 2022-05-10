package uk.gov.di.ipv.stub.orc.utils;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ResponseType;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.*;

public class JWTSigner {
    public static final String URN_UUID = "urn:uuid:";

    public JWTSigner() {}

    public SignedJWT createSignedJWT() throws JOSEException, ParseException {
        Instant now = Instant.now();

        JWSAlgorithm jwsSigningAlgorithm = JWSAlgorithm.ES256;

        ECKey ecSigningKey =
                ECKey.parse(new String(Base64.getDecoder().decode(ORCHESTRATOR_JAR_SIGNING_JWK)));

        JWSSigner jwsSigner = new ECDSASigner(ecSigningKey);

        SignedJWT signedJWT =
                new SignedJWT(
                        new JWSHeader.Builder(jwsSigningAlgorithm)
                                .keyID(ecSigningKey.getKeyID())
                                .build(),
                        new JWTClaimsSet.Builder()
                                .subject(getSubject())
                                .audience(IPV_CORE_AUDIENCE)
                                .issueTime(Date.from(now))
                                .issuer(ORCHESTRATOR_CLIENT_ID)
                                .notBeforeTime(Date.from(now))
                                .expirationTime(Date.from(now.plus(15, ChronoUnit.MINUTES)))
                                .claim("client_id", ORCHESTRATOR_CLIENT_ID)
                                .claim("response_type", ResponseType.Value.CODE)
                                .claim("redirect_uri", ORCHESTRATOR_REDIRECT_URL)
                                .claim("state", UUID.randomUUID())
                                .build());

        signedJWT.sign(jwsSigner);
        return signedJWT;
    }

    private String getSubject() {
        return URN_UUID + UUID.randomUUID();
    }

    public EncryptedJWT encryptJWT(SignedJWT signedJWT) {
        try {
            JWEObject jweObject =
                    new JWEObject(
                            new JWEHeader.Builder(
                                            JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                                    .contentType("JWT")
                                    .build(),
                            new Payload(signedJWT));
            jweObject.encrypt(new RSAEncrypter(getEncryptionPublicKey()));

            return EncryptedJWT.parse(jweObject.serialize());
        } catch (JOSEException
                | java.text.ParseException
                | NoSuchAlgorithmException
                | InvalidKeySpecException e) {
            throw new RuntimeException("JWT encryption failed", e);
        }
    }

    private RSAPublicKey getEncryptionPublicKey()
            throws java.text.ParseException, NoSuchAlgorithmException, InvalidKeySpecException {
        RSAPublicKey publicKey =
                (RSAPublicKey)
                        KeyFactory.getInstance("RSA")
                                .generatePublic(
                                        new X509EncodedKeySpec(
                                                Base64.getDecoder()
                                                        .decode(
                                                                ORCHESTRATOR_CLIENT_ENCRYPTION_KEY)));
        return publicKey;
    }
}
