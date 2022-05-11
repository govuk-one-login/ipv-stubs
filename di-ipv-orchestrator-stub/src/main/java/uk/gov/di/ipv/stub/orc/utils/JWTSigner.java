package uk.gov.di.ipv.stub.orc.utils;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ResponseType;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_CORE_AUDIENCE;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_JAR_ENCRYPTION_PUBLIC_KEY;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_CLIENT_ID;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_CLIENT_SIGNING_KEY;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_REDIRECT_URL;

public class JWTSigner {
    public static final String URN_UUID = "urn:uuid:";

    public JWTSigner() {}

    public SignedJWT createSignedJWT()
            throws JOSEException, InvalidKeySpecException, NoSuchAlgorithmException {
        Instant now = Instant.now();

        JWSAlgorithm jwsSigningAlgorithm = JWSAlgorithm.ES256;

        KeyFactory kf = KeyFactory.getInstance("EC");
        EncodedKeySpec privateKeySpec =
                new PKCS8EncodedKeySpec(
                        Base64.getDecoder().decode(ORCHESTRATOR_CLIENT_SIGNING_KEY));
        ECPrivateKey privateKey = (ECPrivateKey) kf.generatePrivate(privateKeySpec);
        ECDSASigner ecdsaSigner = new ECDSASigner(privateKey);

        SignedJWT signedJWT =
                new SignedJWT(
                        new JWSHeader.Builder(jwsSigningAlgorithm).build(),
                        new JWTClaimsSet.Builder()
                                .subject(getSubject())
                                .audience(IPV_CORE_AUDIENCE)
                                .issueTime(Date.from(now))
                                .issuer(ORCHESTRATOR_CLIENT_ID)
                                .notBeforeTime(Date.from(now))
                                .expirationTime(Date.from(now.plus(15, ChronoUnit.MINUTES)))
                                .claim("client_id", ORCHESTRATOR_CLIENT_ID)
                                .claim("response_type", ResponseType.Value.CODE.toString())
                                .claim("redirect_uri", ORCHESTRATOR_REDIRECT_URL)
                                .claim("state", UUID.randomUUID().toString())
                                .build());

        signedJWT.sign(ecdsaSigner);
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
            throws java.text.ParseException, NoSuchAlgorithmException, InvalidKeySpecException,
                    JOSEException {
        return RSAKey.parse(
                        new String(Base64.getDecoder().decode(ORCHESTRATOR_JAR_ENCRYPTION_PUBLIC_KEY)))
                .toRSAPublicKey();
    }
}
