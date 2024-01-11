package uk.gov.di.ipv.stub.orc.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.stub.orc.models.InheritedIdentityJWT;
import uk.gov.di.ipv.stub.orc.models.UserInfo;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class HMRCUserInfoResponse {
    private static final org.slf4j.Logger logger =
            LoggerFactory.getLogger(HMRCUserInfoResponse.class);

    public static String generateResponse(
            String userId,
            String[] vtr,
            boolean duringMigration,
            String credentialSubject,
            String evidence)
            throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        UserInfo userInfo = new UserInfo();
        userInfo.setInheritedIdentityJWT(
                duringMigration
                        ? new InheritedIdentityJWT(
                                InheritedIdentityJWTBuilder.generate(
                                                userId, vtr, credentialSubject, evidence)
                                        .serialize())
                        : null);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(userInfo);
        } catch (Exception e) {
            logger.info("JSON Convert error: " + e.getMessage());
            return "{}";
        }
    }
}
