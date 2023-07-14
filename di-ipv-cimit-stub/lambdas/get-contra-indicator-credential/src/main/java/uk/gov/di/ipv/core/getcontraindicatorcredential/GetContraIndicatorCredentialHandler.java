package uk.gov.di.ipv.core.getcontraindicatorcredential;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.GetCiCredentialRequest;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GetContraIndicatorCredentialHandler
        implements RequestHandler<GetCiCredentialRequest, String> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GetContraIndicatorCredentialHandler.class);
    private static final String TYPE = "type";
    private static final String VERIFIABLE_CREDENTIAL_TYPE = "VerifiableCredential";
    private static final String VC_EVIDENCE = "evidence";
    public static final String VC = "vc";

    @Override
    public String handleRequest(GetCiCredentialRequest event, Context context) {
        LOGGER.info("EVENT TYPE: " + event.getClass());
        SignedJWT signedJWT = null;
        try {
            signedJWT = generateJWT(getValidClaimsSetValues());
        } catch (Exception ex) {
            LOGGER.error(
                    "Failed at stub during creation of signedJwt. Error message:"
                            + ex.getMessage());
        }
        //        return signedJWT.serialize();
        return "Success";
    }

    private SignedJWT generateJWT(Map<String, Object> claimsSetValues)
            throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        ECDSASigner signer = new ECDSASigner(getPrivateKey());

        SignedJWT signedJWT =
                new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),
                        generateClaimsSet(claimsSetValues));
        signedJWT.sign(signer);

        return signedJWT;
    }

    private ECPrivateKey getPrivateKey() throws InvalidKeySpecException, NoSuchAlgorithmException {
        String EC_PRIVATE_KEY = "";
        return (ECPrivateKey)
                KeyFactory.getInstance("EC")
                        .generatePrivate(
                                new PKCS8EncodedKeySpec(
                                        Base64.getDecoder().decode(EC_PRIVATE_KEY)));
    }

    private JWTClaimsSet generateClaimsSet(Map<String, Object> claimsSetValues) {
        return new JWTClaimsSet.Builder()
                .claim(JWTClaimNames.SUBJECT, claimsSetValues.get(JWTClaimNames.SUBJECT))
                .claim(JWTClaimNames.ISSUER, claimsSetValues.get(JWTClaimNames.ISSUER))
                .claim(JWTClaimNames.NOT_BEFORE, claimsSetValues.get(JWTClaimNames.NOT_BEFORE))
                .claim(
                        JWTClaimNames.EXPIRATION_TIME,
                        claimsSetValues.get(JWTClaimNames.EXPIRATION_TIME))
                .claim(VC, claimsSetValues.get(VC))
                .build();
    }

    private Map<String, Object> getValidClaimsSetValues() {
        return Map.of(
                JWTClaimNames.SUBJECT,
                "sub",
                JWTClaimNames.ISSUED_AT,
                OffsetDateTime.now().toEpochSecond(),
                JWTClaimNames.NOT_BEFORE,
                OffsetDateTime.now().toEpochSecond(),
                JWTClaimNames.EXPIRATION_TIME,
                OffsetDateTime.now().plusSeconds(15 * 60).toEpochSecond(),
                VC,
                generateVC());
    }

    private Map<String, Object> generateVC() {
        Map<String, Object> vc = new LinkedHashMap<>();
        vc.put(TYPE, new String[] {VERIFIABLE_CREDENTIAL_TYPE});
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put(TYPE, "SecurityCheck");
        vc.put(VC_EVIDENCE, List.of(evidence));
        return vc;
    }
}
