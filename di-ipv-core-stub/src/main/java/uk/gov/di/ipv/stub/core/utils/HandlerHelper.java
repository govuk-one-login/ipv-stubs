package uk.gov.di.ipv.stub.core.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.impl.ECDSA;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.JWTAuthenticationClaimsSet;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.utils.StringUtils;
import uk.gov.di.ipv.stub.core.config.CoreStubConfig;
import uk.gov.di.ipv.stub.core.config.credentialissuer.CredentialIssuer;
import uk.gov.di.ipv.stub.core.config.uatuser.EvidenceRequestClaims;
import uk.gov.di.ipv.stub.core.config.uatuser.Identity;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.nimbusds.jose.JWSAlgorithm.ES256;

public class HandlerHelper {
    private class JWTSigner {

        private final JWSSigner jwsSigner;
        private final String keyId;

        JWTSigner() throws JOSEException {
            this.keyId = ecSigningKey.getKeyID();
            this.jwsSigner = new ECDSASigner(ecSigningKey);
        }

        void signJWT(SignedJWT jwtToSign) throws JOSEException {
            jwtToSign.sign(this.jwsSigner);
        }

        String getKeyId() {
            return keyId;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(HandlerHelper.class);
    public static final String SHARED_CLAIMS = "shared_claims";
    private static final String PERSISTENT_SESSION_ID = "persistent_session_id";
    private static final String CLIENT_SESSION_ID = "govuk_signin_journey_id";
    public static final String UNKNOWN_ENV_VAR = "unknown";
    public static final String API_KEY_HEADER = "x-api-key";
    public static final String EVIDENCE_REQUESTED = "evidence_requested";
    public static final String CONTEXT = "context";

    private final ECKey ecSigningKey;
    private final ObjectMapper objectMapper;

    public HandlerHelper(ECKey ecSigningKey) {
        this.ecSigningKey = ecSigningKey;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public AuthorizationResponse getAuthorizationResponse(Request request)
            throws ParseException, JsonProcessingException {
        var authorizationResponse =
                AuthorizationResponse.parse(URI.create("https:///?" + request.queryString()));
        if (!authorizationResponse.indicatesSuccess()) {
            var error = authorizationResponse.toErrorResponse().getErrorObject();
            State state = request.session().attribute("state");
            AuthorizationErrorResponse authorizationErrorResponse =
                    new AuthorizationErrorResponse(
                            CoreStubConfig.CORE_STUB_REDIRECT_URL, error, state, null);
            request.session().removeAttribute("state");
            var errorResponse =
                    objectMapper
                            .writerWithDefaultPrettyPrinter()
                            .writeValueAsString(authorizationErrorResponse);
            throw new IllegalStateException(errorResponse);
        }
        return authorizationResponse;
    }

    public AccessToken exchangeCodeForToken(
            AuthorizationCode authorizationCode, CredentialIssuer credentialIssuer, State state)
            throws JOSEException {

        TokenRequest tokenRequest = createTokenRequest(authorizationCode, credentialIssuer);

        HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
        String apiKey =
                CoreStubConfig.getConfigValue(credentialIssuer.apiKeyEnvVar(), UNKNOWN_ENV_VAR);
        if (!apiKey.equals(UNKNOWN_ENV_VAR)) {
            LOGGER.info(
                    "Found api key and sending it in token request to cri: {}",
                    credentialIssuer.id());
            httpRequest.setHeader(API_KEY_HEADER, apiKey);
        } else {
            LOGGER.warn(
                    "Did not find api key for env var {}, not setting api key header {} in token request",
                    credentialIssuer.apiKeyEnvVar(),
                    API_KEY_HEADER);
        }

        LOGGER.info(
                "🤞 sending OAuth token request for state {} to {}", state, httpRequest.getURL());
        var httpTokenResponse = sendHttpRequest(httpRequest);
        TokenResponse tokenResponse = parseTokenResponse(httpTokenResponse);

        if (tokenResponse instanceof TokenErrorResponse) {
            TokenErrorResponse errorResponse = tokenResponse.toErrorResponse();
            LOGGER.error("Failed to get token: {}", errorResponse.getErrorObject());
            return null;
        }

        return tokenResponse.toSuccessResponse().getTokens().getAccessToken();
    }

    public TokenRequest createTokenRequest(
            AuthorizationCode authorizationCode, CredentialIssuer credentialIssuer)
            throws JOSEException {
        ClientID clientID = new ClientID(CoreStubConfig.CORE_STUB_CLIENT_ID);
        URI tokenURI = credentialIssuer.tokenUrl();

        LOGGER.info("token url is {}", tokenURI);

        AuthorizationCodeGrant authzGrant =
                new AuthorizationCodeGrant(
                        authorizationCode, CoreStubConfig.CORE_STUB_REDIRECT_URL);

        String hashedKid = getHashedKeyId(this.ecSigningKey.getKeyID());
        LOGGER.info("hashed KID is {}", hashedKid);

        PrivateKeyJWT privateKeyJWT =
                new PrivateKeyJWT(
                        new JWTAuthenticationClaimsSet(
                                clientID, new Audience(credentialIssuer.audience())),
                        JWSAlgorithm.ES256,
                        this.ecSigningKey.toECPrivateKey(),
                        hashedKid,
                        null);

        return new TokenRequest(tokenURI, privateKeyJWT, authzGrant);
    }

    public TokenResponse parseTokenResponse(HTTPResponse httpResponse) {
        try {
            return OIDCTokenResponseParser.parse(httpResponse);
        } catch (ParseException parseException) {
            LOGGER.error("Failed to parse token response");
            throw new RuntimeException("Failed to parse token response", parseException);
        }
    }

    public String getUserInfo(
            AccessToken accessToken, CredentialIssuer credentialIssuer, State state) {
        HTTPRequest userInfoRequest =
                new HTTPRequest(HTTPRequest.Method.POST, credentialIssuer.credentialUrl());

        userInfoRequest.setHeader("Content-Type", "");

        String apiKey =
                CoreStubConfig.getConfigValue(credentialIssuer.apiKeyEnvVar(), UNKNOWN_ENV_VAR);
        if (!apiKey.equals(UNKNOWN_ENV_VAR)) {
            LOGGER.info(
                    "Found api key and sending it in credential request to cri: {}",
                    credentialIssuer.id());
            userInfoRequest.setHeader(API_KEY_HEADER, apiKey);
        } else {
            LOGGER.warn(
                    "Did not find api key for env var {}, not setting api key header {} in credential request",
                    credentialIssuer.apiKeyEnvVar(),
                    API_KEY_HEADER);
        }

        userInfoRequest.setAuthorization(accessToken.toAuthorizationHeader());
        LOGGER.info(
                "🎁 sending OAuth credential issue for state {} to {}",
                state,
                userInfoRequest.getURL());
        HTTPResponse userInfoHttpResponse = sendHttpRequest(userInfoRequest);

        // Try lowercase, fall back to TitleCase
        String contentType = userInfoHttpResponse.getHeaderValue("content-type");
        contentType =
                contentType == null
                        ? userInfoHttpResponse.getHeaderValue("Content-Type")
                        : contentType;

        if (!"application/jwt".equalsIgnoreCase(contentType)) {
            // Fail now to prevent CRI's passing with a missmatch
            String message =
                    String.format(
                            "Expected content-type application/jwt but VC response had content-type - %s",
                            contentType);
            throw new RuntimeException(message);
        } else {
            return userInfoHttpResponse.getContent();
        }
    }

    public HTTPResponse sendHttpRequest(HTTPRequest httpRequest) {
        try {
            return httpRequest.send();
        } catch (IOException | SerializeException exception) {
            LOGGER.error("Failed to send a http request", exception);
            throw new RuntimeException("Failed to send a http request", exception);
        }
    }

    public <T> AuthorizationRequest createAuthorizationJAR(
            State state,
            CredentialIssuer credentialIssuer,
            T sharedClaims,
            EvidenceRequestClaims evidenceRequest,
            String context)
            throws JOSEException, java.text.ParseException {
        ClientID clientID = new ClientID(CoreStubConfig.CORE_STUB_CLIENT_ID);

        JWTClaimsSet claimsSet =
                createJWTClaimsSets(
                        state, credentialIssuer, clientID, sharedClaims, evidenceRequest, context);
        // The only difference (frontend/backend) are the ClaimSets are created above for the
        // frontend and clientID is already set in the backend ClaimSet
        LOGGER.info("ClaimsSets generated: {}", claimsSet);
        return createBackEndAuthorizationJAR(credentialIssuer, claimsSet);
    }

    public AuthorizationRequest createBackEndAuthorizationJAR(
            CredentialIssuer credentialIssuer, JWTClaimsSet claimsSet)
            throws java.text.ParseException, JOSEException {

        SignedJWT signedJWT = createSignedJWT(credentialIssuer, claimsSet);

        JWT encryptedJWT = encryptJWT(signedJWT, credentialIssuer);

        // Compose the final authorisation request, the minimal required query
        // parameters are "request" and "client_id"
        return new AuthorizationRequest.Builder(
                        encryptedJWT, new ClientID(claimsSet.getStringClaim("client_id")))
                .endpointURI(credentialIssuer.authorizeUrl())
                .build();
    }

    private SignedJWT createSignedJWT(CredentialIssuer credentialIssuer, JWTClaimsSet claimSets)
            throws JOSEException {
        JWSAlgorithm signingAlgorithm = JWSAlgorithm.parse(credentialIssuer.expectedAlgo());
        JWTSigner jwtSigner = new JWTSigner();
        String hashedKid = getHashedKeyId(jwtSigner.getKeyId());
        LOGGER.info("Hashed KID: {}", hashedKid);

        JWSHeader header = new JWSHeader.Builder(signingAlgorithm).keyID(hashedKid).build();

        SignedJWT signedJWT = new SignedJWT(header, claimSets);

        jwtSigner.signJWT(signedJWT);

        return signedJWT;
    }

    public JWTClaimsSet createJWTClaimsSets(
            State state,
            CredentialIssuer credentialIssuer,
            ClientID clientID,
            Object sharedClaims,
            EvidenceRequestClaims evidenceRequest,
            String context) {
        return createJWTClaimsSetBuilder(
                        state, credentialIssuer, clientID, sharedClaims, evidenceRequest, context)
                .build();
    }

    public JWTClaimsSet createJWTClaimsSets(
            State state,
            CredentialIssuer credentialIssuer,
            ClientID clientID,
            Object sharedClaims,
            EvidenceRequestClaims evidenceRequest) {
        return createJWTClaimsSetBuilder(
                        state, credentialIssuer, clientID, sharedClaims, evidenceRequest, null)
                .build();
    }

    public JWTClaimsSet createJWTClaimsSets(
            State state,
            CredentialIssuer credentialIssuer,
            ClientID clientID,
            Object sharedClaims) {
        return createJWTClaimsSetBuilder(
                        state, credentialIssuer, clientID, sharedClaims, null, null)
                .build();
    }

    private <T> JWTClaimsSet.Builder createJWTClaimsSetBuilder(
            State state,
            CredentialIssuer credentialIssuer,
            ClientID clientID,
            T sharedClaims,
            EvidenceRequestClaims evidenceRequest,
            String context) {

        Instant now = Instant.now();

        JWTClaimsSet authClaimsSet = createAuthorizationClaims(state, clientID);

        JWTClaimsSet.Builder claimsSetBuilder =
                new JWTClaimsSet.Builder(authClaimsSet)
                        .audience(credentialIssuer.audience().toString())
                        .issuer(CoreStubConfig.CORE_STUB_JWT_ISS_CRI_URI)
                        .issueTime(Date.from(now))
                        .expirationTime(
                                Date.from(
                                        now.plus(
                                                Integer.parseInt(CoreStubConfig.MAX_JAR_TTL_MINS),
                                                ChronoUnit.MINUTES)))
                        .notBeforeTime(Date.from(now))
                        .subject(getSubject())
                        .claim(PERSISTENT_SESSION_ID, UUID.randomUUID().toString())
                        .claim(CLIENT_SESSION_ID, UUID.randomUUID().toString());

        if (Objects.nonNull(sharedClaims)) {
            Map<String, Object> map = convertToMap(sharedClaims);
            claimsSetBuilder.claim(SHARED_CLAIMS, map);
        }
        if (Objects.nonNull(evidenceRequest)) {
            claimsSetBuilder.claim(EVIDENCE_REQUESTED, convertToMap(evidenceRequest));
        }
        if (!StringUtils.isEmpty(context)) {
            claimsSetBuilder.claim(CONTEXT, context);
        }
        return claimsSetBuilder;
    }

    private JWTClaimsSet createAuthorizationClaims(State state, ClientID clientID) {
        return new AuthorizationRequest.Builder(ResponseType.CODE, clientID)
                .redirectionURI(CoreStubConfig.CORE_STUB_REDIRECT_URL)
                .state(state)
                .build()
                .toJWTClaimsSet();
    }

    public CredentialIssuer findCredentialIssuer(String credentialIssuerId) {
        return CoreStubConfig.credentialIssuers.stream()
                .filter(cri -> credentialIssuerId.equals(cri.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("illegal cri"));
    }

    public List<Identity> findByName(String searchTerm) {
        if (StringUtils.isNotBlank(searchTerm)) {
            String[] parts = searchTerm.toLowerCase().split(" ");

            return CoreStubConfig.identities.stream()
                    .filter(
                            identity -> {
                                String name = identity.name().fullName().toLowerCase();
                                return Arrays.stream(parts).allMatch(term -> name.contains(term));
                            })
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    public Identity findIdentityByRowNumber(Integer rowNumber) {
        return CoreStubConfig.identities.stream()
                .filter(identity -> rowNumber == identity.rowNumber())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("unmatched rowNumber"));
    }

    public SignedJWT createSignedJWT(Object identity, CredentialIssuer credentialIssuer)
            throws JOSEException {
        Instant now = Instant.now();

        Map<String, Object> map = convertToMap(identity);

        JWSAlgorithm jwsSigningAlgorithm = JWSAlgorithm.parse(credentialIssuer.expectedAlgo());

        JWTSigner jwtSigner = new JWTSigner();

        SignedJWT signedJWT =
                new SignedJWT(
                        new JWSHeader.Builder(jwsSigningAlgorithm)
                                .keyID(jwtSigner.getKeyId())
                                .build(),
                        new JWTClaimsSet.Builder()
                                .subject(getSubject())
                                .audience(credentialIssuer.audience().toString())
                                .issueTime(Date.from(now))
                                .issuer(CoreStubConfig.CORE_STUB_JWT_ISS_CRI_URI)
                                .notBeforeTime(Date.from(now))
                                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                                .claim(SHARED_CLAIMS, map)
                                .build());

        jwtSigner.signJWT(signedJWT);

        return signedJWT;
    }

    private String getSubject() {
        // for more info, see di architecture RFC: 0027-subject-identifier-format.md
        return "urn:fdc:gov.uk:2022:" + UUID.randomUUID();
    }

    private Map<String, Object> convertToMap(Object input) {
        return this.objectMapper.convertValue(input, Map.class);
    }

    public EncryptedJWT encryptJWT(SignedJWT signedJWT, CredentialIssuer credentialIssuer) {
        try {
            JWEHeader.Builder headerBuilder =
                    new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                            .contentType("JWT");

            RSAKey rsaKey;
            String encryptionKeyId;
            boolean useKeyRotation = credentialIssuer.useKeyRotation();
            LOGGER.info("useKeyRotation is set to {}", useKeyRotation);
            if (useKeyRotation) {
                // automated key rotation path
                List<JWK> keys = JWKSet.parse(getJWKs(credentialIssuer)).getKeys();
                rsaKey = getLastEncryptionPublicKey(keys);

                // keyId added to JWE header for future keyID validation in CRI
                encryptionKeyId = rsaKey.getKeyID();
                LOGGER.info("hashed keyId from JWKS endpoint {}", encryptionKeyId);
                headerBuilder.keyID(encryptionKeyId);

            } else {

                String keyValueFromFile = credentialIssuer.publicEncryptionJwkBase64();
                rsaKey = getEncryptionPublicKey(keyValueFromFile);
            }

            JWEObject jweObject = new JWEObject(headerBuilder.build(), new Payload(signedJWT));

            encryptJWEObject(jweObject, rsaKey);

            return EncryptedJWT.parse(jweObject.serialize());

        } catch (java.text.ParseException e) {
            LOGGER.error("Error parsing JWT");
            throw new RuntimeException(e);
        }
    }

    private static void encryptJWEObject(JWEObject jweObject, RSAKey rsaKey) {
        try {
            jweObject.encrypt(new RSAEncrypter(rsaKey));
        } catch (JOSEException e) {
            LOGGER.error("Error during JWE encryption");
            throw new RuntimeException(e);
        }
    }

    public String getJWKs(CredentialIssuer credentialIssuer) {
        HTTPRequest jwksEndpointRequest =
                new HTTPRequest(HTTPRequest.Method.GET, credentialIssuer.jwksEndpoint());

        String apiKey =
                CoreStubConfig.getConfigValue(credentialIssuer.apiKeyEnvVar(), UNKNOWN_ENV_VAR);
        if (!apiKey.equals(UNKNOWN_ENV_VAR)) {
            LOGGER.info(
                    "Found api key and sending it in JWKS request to cri: {}",
                    credentialIssuer.id());
            jwksEndpointRequest.setHeader(API_KEY_HEADER, apiKey);
        } else {
            LOGGER.warn(
                    "Did not find api key for env var {}, not setting api key header {} in JWKS request",
                    credentialIssuer.apiKeyEnvVar(),
                    API_KEY_HEADER);
        }
        HTTPResponse jwksEndpointHttpResponse = sendHttpRequest(jwksEndpointRequest);
        return jwksEndpointHttpResponse.getBody();
    }

    private RSAKey getLastEncryptionPublicKey(List<JWK> keys) {
        if (keys.get(keys.size() - 1) instanceof RSAKey lastKey
                && lastKey.getKeyUse() == KeyUse.ENCRYPTION) {
            return lastKey;
        }
        throw new RuntimeException("Last key is not a RSA key");
    }

    private RSAKey getEncryptionPublicKey(String key) throws java.text.ParseException {
        return RSAKey.parse(new String(Base64.getDecoder().decode(key)));
    }

    public boolean checkES256SignatureFormat(SignedJWT signedJWT) throws JOSEException {
        return signedJWT.getSignature().decode().length == ECDSA.getSignatureByteArrayLength(ES256);
    }

    public boolean verifySignedJwt(SignedJWT signedJWT, CredentialIssuer credentialIssuer)
            throws JOSEException, java.text.ParseException {
        return signedJWT.verify(
                new ECDSAVerifier(
                        ECKey.parse(
                                base64Decode(
                                        credentialIssuer.publicVCSigningVerificationJwkBase64()))));
    }

    private String getHashedKeyId(String keyId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(keyId.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private String base64Decode(String value) {
        return new String(Base64.getDecoder().decode(value));
    }
}
