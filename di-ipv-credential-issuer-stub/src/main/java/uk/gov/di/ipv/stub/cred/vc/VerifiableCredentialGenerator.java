package uk.gov.di.ipv.stub.cred.vc;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.domain.Credential;
import uk.gov.di.ipv.stub.cred.service.ConfigService;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

import static com.nimbusds.jwt.JWTClaimNames.AUDIENCE;
import static com.nimbusds.jwt.JWTClaimNames.ISSUER;
import static com.nimbusds.jwt.JWTClaimNames.JWT_ID;
import static com.nimbusds.jwt.JWTClaimNames.NOT_BEFORE;
import static com.nimbusds.jwt.JWTClaimNames.SUBJECT;
import static uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig.getCriType;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.ADDRESS_CREDENTIAL_TYPE;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.CREDENTIAL_SUBJECT_ADDRESS;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.CREDENTIAL_SUBJECT_BIRTH_DATE;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.CREDENTIAL_SUBJECT_NAME;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.IDENTITY_ASSERTION_CREDENTIAL_TYPE;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.IDENTITY_CHECK_CREDENTIAL_TYPE;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.VC_CLAIM;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.VC_CREDENTIAL_SUBJECT;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.VC_EVIDENCE;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.VC_TYPE;
import static uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialConstants.VERIFIABLE_CREDENTIAL_TYPE;

public class VerifiableCredentialGenerator {
    static final String JTI_SCHEME_AND_PATH_PREFIX = "urn:uuid";

    public SignedJWT generate(Credential credential)
            throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {

        Map<String, Object> vc = new LinkedHashMap<>();
        vc.put(VC_TYPE, getVcType());

        Map<String, Object> credentialSubject = new LinkedHashMap<>();

        Map<String, Object> attributes = new HashMap<>(credential.credentialSubject());

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
        if (getCriType().isIdentityCheck()) {
            vc.put(VC_EVIDENCE, List.of(credential.evidence()));
        }

        return generateAndSignVerifiableCredentialJwt(credential, vc);
    }

    private List<String> getVcType() {
        return switch (getCriType()) {
            case USER_ASSERTED_CRI_TYPE ->
                    List.of(VERIFIABLE_CREDENTIAL_TYPE, IDENTITY_ASSERTION_CREDENTIAL_TYPE);
            case ADDRESS_CRI_TYPE -> List.of(VERIFIABLE_CREDENTIAL_TYPE, ADDRESS_CREDENTIAL_TYPE);
            default -> List.of(VERIFIABLE_CREDENTIAL_TYPE, IDENTITY_CHECK_CREDENTIAL_TYPE);
        };
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
        JWTClaimsSet.Builder claim =
                new JWTClaimsSet.Builder()
                        .claim(SUBJECT, credential.userId())
                        .claim(ISSUER, CredentialIssuerConfig.getVerifiableCredentialIssuer())
                        .claim(
                                AUDIENCE,
                                ConfigService.getClientConfig(credential.clientId())
                                        .getAudienceForVcJwt())
                        .claim(NOT_BEFORE, credential.nbf())
                        .claim(
                                JWT_ID,
                                String.format(
                                        "%s:%s", JTI_SCHEME_AND_PATH_PREFIX, UUID.randomUUID()))
                        .claim(VC_CLAIM, vc);

        JWTClaimsSet claimsSet = claim.build();

        return signTestVc(claimsSet);
    }

    private static SignedJWT signTestVc(JWTClaimsSet claimsSet)
            throws InvalidKeySpecException, NoSuchAlgorithmException, JOSEException {
        var signingAlgorithm = CredentialIssuerConfig.getVerifiableCredentialSigningAlgorithm();
        var signingKey = CredentialIssuerConfig.getVerifiableCredentialSigningKey();

        return switch (signingAlgorithm) {
            case EC -> signTestVcWithEc(claimsSet, signingKey);
            case RSA -> signTestVcWithRSA(claimsSet, signingKey);
        };
    }

    private static SignedJWT signTestVcWithEc(JWTClaimsSet claimsSet, String signingKey)
            throws InvalidKeySpecException, NoSuchAlgorithmException, JOSEException {
        var kf = KeyFactory.getInstance(EncryptionAlgorithm.EC.name());
        var privateKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(signingKey));
        var ecdsaSigner = new ECDSASigner((ECPrivateKey) kf.generatePrivate(privateKeySpec));

        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build();

        var signedJWT = new SignedJWT(jwsHeader, claimsSet);
        signedJWT.sign(ecdsaSigner);

        return signedJWT;
    }

    private static SignedJWT signTestVcWithRSA(JWTClaimsSet claimsSet, String signingKey)
            throws InvalidKeySpecException, NoSuchAlgorithmException, JOSEException {
        var kf = KeyFactory.getInstance(EncryptionAlgorithm.RSA.name());
        var privateKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(signingKey));
        var signer = new RSASSASigner(kf.generatePrivate(privateKeySpec));

        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build();

        var signedJWT = new SignedJWT(jwsHeader, claimsSet);
        signedJWT.sign(signer);

        return signedJWT;
    }
}
