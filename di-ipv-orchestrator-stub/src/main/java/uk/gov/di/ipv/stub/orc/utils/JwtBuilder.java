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
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.stub.orc.exceptions.JWSCreationException;
import uk.gov.di.ipv.stub.orc.models.JarClaims;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.AUTH_SIGNING_JWK;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_CORE_AUDIENCE;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_BUILD_JAR_ENCRYPTION_PUBLIC_JWK;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_CLIENT_ID;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_CLIENT_JWT_TTL;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_DEFAULT_JAR_ENCRYPTION_PUBLIC_JWK;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_DEV_JAR_ENCRYPTION_PUBLIC_JWK;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_INTEGRATION_JAR_ENCRYPTION_PUBLIC_JWK;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_REDIRECT_URL;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_SIGNING_JWK;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_STAGING_JAR_ENCRYPTION_PUBLIC_JWK;

public class JwtBuilder {

    public static final String URN_UUID = "urn:uuid:";
    public static final String INVALID_AUDIENCE = "invalid-audience";
    public static final String INVALID_REDIRECT_URI = "invalid-redirect-uri";
    public static final String INVALID_INHERITED_ID = "invalid-jwt";
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtBuilder.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JWSHeader ORCH_JWS_HEADER = createHeader(ORCHESTRATOR_SIGNING_JWK);
    private static final JWSHeader AUTH_JWS_HEADER = createHeader(AUTH_SIGNING_JWK);
    private static final JWSSigner ORCH_SIGNER = createSigner(ORCHESTRATOR_SIGNING_JWK);
    private static final JWSSigner AUTH_SIGNER = createSigner(AUTH_SIGNING_JWK);

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
                    JsonProcessingException, ParseException {
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
                OBJECT_MAPPER.convertValue(jarClaims, new TypeReference<Map<String, Object>>() {});

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

    public static SignedJWT createSignedJwt(JWTClaimsSet claims, boolean isAuth)
            throws JOSEException, NoSuchAlgorithmException, InvalidKeySpecException {
        var signedJwt = new SignedJWT(isAuth ? AUTH_JWS_HEADER : ORCH_JWS_HEADER, claims);
        signedJwt.sign(isAuth ? AUTH_SIGNER : ORCH_SIGNER);
        return signedJwt;
    }

    public static EncryptedJWT encryptJwt(SignedJWT signedJwt, String targetEnvironment)
            throws ParseException, JOSEException {
        var encryptionKey = getEncryptionKey(targetEnvironment);
        JWEObject jweObject =
                new JWEObject(
                        new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                                .contentType("JWT")
                                .keyID(encryptionKey.getKeyID())
                                .build(),
                        new Payload(signedJwt));
        jweObject.encrypt(new RSAEncrypter(encryptionKey));
        return EncryptedJWT.parse(jweObject.serialize());
    }

    private static RSAKey getEncryptionKey(String targetEnvironment) throws ParseException {
        return switch (targetEnvironment) {
            case ("DEV") -> RSAKey.parse(ORCHESTRATOR_DEV_JAR_ENCRYPTION_PUBLIC_JWK);
            case ("BUILD") -> RSAKey.parse(ORCHESTRATOR_BUILD_JAR_ENCRYPTION_PUBLIC_JWK);
            case ("STAGING") -> RSAKey.parse(ORCHESTRATOR_STAGING_JAR_ENCRYPTION_PUBLIC_JWK);
            case ("INTEGRATION") -> RSAKey.parse(
                    ORCHESTRATOR_INTEGRATION_JAR_ENCRYPTION_PUBLIC_JWK);
            default -> RSAKey.parse(ORCHESTRATOR_DEFAULT_JAR_ENCRYPTION_PUBLIC_JWK);
        };
    }

    private static String getIpvCoreAudience(String targetEnvironment) {
        return switch (targetEnvironment) {
            case ("DEV") -> "https://dev.01.dev.identity.account.gov.uk/";
            case ("BUILD") -> "https://identity.build.account.gov.uk";
            case ("STAGING") -> "https://identity.staging.account.gov.uk";
            case ("INTEGRATION") -> "https://identity.integration.account.gov.uk";
            default -> IPV_CORE_AUDIENCE;
        };
    }

    private static Date generateExpirationTime(Instant now) {
        return Date.from(now.plus(Long.parseLong(ORCHESTRATOR_CLIENT_JWT_TTL), ChronoUnit.SECONDS));
    }

    private static JWSHeader createHeader(String keyMaterial) {
        try {
            return new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(JOSEObjectType.JWT)
                    .keyID(ECKey.parse(keyMaterial).getKeyID())
                    .build();
        } catch (ParseException e) {
            throw new JWSCreationException(e);
        }
    }

    private static JWSSigner createSigner(String keyMaterial) {
        try {
            return new ECDSASigner(ECKey.parse(keyMaterial));
        } catch (JOSEException | ParseException e) {
            LOGGER.error("Failed to create JWT signer");
            throw new JWSCreationException(e);
        }
    }
}
