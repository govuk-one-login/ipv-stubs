package uk.gov.di.ipv.stub.orc.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.stub.orc.exceptions.JWSCreationException;
import uk.gov.di.ipv.stub.orc.exceptions.OrchestratorStubException;
import uk.gov.di.ipv.stub.orc.models.EvcsTokenRequest;
import uk.gov.di.ipv.stub.orc.models.EvcsTokenResponse;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.List;

import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.EVCS_ACCESS_TOKEN_ENDPOINT;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.EVCS_ACCESS_TOKEN_SIGNING_KEY_JWK;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.EVCS_ACCESS_TOKEN_TTL;

public class EvcsAccessTokenGenerator {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(EvcsAccessTokenGenerator.class);
    private static final List<String> EVCS_ENVIRONMENTS = List.of("STAGING", "INTEGRATION");
    private static final JWSHeader JWS_HEADER =
            new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build();
    private final ECDSASigner signer;

    public EvcsAccessTokenGenerator() {
        this.signer = createJwtSigner();
    }

    public String getAccessToken(String environment, String userId)
            throws OrchestratorStubException {
        if (EVCS_ENVIRONMENTS.contains(environment)) {
            LOGGER.info("Getting evcs access token from token generator");
            return getAccessTokenFromEndpoint(userId);
        }

        LOGGER.info("Generating evcs stub access token");
        return generateAccessToken(userId);
    }

    private String generateAccessToken(String userId) throws OrchestratorStubException {
        SignedJWT signedJWT =
                new SignedJWT(JWS_HEADER, new JWTClaimsSet.Builder().subject(userId).build());

        try {
            signedJWT.sign(signer);
        } catch (JOSEException e) {
            LOGGER.error("Failed to sign evcs access token");
            throw new OrchestratorStubException(e);
        }

        return signedJWT.serialize();
    }

    private String getAccessTokenFromEndpoint(String userId) throws OrchestratorStubException {
        var httpRequest =
                new HTTPRequest(HTTPRequest.Method.POST, URI.create(EVCS_ACCESS_TOKEN_ENDPOINT));

        try {
            httpRequest.setQuery(
                    OBJECT_MAPPER.writeValueAsString(
                            new EvcsTokenRequest(userId, Integer.parseInt(EVCS_ACCESS_TOKEN_TTL))));
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to create evcs token request");
            throw new OrchestratorStubException(e);
        }

        var response = sendHttpRequest(httpRequest);

        if (!response.indicatesSuccess()) {
            throw new OrchestratorStubException(
                    String.format(
                            "Failed to get access token - status code: %d",
                            response.getStatusCode()));
        }

        try {
            return OBJECT_MAPPER.readValue(response.getContent(), EvcsTokenResponse.class).token();
        } catch (IOException e) {
            LOGGER.error("Failed to read evcs token response");
            throw new OrchestratorStubException(e);
        }
    }

    private HTTPResponse sendHttpRequest(HTTPRequest request) throws OrchestratorStubException {
        try {
            return request.send();
        } catch (IOException e) {
            throw new OrchestratorStubException(e);
        }
    }

    private ECDSASigner createJwtSigner() {
        try {
            LOGGER.info(EVCS_ACCESS_TOKEN_SIGNING_KEY_JWK);
            return new ECDSASigner(ECKey.parse(EVCS_ACCESS_TOKEN_SIGNING_KEY_JWK).toECPrivateKey());
        } catch (ParseException | JOSEException e) {
            LOGGER.error("Failed to create jwt signer");
            throw new JWSCreationException(e);
        }
    }
}
