package uk.gov.di.ipv.stub.orc.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.nimbusds.oauth2.sdk.Scope;
import uk.gov.di.ipv.stub.orc.models.JarClaims;

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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_CORE_AUDIENCE;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_BUILD_JAR_ENCRYPTION_PUBLIC_KEY;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_CLIENT_ID;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_CLIENT_JWT_TTL;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_CLIENT_SIGNING_KEY;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_DEFAULT_JAR_ENCRYPTION_PUBLIC_KEY;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_INTEGRATION_JAR_ENCRYPTION_PUBLIC_KEY;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_REDIRECT_URL;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_STAGING_JAR_ENCRYPTION_PUBLIC_KEY;

public class JwtBuilder {
    public static final String URN_UUID = "urn:uuid:";
    public static final String INVALID_AUDIENCE = "invalid-audience";
    public static final String INVALID_REDIRECT_URI = "invalid-redirect-uri";
    public static final String INVALID_INHERITED_ID = "invalid-jwt";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public enum ReproveIdentityClaimValue {
        NOT_PRESENT,
        TRUE,
        FALSE
    }

    public static JWTClaimsSet buildAuthorizationRequestClaims(
            String userId,
            String signInJourneyId,
            String state,
            List<String> vtr,
            String errorType,
            String userEmailAddress,
            ReproveIdentityClaimValue reproveIdentityValue,
            String environment,
            boolean includeInheritedId,
            String inheritedIdSubject,
            String inheritedIdEvidence,
            String inheritedIdVot,
            Scope scope,
            String clientId,
            String evcsAccessToken)
            throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException,
                    JsonProcessingException {
        String audience = getIpvCoreAudience(environment);
        String redirectUri = ORCHESTRATOR_REDIRECT_URL;

        var inheritedIdJwt =
                includeInheritedId
                        ? InheritedIdentityJwtBuilder.generate(
                                        userId,
                                        inheritedIdVot,
                                        inheritedIdSubject,
                                        inheritedIdEvidence)
                                .serialize()
                        : null;

        if (errorType != null) {
            switch (errorType) {
                case "recoverable" -> audience = INVALID_AUDIENCE;
                case "non-recoverable" -> redirectUri = INVALID_REDIRECT_URI;
                case "inherited-identity" -> inheritedIdJwt = INVALID_INHERITED_ID;
            }
        }

        var jarClaims = new JarClaims(inheritedIdJwt, evcsAccessToken);
        var jarClaimsMap =
                objectMapper.convertValue(jarClaims, new TypeReference<Map<String, Object>>() {});

        Instant now = Instant.now();
        var claimSetBuilder =
                new JWTClaimsSet.Builder()
                        .subject(userId)
                        .audience(audience)
                        .issueTime(Date.from(now))
                        .issuer(clientId)
                        .notBeforeTime(Date.from(now))
                        .expirationTime(generateExpirationTime(now))
                        .jwtID(UUID.randomUUID().toString())
                        .claim("claims", jarClaimsMap)
                        .claim("client_id", clientId)
                        .claim("response_type", ResponseType.Value.CODE.toString())
                        .claim("redirect_uri", redirectUri)
                        .claim("state", state)
                        .claim("govuk_signin_journey_id", signInJourneyId)
                        .claim("email_address", userEmailAddress)
                        .claim("vtr", vtr)
                        .claim("scope", scope.toString());
        if (reproveIdentityValue != ReproveIdentityClaimValue.NOT_PRESENT) {
            claimSetBuilder.claim(
                    "reprove_identity", reproveIdentityValue == ReproveIdentityClaimValue.TRUE);
        }
        return claimSetBuilder.build();
    }

    public static JWTClaimsSet buildClientAuthenticationClaims(String targetEnvironment) {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .subject(ORCHESTRATOR_CLIENT_ID)
                .audience(getIpvCoreAudience(targetEnvironment))
                .issuer(ORCHESTRATOR_CLIENT_ID)
                .expirationTime(generateExpirationTime(now))
                .jwtID(UUID.randomUUID().toString())
                .build();
    }

    public static SignedJWT createSignedJwt(JWTClaimsSet claims)
            throws JOSEException, NoSuchAlgorithmException, InvalidKeySpecException {
        JWSSigner signer = new ECDSASigner(getSigningKey());
        SignedJWT signedJwt = new SignedJWT(generateHeader(), claims);
        signedJwt.sign(signer);
        return signedJwt;
    }

    public static EncryptedJWT encryptJwt(SignedJWT signedJwt, String targetEnvironment)
            throws ParseException, JOSEException {
        JWEObject jweObject =
                new JWEObject(
                        new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                                .contentType("JWT")
                                .build(),
                        new Payload(signedJwt));
        jweObject.encrypt(new RSAEncrypter(getEncryptionKey(targetEnvironment)));
        return EncryptedJWT.parse(jweObject.serialize());
    }

    private static ECPrivateKey getSigningKey()
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] binaryKey = Base64.getDecoder().decode(ORCHESTRATOR_CLIENT_SIGNING_KEY);
        KeyFactory factory = KeyFactory.getInstance("EC");
        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(binaryKey);
        return (ECPrivateKey) factory.generatePrivate(privateKeySpec);
    }

    private static RSAPublicKey getEncryptionKey(String targetEnvironment)
            throws java.text.ParseException, JOSEException {
        String jarEncryptionPublicKey = getJarEncryptionPublicKey(targetEnvironment);

        byte[] binaryKey = Base64.getDecoder().decode(jarEncryptionPublicKey);
        return RSAKey.parse(new String(binaryKey)).toRSAPublicKey();
    }

    private static String getJarEncryptionPublicKey(String targetEnvironment) {
        return switch (targetEnvironment) {
            case ("BUILD") -> ORCHESTRATOR_BUILD_JAR_ENCRYPTION_PUBLIC_KEY;
            case ("STAGING") -> ORCHESTRATOR_STAGING_JAR_ENCRYPTION_PUBLIC_KEY;
            case ("INTEGRATION") -> ORCHESTRATOR_INTEGRATION_JAR_ENCRYPTION_PUBLIC_KEY;
            default -> ORCHESTRATOR_DEFAULT_JAR_ENCRYPTION_PUBLIC_KEY;
        };
    }

    private static String getIpvCoreAudience(String targetEnvironment) {
        return switch (targetEnvironment) {
            case ("BUILD") -> "https://identity.build.account.gov.uk";
            case ("STAGING") -> "https://identity.staging.account.gov.uk";
            case ("INTEGRATION") -> "https://identity.integration.account.gov.uk";
            default -> IPV_CORE_AUDIENCE;
        };
    }

    private static JWSHeader generateHeader() {
        return new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build();
    }

    private static Date generateExpirationTime(Instant now) {
        return Date.from(now.plus(Long.parseLong(ORCHESTRATOR_CLIENT_JWT_TTL), ChronoUnit.SECONDS));
    }
}
