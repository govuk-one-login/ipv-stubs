package uk.gov.di.ipv.stub.orc.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.INHERITED_IDENTITY_JWT_ISSUER;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.INHERITED_IDENTITY_JWT_SIGNING_KEY;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.INHERITED_IDENTITY_JWT_TTL;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.INHERITED_IDENTITY_JWT_VTM;

public class InheritedIdentityJWTBuilder {

    private static final String VC_CREDENTIAL_SUBJECT = "credentialSubject";
    private static final String VOT = "vot";
    private static final String VTM = "vtm";
    private static final String VC = "vc";
    private static final String VC_TYPE = "type";
    private static final String VERIFIABLE_CREDENTIAL_TYPE = "VerifiableCredential";
    private static final String INHERITED_IDENTITY_CREDENTIAL_TYPE = "InheritedIdentityCredential";
    private static final String VC_EVIDENCE = "evidence";
    private static final String EC_ALGO = "EC";
    private static final List<String> allowedValues = List.of("PCL200", "PCL250", "P2");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(InheritedIdentityJWTBuilder.class);

    public static SignedJWT generate(
            String userId, String[] vtr, String credentialSubject, String evidence)
            throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        Map<String, Object> vc = new LinkedHashMap<>();
        vc.put(
                VC_TYPE,
                new String[] {VERIFIABLE_CREDENTIAL_TYPE, INHERITED_IDENTITY_CREDENTIAL_TYPE});
        vc.put(VC_CREDENTIAL_SUBJECT, convertJsonToMap(credentialSubject));
        vc.put(VC_EVIDENCE, List.of(convertJsonToMap(evidence)));
        Instant now = Instant.now();
        JWTClaimsSet claimsSet =
                new JWTClaimsSet.Builder()
                        .subject(userId)
                        .issuer(INHERITED_IDENTITY_JWT_ISSUER)
                        .notBeforeTime(Date.from(now))
                        .expirationTime(generateExpirationTime(now))
                        .claim(VOT, getValidVtrValues(vtr))
                        .claim(VTM, INHERITED_IDENTITY_JWT_VTM)
                        .claim(VC, vc)
                        .build();
        return createSignedJwt(claimsSet);
    }

    private static List<String> getValidVtrValues(String[] vtr) {
        Arrays.stream(vtr)
                .filter(value -> !allowedValues.contains(value))
                .peek(value -> logger.warn("Invalid VTR value ignored: " + value))
                .collect(Collectors.toList());

        return Arrays.stream(vtr).filter(allowedValues::contains).collect(Collectors.toList());
    }

    private static Map<String, Object> convertJsonToMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }

    private static Date generateExpirationTime(Instant now) {
        return Date.from(now.plus(Long.parseLong(INHERITED_IDENTITY_JWT_TTL), ChronoUnit.SECONDS));
    }

    public static SignedJWT createSignedJwt(JWTClaimsSet claims)
            throws JOSEException, NoSuchAlgorithmException, InvalidKeySpecException {
        JWSSigner signer = new ECDSASigner(getSigningKey());
        SignedJWT signedJwt = new SignedJWT(generateHeader(), claims);
        signedJwt.sign(signer);
        return signedJwt;
    }

    private static ECPrivateKey getSigningKey()
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] binaryKey = Base64.getDecoder().decode(INHERITED_IDENTITY_JWT_SIGNING_KEY);
        KeyFactory factory = KeyFactory.getInstance(EC_ALGO);
        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(binaryKey);
        return (ECPrivateKey) factory.generatePrivate(privateKeySpec);
    }

    private static JWSHeader generateHeader() {
        return new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build();
    }
}
