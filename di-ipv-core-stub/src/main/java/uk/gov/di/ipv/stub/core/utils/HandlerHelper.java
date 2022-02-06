package uk.gov.di.ipv.stub.core.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
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

import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


public class HandlerHelper {

    private final Logger logger = LoggerFactory.getLogger(HandlerHelper.class);

    public AuthorizationResponse getAuthorizationResponse(Request request) throws ParseException {
        var authorizationResponse = AuthorizationResponse.parse(URI.create("https:///?" + request.queryString()));
        if (!authorizationResponse.indicatesSuccess()) {
            var error = authorizationResponse.toErrorResponse().getErrorObject();
            logger.error("Failed authorization code request: {}", error);
            throw new RuntimeException("Failed authorization code request");
        }

        return authorizationResponse;
    }

    public AccessToken exchangeCodeForToken(AuthorizationCode authorizationCode, CredentialIssuer credentialIssuer) {
        URI resolve = credentialIssuer.tokenUrl();
        logger.info("token url is " + resolve);
        TokenRequest tokenRequest = new TokenRequest(
                resolve,
                new ClientID(CoreStubConfig.CORE_STUB_CLIENT_ID),
                new AuthorizationCodeGrant(authorizationCode, CoreStubConfig.CORE_STUB_REDIRECT_URL)
        );

        var httpTokenResponse = sendHttpRequest(tokenRequest.toHTTPRequest());
        TokenResponse tokenResponse = parseTokenResponse(httpTokenResponse);

        if (tokenResponse instanceof TokenErrorResponse) {
            TokenErrorResponse errorResponse = tokenResponse.toErrorResponse();
            logger.error("Failed to get token: " + errorResponse.getErrorObject());
            return null;
        }

        return tokenResponse
                .toSuccessResponse()
                .getTokens()
                .getAccessToken();
    }

    public TokenResponse parseTokenResponse(HTTPResponse httpResponse) {
        try {
            return OIDCTokenResponseParser.parse(httpResponse);
        } catch (ParseException parseException) {
            logger.error("Failed to parse token response");
            throw new RuntimeException("Failed to parse token response", parseException);
        }
    }


    public JSONObject getUserInfo(AccessToken accessToken, CredentialIssuer credentialIssuer) {
        var userInfoRequest = new UserInfoRequest(
                credentialIssuer.credentialUrl(),
                accessToken
        );

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
            logger.error("Failed to send a http request", exception);
            throw new RuntimeException("Failed to send a http request", exception);
        }
    }

    public AuthorizationRequest createAuthorizationRequest(State state, CredentialIssuer credentialIssuer, String jwt) {
        return new AuthorizationRequest.Builder(
                new ResponseType(ResponseType.Value.CODE), new ClientID(CoreStubConfig.CORE_STUB_CLIENT_ID))
                .state(state)
                .scope(new Scope("openid"))
                .redirectionURI(CoreStubConfig.CORE_STUB_REDIRECT_URL)
                .customParameter("request", jwt)
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
            return CoreStubConfig.identities.stream().filter(identity -> identity.name().firstLastName().toLowerCase().contains(searchTerm.toLowerCase())).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }


    public Identity findIdentityByRowNumber(Integer rowNumber) {
        return CoreStubConfig.identities.stream().filter(identity -> rowNumber == identity.rowNumber())
                .findFirst().orElseThrow(() -> new IllegalStateException("unmatched rowNumber"));
    }

    public String createClaimsJWT(Object identity, RSAKey signingPrivateKey) throws JOSEException {

        String ipv_session_id = UUID.randomUUID().toString();
        Instant now = Instant.now();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        Map<String, Object> map = objectMapper.convertValue(identity, Map.class);

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingPrivateKey.getKeyID()).build(),
                new JWTClaimsSet.Builder()
                        .subject(ipv_session_id)
                        .issueTime(Date.from(now))
                        .issuer(CoreStubConfig.CORE_STUB_CLIENT_ID)
                        .notBeforeTime(Date.from(now))
                        .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                        .claim("vc_http_api", map)
                        .build());

        signedJWT.sign(new RSASSASigner(signingPrivateKey));

        return signedJWT.serialize();
    }
}
