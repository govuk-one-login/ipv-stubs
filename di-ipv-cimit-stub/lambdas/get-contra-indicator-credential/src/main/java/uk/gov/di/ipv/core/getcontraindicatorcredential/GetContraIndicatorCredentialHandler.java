package uk.gov.di.ipv.core.getcontraindicatorcredential;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringMapMessage;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.GetCiCredentialRequest;
import uk.gov.di.ipv.core.library.service.ConfigService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

import static uk.gov.di.ipv.core.library.config.EnvironmentVariable.CIMIT_COMPONENT_ID;

public class GetContraIndicatorCredentialHandler implements RequestStreamHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String TYPE = "type";
    private static final String SECURITY_CHECK_CREDENTIAL_VC_TYPE = "SecurityCheckCredential";
    private static final String VC_EVIDENCE = "evidence";
    private static final String VC = "vc";

    private static final ObjectMapper mapper = new ObjectMapper();
    public static final String FAILURE_RESPONSE = "Failure";
    private final ConfigService configService;

    public GetContraIndicatorCredentialHandler() {
        this.configService = new ConfigService();
    }

    public GetContraIndicatorCredentialHandler(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context)
            throws IOException {
        LOGGER.info(
                new StringMapMessage().with("Function invoked:", "GetContraIndicatorCredential"));
        GetCiCredentialRequest event = null;
        String response = null;
        try {
            event = mapper.readValue(input, GetCiCredentialRequest.class);
        } catch (Exception e) {
            LOGGER.error(
                    new StringMapMessage().with("Unable to parse input request", e.getMessage()));
            response = FAILURE_RESPONSE;
        }

        if (response == null || !response.equals(FAILURE_RESPONSE)) {
            SignedJWT signedJWT;
            try {
                signedJWT = generateJWT(getValidClaimsSetValues(event.getUserId()));
                response = signedJWT.serialize();
            } catch (Exception ex) {
                LOGGER.error(
                        new StringMapMessage()
                                .with(
                                        "errorDescription",
                                        "Failed at stub during creation of signedJwt. Error message:"
                                                + ex.getMessage()));
                response = FAILURE_RESPONSE;
            }
        }
        mapper.writeValue(output, response);
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
        var EC_PRIVATE_KEY = configService.getCimitSigningKey();
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

    private Map<String, Object> getValidClaimsSetValues(String userId) {
        return Map.of(
                JWTClaimNames.SUBJECT,
                userId,
                JWTClaimNames.ISSUER,
                configService.getEnvironmentVariable(CIMIT_COMPONENT_ID),
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
        vc.put(TYPE, new String[] {SECURITY_CHECK_CREDENTIAL_VC_TYPE});
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put(TYPE, "SecurityCheck");
        vc.put(VC_EVIDENCE, List.of(evidence));
        return vc;
    }
}
