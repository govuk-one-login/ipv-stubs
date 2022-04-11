package uk.gov.di.ipv.stub.core.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
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
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import net.minidev.json.JSONObject;
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.di.ipv.stub.core.handlers.CoreStubHandler.ES256;
import static uk.gov.di.ipv.stub.core.handlers.CoreStubHandler.RS256;

public class HandlerHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(HandlerHelper.class);
    public static final String URN_UUID = "urn:uuid:";
    public static final String SHARED_CLAIMS = "shared_claims";

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
            AuthorizationCode authorizationCode,
            CredentialIssuer credentialIssuer,
            RSAKey rsaPrivateKey,
            ECKey ecPrivateKey)
            throws JOSEException {

        ClientID clientID = new ClientID(CoreStubConfig.CORE_STUB_CLIENT_ID);
        URI resolve = credentialIssuer.tokenUrl();

        LOGGER.info("token url is {}", resolve);

        AuthorizationCodeGrant authzGrant =
                new AuthorizationCodeGrant(
                        authorizationCode, CoreStubConfig.CORE_STUB_REDIRECT_URL);

        PrivateKeyJWT privateKeyJWT;
        switch (credentialIssuer.expectedAlgo()) {
            case RS256 -> {
                privateKeyJWT =
                        new PrivateKeyJWT(
                                clientID,
                                resolve,
                                JWSAlgorithm.RS256,
                                rsaPrivateKey.toRSAPrivateKey(),
                                rsaPrivateKey.getKeyID(),
                                null);
            }
            case ES256 -> {
                privateKeyJWT =
                        new PrivateKeyJWT(
                                clientID,
                                resolve,
                                JWSAlgorithm.ES256,
                                ecPrivateKey.toECPrivateKey(),
                                ecPrivateKey.getKeyID(),
                                null);
            }
            default -> throw new RuntimeException(
                    String.format(
                            "Signing algorithm not supported: %s",
                            credentialIssuer.expectedAlgo()));
        }

        TokenRequest tokenRequest = new TokenRequest(resolve, privateKeyJWT, authzGrant);

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

    public JSONObject getUserInfo(AccessToken accessToken, CredentialIssuer credentialIssuer) {
        // The CRIs userInfo endpoint should be post. Supporting GET for backwards compatability
        HTTPRequest.Method method = HTTPRequest.Method.GET;
        if (HTTPRequest.Method.POST.name().equals(credentialIssuer.userInfoRequestMethod())) {
            method = HTTPRequest.Method.POST;
        }
        var userInfoRequest =
                new UserInfoRequest(credentialIssuer.credentialUrl(), method, accessToken);

        HTTPResponse userInfoHttpResponse = sendHttpRequest(userInfoRequest.toHTTPRequest());

        try {
            return userInfoHttpResponse.getContentAsJSONObject();
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse user info response to JSON");
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
            State state,
            CredentialIssuer credentialIssuer,
            SharedClaims sharedClaims,
            RSAKey signingKey)
            throws JOSEException {
        Instant now = Instant.now();

        ClientID clientID = new ClientID(CoreStubConfig.CORE_STUB_CLIENT_ID);
        JWSHeader header =
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build();

        JWTClaimsSet authClaimsSet =
                new AuthorizationRequest.Builder(ResponseType.CODE, clientID)
                        .redirectionURI(CoreStubConfig.CORE_STUB_REDIRECT_URL)
                        .scope(new Scope("openid"))
                        .state(state)
                        .build()
                        .toJWTClaimsSet();

        JWTClaimsSet.Builder claimsSetBuilder =
                new JWTClaimsSet.Builder(authClaimsSet)
                        .audience(credentialIssuer.id())
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

        // Sign the JAR with client's private RSA JWK
        signedJWT.sign(new RSASSASigner(signingKey));

        // Compose the final authorisation request, the minimal required query
        // parameters are "request" and "client_id"
        return new AuthorizationRequest.Builder(signedJWT, clientID)
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

    public SignedJWT createRS256ClaimsJWT(Object identity, RSAKey signingPrivateKey)
            throws JOSEException {

        Instant now = Instant.now();

        Map<String, Object> map = convertToMap(identity);

        SignedJWT signedJWT =
                new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.RS256)
                                .keyID(signingPrivateKey.getKeyID())
                                .build(),
                        new JWTClaimsSet.Builder()
                                .subject(getSubject())
                                .audience(CoreStubConfig.CORE_STUB_JWT_AUD_EXPERIAN_CRI_URI)
                                .issueTime(Date.from(now))
                                .issuer(CoreStubConfig.CORE_STUB_JWT_ISS_CRI_URI)
                                .notBeforeTime(Date.from(now))
                                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                                .claim(SHARED_CLAIMS, map)
                                .build());

        signedJWT.sign(new RSASSASigner(signingPrivateKey));

        return signedJWT;
    }

    public SignedJWT createES256ClaimsJWT(Object identity, ECKey signingPrivateKey)
            throws JOSEException {

        String subjectIdentifier = URN_UUID + UUID.randomUUID();
        Instant now = Instant.now();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        Map<String, Object> map = objectMapper.convertValue(identity, Map.class);

        SignedJWT signedJWT =
                new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.ES256)
                                .keyID(signingPrivateKey.getKeyID())
                                .build(),
                        new JWTClaimsSet.Builder()
                                .subject(subjectIdentifier)
                                .audience(CoreStubConfig.CORE_STUB_JWT_AUD_EXPERIAN_CRI_URI)
                                .issueTime(Date.from(now))
                                .issuer(CoreStubConfig.CORE_STUB_JWT_ISS_CRI_URI)
                                .notBeforeTime(Date.from(now))
                                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                                .claim(SHARED_CLAIMS, map)
                                .build());

        signedJWT.sign(new ECDSASigner(signingPrivateKey));

        return signedJWT;
    }

    private String getSubject() {
        return URN_UUID + UUID.randomUUID();
    }

    private Map<String, Object> convertToMap(Object input) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        return objectMapper.convertValue(input, Map.class);
    }
}
