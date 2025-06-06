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
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.stub.orc.exceptions.JWSCreationException;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.INHERITED_IDENTITY_JWT_ISSUER;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.INHERITED_IDENTITY_JWT_SIGNING_JWK;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.INHERITED_IDENTITY_JWT_TTL;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.INHERITED_IDENTITY_JWT_VTM;

public class InheritedIdentityJwtBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(InheritedIdentityJwtBuilder.class);
    private static final String VC_CREDENTIAL_SUBJECT = "credentialSubject";
    private static final String VOT = "vot";
    private static final String VTM = "vtm";
    private static final String VC = "vc";
    private static final String VC_TYPE = "type";
    private static final String VERIFIABLE_CREDENTIAL_TYPE = "VerifiableCredential";
    private static final String IDENTITY_CHECK_CREDENTIAL_TYPE = "IdentityCheckCredential";
    private static final String VC_EVIDENCE = "evidence";
    private static final String JWT_ID = "jti";
    private static final String JTI_SCHEME_AND_PATH_PREFIX = "urn:uuid";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JWSHeader JWS_HEADER = createHeader();
    private static final JWSSigner SIGNER = createSigner();

    public static SignedJWT generate(
            String userId, String vot, String credentialSubject, String evidence)
            throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException,
                    JsonProcessingException, ParseException {

        Map<String, Object> vc = new LinkedHashMap<>();
        vc.put(VC_TYPE, new String[] {VERIFIABLE_CREDENTIAL_TYPE, IDENTITY_CHECK_CREDENTIAL_TYPE});
        vc.put(VC_CREDENTIAL_SUBJECT, convertJsonToMap(credentialSubject));
        vc.put(
                VC_EVIDENCE,
                !evidence.equals("{}")
                        ? List.of(convertJsonToMap(evidence))
                        : Collections.emptyList());
        Instant now = Instant.now();
        JWTClaimsSet claimsSet =
                new JWTClaimsSet.Builder()
                        .subject(userId)
                        .issuer(INHERITED_IDENTITY_JWT_ISSUER)
                        .notBeforeTime(Date.from(now))
                        .expirationTime(generateExpirationTime(now))
                        .claim(
                                JWT_ID,
                                String.format(
                                        "%s:%s", JTI_SCHEME_AND_PATH_PREFIX, UUID.randomUUID()))
                        .claim(VOT, vot)
                        .claim(VTM, INHERITED_IDENTITY_JWT_VTM)
                        .claim(VC, vc)
                        .build();
        return createSignedJwt(claimsSet);
    }

    private static Map<String, Object> convertJsonToMap(String json)
            throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
    }

    private static Date generateExpirationTime(Instant now) {
        return Date.from(now.plus(Long.parseLong(INHERITED_IDENTITY_JWT_TTL), ChronoUnit.SECONDS));
    }

    private static SignedJWT createSignedJwt(JWTClaimsSet claims) throws JOSEException {
        var signedJwt = new SignedJWT(JWS_HEADER, claims);
        signedJwt.sign(SIGNER);
        return signedJwt;
    }

    private static JWSHeader createHeader() {
        try {
            return new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(JOSEObjectType.JWT)
                    .keyID(ECKey.parse(INHERITED_IDENTITY_JWT_SIGNING_JWK).getKeyID())
                    .build();
        } catch (ParseException e) {
            throw new JWSCreationException(e);
        }
    }

    private static ECDSASigner createSigner() {
        try {
            return new ECDSASigner(ECKey.parse(INHERITED_IDENTITY_JWT_SIGNING_JWK));
        } catch (JOSEException | ParseException e) {
            LOGGER.error("Failed to create inherited identity signer");
            throw new JWSCreationException(e);
        }
    }
}
