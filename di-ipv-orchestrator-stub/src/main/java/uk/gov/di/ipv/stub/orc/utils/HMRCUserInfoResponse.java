package uk.gov.di.ipv.stub.orc.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.JOSEException;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.stub.orc.models.InheritedIdentityJWT;
import uk.gov.di.ipv.stub.orc.models.UserInfo;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class HMRCUserInfoResponse {
    private static final org.slf4j.Logger logger =
            LoggerFactory.getLogger(HMRCUserInfoResponse.class);

    public static ObjectNode generateResponse(
            String userId,
            String vot,
            boolean duringMigration,
            String credentialSubject,
            String evidence)
            throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException,
                    JsonProcessingException {

        UserInfo userInfo = new UserInfo();
        userInfo.setInheritedIdentityJWT(
                duringMigration
                        ? new InheritedIdentityJWT(
                                InheritedIdentityJWTBuilder.generate(
                                                userId, vot, credentialSubject, evidence)
                                        .serialize())
                        : null);

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonObject = objectMapper.createObjectNode();
        try {
            jsonObject.set(
                    "userInfo", objectMapper.readTree(objectMapper.writeValueAsString(userInfo)));
            return jsonObject;
        } catch (Exception e) {
            logger.info("JSON Convert error: " + e.getMessage());
            return objectMapper.createObjectNode();
        }
    }
}
