package uk.gov.di.ipv.stub.cred.vc;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.config.CriType;
import uk.gov.di.ipv.stub.cred.domain.Credential;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.*;

import static com.nimbusds.jwt.JWTClaimNames.AUDIENCE;
import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static com.nimbusds.jwt.JWTClaimNames.ISSUER;
import static com.nimbusds.jwt.JWTClaimNames.JWT_ID;
import static com.nimbusds.jwt.JWTClaimNames.NOT_BEFORE;
import static com.nimbusds.jwt.JWTClaimNames.SUBJECT;
import static uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig.getCriType;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.CREDENTIAL_SUBJECT_ADDRESS;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.CREDENTIAL_SUBJECT_BIRTH_DATE;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.CREDENTIAL_SUBJECT_NAME;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.IDENTITY_CHECK_CREDENTIAL_TYPE;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.VC_CLAIM;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.VC_CREDENTIAL_SUBJECT;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.VC_EVIDENCE;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.VC_TYPE;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.VERIFIABLE_CREDENTIAL_TYPE;

public class VerifiableCredentialGenerator {

    public static final String EC_ALGO = "EC";

    public SignedJWT generate(Credential credential)
            throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {

        Map<String, Object> vc = new LinkedHashMap<>();
        vc.put(VC_TYPE, new String[] {VERIFIABLE_CREDENTIAL_TYPE, IDENTITY_CHECK_CREDENTIAL_TYPE});

        Map<String, Object> credentialSubject = new LinkedHashMap<>();

        Map<String, Object> attributes = new HashMap<>(credential.getAttributes());

        if (isPopulatedList(attributes, CREDENTIAL_SUBJECT_NAME)) {
            credentialSubject.put(CREDENTIAL_SUBJECT_NAME, attributes.get(CREDENTIAL_SUBJECT_NAME));
            attributes.remove(CREDENTIAL_SUBJECT_NAME);
        }

        if (isPopulatedList(attributes, CREDENTIAL_SUBJECT_BIRTH_DATE)) {
            credentialSubject.put(
                    CREDENTIAL_SUBJECT_BIRTH_DATE, attributes.get(CREDENTIAL_SUBJECT_BIRTH_DATE));
            attributes.remove(CREDENTIAL_SUBJECT_BIRTH_DATE);
        }

        if (isPopulatedList(attributes, CREDENTIAL_SUBJECT_ADDRESS)) {
            credentialSubject.put(
                    CREDENTIAL_SUBJECT_ADDRESS, attributes.get(CREDENTIAL_SUBJECT_ADDRESS));
            attributes.remove(CREDENTIAL_SUBJECT_ADDRESS);
        }
        // Copy any remaining attributes in. The JSON manually entered into the stub.
        credentialSubject.putAll(attributes);
        vc.put(VC_CREDENTIAL_SUBJECT, credentialSubject);

        // VCs from user asserted CRI types, like address, should not contain an evidence attribute
        if (getCriType() != CriType.USER_ASSERTED_CRI_TYPE) {
            vc.put(VC_EVIDENCE, List.of(credential.getEvidence()));
        }

        return generateAndSignVerifiableCredentialJwt(credential, vc);
    }

    private boolean isPopulatedList(Map<String, Object> attributes, String attributeName) {
        if (attributes.get(attributeName) instanceof List
                && !((List<?>) attributes.get(attributeName)).isEmpty()) {
            return true;
        } else {
            // in case of empty list
            attributes.remove(attributeName);
            return false;
        }
    }

    private SignedJWT generateAndSignVerifiableCredentialJwt(
            Credential credential, Map<String, Object> vc)
            throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        Instant now = Instant.now();
        JWTClaimsSet.Builder claim =
                new JWTClaimsSet.Builder()
                        .claim(SUBJECT, credential.getUserId())
                        .claim(ISSUER, CredentialIssuerConfig.getVerifiableCredentialIssuer())
                        .claim(
                                AUDIENCE,
                                CredentialIssuerConfig.getClientConfig(credential.getClientId())
                                        .getAudienceForVcJwt())
                        .claim(NOT_BEFORE, now.getEpochSecond())
                        .claim(JWT_ID, UUID.randomUUID())
                        .claim(VC_CLAIM, vc);
        if (!Objects.isNull(credential.getExp())) {
            claim = claim.claim(EXPIRATION_TIME, credential.getExp());
        }
        JWTClaimsSet claimsSet = claim.build();

        KeyFactory kf = KeyFactory.getInstance(EC_ALGO);
        EncodedKeySpec privateKeySpec =
                new PKCS8EncodedKeySpec(
                        Base64.getDecoder()
                                .decode(
                                        CredentialIssuerConfig
                                                .getVerifiableCredentialSigningKey()));
        ECDSASigner ecdsaSigner =
                new ECDSASigner((ECPrivateKey) kf.generatePrivate(privateKeySpec));

        JWSHeader jwsHeader =
                new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build();

        SignedJWT signedJWT = new SignedJWT(jwsHeader, claimsSet);
        signedJWT.sign(ecdsaSigner);

        return signedJWT;
    }
}
