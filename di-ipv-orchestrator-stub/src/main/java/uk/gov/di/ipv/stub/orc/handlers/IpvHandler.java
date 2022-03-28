package uk.gov.di.ipv.stub.orc.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.nimbusds.jose.JOSEException;
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
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.orc.exceptions.OrchestratorStubException;
import uk.gov.di.ipv.stub.orc.utils.JwtHelper;
import uk.gov.di.ipv.stub.orc.utils.ViewHelper;

import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_BACKCHANNEL_ENDPOINT;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_BACKCHANNEL_TOKEN_PATH;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_BACKCHANNEL_USER_IDENTITY_PATH;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_ENDPOINT;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_CLIENT_ID;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_REDIRECT_URL;

public class IpvHandler {

    private final Logger logger = LoggerFactory.getLogger(IpvHandler.class);
    private final Map<String, Object> stateSession = new HashMap<>();

    public Route doAuthorize =
            (Request request, Response response) -> {
                var state = new State();
                stateSession.put(state.getValue(), null);
                String journeyType = request.queryMap().get("journeyType").value();
                URI journeyTypeEndpointURI =
                        journeyType.equals("debug")
                                ? new URI(IPV_ENDPOINT).resolve("/oauth2/debug-authorize")
                                : new URI(IPV_ENDPOINT).resolve("/oauth2/authorize");
                var authRequest =
                        new AuthorizationRequest.Builder(
                                        new ResponseType(ResponseType.Value.CODE),
                                        new ClientID(ORCHESTRATOR_CLIENT_ID))
                                .state(state)
                                .scope(new Scope("openid"))
                                .redirectionURI(new URI(ORCHESTRATOR_REDIRECT_URL))
                                .endpointURI(journeyTypeEndpointURI)
                                .build();

                response.redirect(authRequest.toURI().toString());
                return null;
            };

    public Route doCallback =
            (Request request, Response response) -> {
                var authorizationCode = getAuthorizationCode(request);

                List<Map<String, Object>> mustacheData = new ArrayList<>();
                Map<String, Object> moustacheDataModel = new HashMap<>();

                try {
                    var accessToken = exchangeCodeForToken(authorizationCode);

                    var userInfo = getUserInfo(accessToken);

                    mustacheData = buildMustacheData(userInfo);
                    moustacheDataModel.put("data", mustacheData);
                } catch (OrchestratorStubException | ParseException | JsonSyntaxException e) {
                    moustacheDataModel.put("error", e.getMessage());
                }

                return ViewHelper.render(moustacheDataModel, "userinfo.mustache");
            };

    private AuthorizationCode getAuthorizationCode(Request request) throws ParseException {
        var authorizationResponse =
                AuthorizationResponse.parse(URI.create("https:///?" + request.queryString()));
        if (!authorizationResponse.indicatesSuccess()) {
            var error = authorizationResponse.toErrorResponse().getErrorObject();
            logger.error("Failed authorization code request: {}", error);
            throw new RuntimeException("Failed authorization code request");
        }

        return authorizationResponse.toSuccessResponse().getAuthorizationCode();
    }

    private AccessToken exchangeCodeForToken(AuthorizationCode authorizationCode)
            throws OrchestratorStubException, CertificateException, JOSEException {
        URI resolve = URI.create(IPV_BACKCHANNEL_ENDPOINT).resolve(IPV_BACKCHANNEL_TOKEN_PATH);
        logger.info("token url is " + resolve);

        SignedJWT signedClientJwt;
        try {
            signedClientJwt = JwtHelper.createSignedClientAuthJwt();
        } catch (JOSEException | InvalidKeySpecException | NoSuchAlgorithmException e) {
            logger.error("Failed to generate orch client JWT", e);
            throw new OrchestratorStubException("Failed to generate orch client JWT");
        }

        ClientAuthentication clientAuthentication = new PrivateKeyJWT(signedClientJwt);

        TokenRequest tokenRequest =
                new TokenRequest(
                        resolve,
                        clientAuthentication,
                        new AuthorizationCodeGrant(
                                authorizationCode, URI.create(ORCHESTRATOR_REDIRECT_URL)));

        var httpTokenResponse = sendHttpRequest(tokenRequest.toHTTPRequest());
        TokenResponse tokenResponse = parseTokenResponse(httpTokenResponse);

        if (tokenResponse instanceof TokenErrorResponse) {
            TokenErrorResponse errorResponse = tokenResponse.toErrorResponse();
            logger.error("Failed to get token: " + errorResponse.getErrorObject());
            throw new OrchestratorStubException(errorResponse.getErrorObject().getDescription());
        }

        return tokenResponse.toSuccessResponse().getTokens().getAccessToken();
    }

    public JSONObject getUserInfo(AccessToken accessToken) {
        var userInfoRequest =
                new UserInfoRequest(
                        URI.create(IPV_BACKCHANNEL_ENDPOINT)
                                .resolve(IPV_BACKCHANNEL_USER_IDENTITY_PATH),
                        (BearerAccessToken) accessToken);

        HTTPResponse userInfoHttpResponse = sendHttpRequest(userInfoRequest.toHTTPRequest());

        try {
            return userInfoHttpResponse.getContentAsJSONObject();
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse user info response to JSON");
        }
    }

    private List<Map<String, Object>> buildMustacheData(JSONObject credentials)
            throws ParseException, JsonSyntaxException, java.text.ParseException {
        List<Map<String, Object>> moustacheDataModel = new ArrayList<>();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        for (String key : credentials.keySet()) {
            SignedJWT signedJWT = SignedJWT.parse(credentials.get(key).toString());

            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            Map<String, Object> claims = claimsSet.toJSONObject();

            String json = gson.toJson(claims);

            Map<String, Object> criMap = new HashMap<>();
            criMap.put("VC", json);
            criMap.put("criType", key);
            moustacheDataModel.add(criMap);
        }
        return moustacheDataModel;
    }

    private HTTPResponse sendHttpRequest(HTTPRequest httpRequest) {
        try {
            return httpRequest.send();
        } catch (IOException | SerializeException exception) {
            logger.error("Failed to send a http request", exception);
            throw new RuntimeException("Failed to send a http request", exception);
        }
    }

    private TokenResponse parseTokenResponse(HTTPResponse httpResponse) {
        try {
            return OIDCTokenResponseParser.parse(httpResponse);
        } catch (ParseException parseException) {
            logger.error("Failed to parse token response");
            throw new RuntimeException("Failed to parse token response", parseException);
        }
    }

    private UserInfoResponse parseUserInfoResponse(HTTPResponse httpResponse) {
        try {
            return UserInfoResponse.parse(httpResponse);
        } catch (ParseException parseException) {
            logger.error("Failed to parse user info response");
            throw new RuntimeException("Failed to parse user info response", parseException);
        }
    }
}
