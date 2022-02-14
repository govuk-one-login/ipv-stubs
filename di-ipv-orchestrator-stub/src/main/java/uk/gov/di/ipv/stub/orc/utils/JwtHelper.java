package uk.gov.di.ipv.stub.orc.utils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.net.URI;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;

import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_BACKCHANNEL_ENDPOINT;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_BACKCHANNEL_TOKEN_PATH;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_CLIENT_ID;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_CLIENT_JWT_EXPIRY_MINS;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_CLIENT_SIGNING_KEY;

public class JwtHelper {

    public static SignedJWT createSignedClientAuthJwt()
            throws JOSEException, InvalidKeySpecException, NoSuchAlgorithmException {
        JWSSigner signer = new RSASSASigner(getPrivateKey());
        JWSHeader jwsHeader = generateHeader();
        JWTClaimsSet claimsSet = generateClaims();
        SignedJWT signedJWT = new SignedJWT(jwsHeader, claimsSet);
        signedJWT.sign(signer);
        return signedJWT;
    }

    private static JWSHeader generateHeader() {
        return new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build();
    }

    private static JWTClaimsSet generateClaims() {
        var claimsBuilder = new JWTClaimsSet.Builder();

        claimsBuilder.claim(JWTClaimNames.ISSUER, ORCHESTRATOR_CLIENT_ID);
        claimsBuilder.claim(JWTClaimNames.SUBJECT, ORCHESTRATOR_CLIENT_ID);
        claimsBuilder.claim(JWTClaimNames.AUDIENCE, URI.create(IPV_BACKCHANNEL_ENDPOINT).resolve(IPV_BACKCHANNEL_TOKEN_PATH).toString());

        OffsetDateTime dateTime = OffsetDateTime.now();
        claimsBuilder.claim(JWTClaimNames.EXPIRATION_TIME,
                Instant.parse(dateTime.plusMinutes(Long.parseLong(ORCHESTRATOR_CLIENT_JWT_EXPIRY_MINS)).toString()).toEpochMilli());

        return claimsBuilder.build();
    }

    private static PrivateKey getPrivateKey() throws InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] binaryKey =
                Base64.getDecoder().decode(ORCHESTRATOR_CLIENT_SIGNING_KEY);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(binaryKey);
        return factory.generatePrivate(privateKeySpec);
    }
}
