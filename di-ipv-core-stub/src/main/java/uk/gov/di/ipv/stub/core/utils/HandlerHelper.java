package uk.gov.di.ipv.stub.core.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
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
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.utils.StringUtils;
import uk.gov.di.ipv.stub.core.config.CoreStubConfig;
import uk.gov.di.ipv.stub.core.config.credentialissuer.CredentialIssuer;
import uk.gov.di.ipv.stub.core.config.uatuser.Identity;
import uk.gov.di.ipv.stub.core.config.uatuser.SharedClaims;

import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class HandlerHelper {
    private class JWTSigner {
        private static final String RS256_ALGORITHM_NAME = "RS256";
        private static final String ES256_ALGORITHM_NAME = "ES256";

        private final JWSSigner jwsSigner;
        private final String keyId;

        JWTSigner(JWSAlgorithm signingAlgorithm) throws JOSEException {
            switch (signingAlgorithm.getName()) {
                case RS256_ALGORITHM_NAME -> {
                    this.keyId = rsaSigningKey.getKeyID();
                    this.jwsSigner = new RSASSASigner(rsaSigningKey);
                }
                case ES256_ALGORITHM_NAME -> {
                    this.keyId = ecSigningKey.getKeyID();
                    this.jwsSigner = new ECDSASigner(ecSigningKey);
                }
                default -> throw new IllegalArgumentException(
                        "Unexpected signing algorithm encountered" + signingAlgorithm.getName());
            }
        }

        void signJWT(SignedJWT jwtToSign) throws JOSEException {
            jwtToSign.sign(this.jwsSigner);
        }

        String getKeyId() {
            return keyId;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(HandlerHelper.class);
    public static final String URN_UUID = "urn:uuid:";
    public static final String SHARED_CLAIMS = "shared_claims";

    private final RSAKey rsaSigningKey;
    private final ECKey ecSigningKey;
    private final ObjectMapper objectMapper;

    public HandlerHelper(RSAKey rsaSigningKey, ECKey ecSigningKey) {
        this.rsaSigningKey = rsaSigningKey;
        this.ecSigningKey = ecSigningKey;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
    }

    public AuthorizationResponse getAuthorizationResponse(Request request) throws ParseException {
        var authorizationResponse =
                AuthorizationResponse.parse(URI.create("https:///?" + request.queryString()));
        if (!authorizationResponse.indicatesSuccess()) {
            var error = authorizationResponse.toErrorResponse().getErrorObject();
            LOGGER.error("Failed authorization code request: {}", error);
            throw new RuntimeException("Failed authorization code request");
        }
        return authorizationResponse;
    }

    public AccessToken exchangeCodeForToken(
            AuthorizationCode authorizationCode, CredentialIssuer credentialIssuer)
            throws JOSEException {

        ClientID clientID = new ClientID(CoreStubConfig.CORE_STUB_CLIENT_ID);
        URI tokenURI = credentialIssuer.tokenUrl();

        LOGGER.info("token url is {}", tokenURI);

        AuthorizationCodeGrant authzGrant =
                new AuthorizationCodeGrant(
                        authorizationCode, CoreStubConfig.CORE_STUB_REDIRECT_URL);

        PrivateKeyJWT privateKeyJWT =
                switch (credentialIssuer.expectedAlgo()) {
                    case JWTSigner.RS256_ALGORITHM_NAME -> new PrivateKeyJWT(
                            new JWTAuthenticationClaimsSet(
                                    clientID, new Audience(credentialIssuer.audience())),
                            JWSAlgorithm.RS256,
                            this.rsaSigningKey.toRSAPrivateKey(),
                            this.rsaSigningKey.getKeyID(),
                            null);
                    case JWTSigner.ES256_ALGORITHM_NAME -> new PrivateKeyJWT(
                            new JWTAuthenticationClaimsSet(
                                    clientID, new Audience(credentialIssuer.audience())),
                            JWSAlgorithm.ES256,
                            this.ecSigningKey.toECPrivateKey(),
                            this.ecSigningKey.getKeyID(),
                            null);
                    default -> throw new IllegalArgumentException(
                            String.format(
                                    "Signing algorithm not supported: %s",
                                    credentialIssuer.expectedAlgo()));
                };

        TokenRequest tokenRequest = new TokenRequest(tokenURI, privateKeyJWT, authzGrant);

        var httpTokenResponse = sendHttpRequest(tokenRequest.toHTTPRequest());
        TokenResponse tokenResponse = parseTokenResponse(httpTokenResponse);

        if (tokenResponse instanceof TokenErrorResponse) {
            TokenErrorResponse errorResponse = tokenResponse.toErrorResponse();
            LOGGER.error("Failed to get token: {}", errorResponse.getErrorObject());
            return null;
        }

        return tokenResponse.toSuccessResponse().getTokens().getAccessToken();
    }

    public TokenResponse parseTokenResponse(HTTPResponse httpResponse) {
        try {
            return OIDCTokenResponseParser.parse(httpResponse);
        } catch (ParseException parseException) {
            LOGGER.error("Failed to parse token response");
            throw new RuntimeException("Failed to parse token response", parseException);
        }
    }

    public String getUserInfo(AccessToken accessToken, CredentialIssuer credentialIssuer) {
        HTTPRequest userInfoRequest =
                new HTTPRequest(HTTPRequest.Method.POST, credentialIssuer.credentialUrl());
        userInfoRequest.setAuthorization(accessToken.toAuthorizationHeader());
        HTTPResponse userInfoHttpResponse = sendHttpRequest(userInfoRequest);
        return userInfoHttpResponse.getContent();
    }

    public HTTPResponse sendHttpRequest(HTTPRequest httpRequest) {
        try {
            return httpRequest.send();
        } catch (IOException | SerializeException exception) {
            LOGGER.error("Failed to send a http request", exception);
            throw new RuntimeException("Failed to send a http request", exception);
        }
    }

    public AuthorizationRequest createAuthorizationRequest(
            State state, CredentialIssuer credentialIssuer, SignedJWT jwt) {
        return new AuthorizationRequest.Builder(
                        new ResponseType(ResponseType.Value.CODE),
                        new ClientID(CoreStubConfig.CORE_STUB_CLIENT_ID))
                .state(state)
                .scope(new Scope("openid"))
                .redirectionURI(CoreStubConfig.CORE_STUB_REDIRECT_URL)
                .customParameter("request", jwt.serialize())
                .endpointURI(credentialIssuer.authorizeUrl())
                .build();
    }

    public AuthorizationRequest createAuthorizationJAR(
            State state, CredentialIssuer credentialIssuer, SharedClaims sharedClaims)
            throws JOSEException {
        Instant now = Instant.now();
        ClientID clientID = new ClientID(CoreStubConfig.CORE_STUB_CLIENT_ID);
        JWSAlgorithm signingAlgorithm = JWSAlgorithm.parse(credentialIssuer.expectedAlgo());
        JWTSigner jwtSigner = new JWTSigner(signingAlgorithm);
        JWSHeader header =
                new JWSHeader.Builder(signingAlgorithm).keyID(jwtSigner.getKeyId()).build();

        JWTClaimsSet authClaimsSet =
                new AuthorizationRequest.Builder(ResponseType.CODE, clientID)
                        .redirectionURI(CoreStubConfig.CORE_STUB_REDIRECT_URL)
                        .scope(new Scope("openid"))
                        .state(state)
                        .build()
                        .toJWTClaimsSet();

        JWTClaimsSet.Builder claimsSetBuilder =
                new JWTClaimsSet.Builder(authClaimsSet)
                        .audience(credentialIssuer.audience().toString())
                        .issuer(CoreStubConfig.CORE_STUB_JWT_ISS_CRI_URI)
                        .issueTime(Date.from(now))
                        .expirationTime(Date.from(now.plus(1L, ChronoUnit.HOURS)))
                        .notBeforeTime(Date.from(now))
                        .subject(getSubject());

        if (Objects.nonNull(sharedClaims)) {
            Map<String, Object> map = convertToMap(sharedClaims);
            claimsSetBuilder.claim(SHARED_CLAIMS, map);
        }

        SignedJWT signedJWT = new SignedJWT(header, claimsSetBuilder.build());
        jwtSigner.signJWT(signedJWT);

        JWT outputJWT =
                (credentialIssuer.sendEncryptedOAuthJAR())
                        ? encryptJWT(signedJWT, credentialIssuer)
                        : signedJWT;

        // Compose the final authorisation request, the minimal required query
        // parameters are "request" and "client_id"
        return new AuthorizationRequest.Builder(outputJWT, clientID)
                .endpointURI(credentialIssuer.authorizeUrl())
                .build();
    }

    public CredentialIssuer findCredentialIssuer(String credentialIssuerId) {
        return CoreStubConfig.credentialIssuers.stream()
                .filter(cri -> credentialIssuerId.equals(cri.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("illegal cri"));
    }

    public List<Identity> findByName(String searchTerm) {
        if (StringUtils.isNotBlank(searchTerm)) {
            return CoreStubConfig.identities.stream()
                    .filter(
                            identity ->
                                    identity.name()
                                            .firstLastName()
                                            .toLowerCase()
                                            .contains(searchTerm.toLowerCase()))
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

        JWTSigner jwtSigner = new JWTSigner(jwsSigningAlgorithm);

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
        return URN_UUID + UUID.randomUUID();
    }

    private Map<String, Object> convertToMap(Object input) {
        return this.objectMapper.convertValue(input, Map.class);
    }

    private EncryptedJWT encryptJWT(SignedJWT signedJWT, CredentialIssuer credentialIssuer) {
        try {
            JWEObject jweObject =
                    new JWEObject(
                            new JWEHeader.Builder(
                                            JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                                    .contentType("JWT")
                                    .build(),
                            new Payload(signedJWT));
            jweObject.encrypt(new RSAEncrypter(getEncryptionPublicKey(credentialIssuer)));

            return EncryptedJWT.parse(jweObject.serialize());
        } catch (JOSEException | java.text.ParseException e) {
            throw new RuntimeException("JWT encryption failed", e);
        }
    }

    private RSAKey getEncryptionPublicKey(CredentialIssuer credentialIssuer)
            throws java.text.ParseException {
        return RSAKey.parse(
                new String(
                        Base64.getDecoder().decode(credentialIssuer.publicEncryptionJwkBase64())));
    }
}
