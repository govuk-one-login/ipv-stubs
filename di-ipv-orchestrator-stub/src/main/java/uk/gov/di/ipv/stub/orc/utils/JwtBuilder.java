package uk.gov.di.ipv.stub.orc.utils;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
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
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_CORE_AUDIENCE;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_CLIENT_ID;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_CLIENT_JWT_TTL;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_CLIENT_SIGNING_KEY;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_JAR_ENCRYPTION_PUBLIC_KEY;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_REDIRECT_URL;

public class JwtBuilder {
    public static final String URN_UUID = "urn:uuid:";

    public static JWTClaimsSet buildAuthRequestClaims() {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .subject(URN_UUID + UUID.randomUUID())
                .audience(IPV_CORE_AUDIENCE)
                .issueTime(Date.from(now))
                .issuer(ORCHESTRATOR_CLIENT_ID)
                .notBeforeTime(Date.from(now))
                .expirationTime(generateExpirationTime(now))
                .claim("client_id", ORCHESTRATOR_CLIENT_ID)
                .claim("response_type", ResponseType.Value.CODE.toString())
                .claim("redirect_uri", ORCHESTRATOR_REDIRECT_URL)
                .claim("state", UUID.randomUUID().toString())
                .build();
    }

    public static JWTClaimsSet buildClientAuthClaims() {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .subject(ORCHESTRATOR_CLIENT_ID)
                .audience(IPV_CORE_AUDIENCE)
                .expirationTime(generateExpirationTime(now))
                .build();
    }

    public static SignedJWT createSignedJwt(JWTClaimsSet claims)
            throws JOSEException, NoSuchAlgorithmException, InvalidKeySpecException {
        JWSSigner signer = new ECDSASigner(getSigningKey());
        SignedJWT signedJwt = new SignedJWT(generateHeader(), claims);
        signedJwt.sign(signer);
        return signedJwt;
    }

    public static EncryptedJWT encryptJwt(SignedJWT signedJwt)
            throws ParseException, JOSEException {
        JWEObject jweObject =
                new JWEObject(
                        new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                                .contentType("JWT")
                                .build(),
                        new Payload(signedJwt));
        jweObject.encrypt(new RSAEncrypter(getEncryptionKey()));
        return EncryptedJWT.parse(jweObject.serialize());
    }

    private static ECPrivateKey getSigningKey()
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] binaryKey = Base64.getDecoder().decode(ORCHESTRATOR_CLIENT_SIGNING_KEY);
        KeyFactory factory = KeyFactory.getInstance("EC");
        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(binaryKey);
        return (ECPrivateKey) factory.generatePrivate(privateKeySpec);
    }

    private static RSAPublicKey getEncryptionKey() throws java.text.ParseException, JOSEException {
        byte[] binaryKey = Base64.getDecoder().decode(ORCHESTRATOR_JAR_ENCRYPTION_PUBLIC_KEY);
        return RSAKey.parse(new String(binaryKey)).toRSAPublicKey();
    }

    private static JWSHeader generateHeader() {
        return new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build();
    }

    private static Date generateExpirationTime(Instant now) {
        return Date.from(now.plus(Long.parseLong(ORCHESTRATOR_CLIENT_JWT_TTL), ChronoUnit.SECONDS));
    }
}
