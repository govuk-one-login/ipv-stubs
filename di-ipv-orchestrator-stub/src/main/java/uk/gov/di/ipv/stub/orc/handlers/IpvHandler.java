package uk.gov.di.ipv.stub.orc.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
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
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.orc.utils.ViewHelper;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_BACKCHANNEL_ENDPOINT;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_BACKCHANNEL_TOKEN_PATH;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_BACKCHANNEL_USER_IDENTITY_PATH;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_CLIENT_ID;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_ENDPOINT;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_REDIRECT_URL;

public class IpvHandler {

    private final Logger logger = LoggerFactory.getLogger(IpvHandler.class);
    private final Map<String, Object> stateSession = new HashMap<>();

    public Route doAuthorize = (Request request, Response response) -> {
        var state = new State();
        stateSession.put(state.getValue(), null);

        var authRequest = new AuthorizationRequest.Builder(
                new ResponseType(ResponseType.Value.CODE), new ClientID(IPV_CLIENT_ID))
                .state(state)
                .scope(new Scope("openid"))
                .redirectionURI(new URI(ORCHESTRATOR_REDIRECT_URL))
                .endpointURI(new URI(IPV_ENDPOINT).resolve("/oauth2/authorize"))
                .build();

        response.redirect(authRequest.toURI().toString());
        return null;
    };

    public Route doCallback = (Request request, Response response) -> {
        var authorizationCode = getAuthorizationCode(request);

        var accessToken = exchangeCodeForToken(authorizationCode);

        var userInfo = getUserInfo(accessToken);
        List<Map<String, Object>> mustacheData = new ArrayList<>();
        Map<String, Object> moustacheDataModel = new HashMap<>();

        try {
            mustacheData = buildMustacheData(userInfo);
            moustacheDataModel.put("data", mustacheData);
        } catch (ParseException | JsonIOException e) {
            moustacheDataModel.put("error", userInfo.toJSONString());
        }

        return ViewHelper.render(moustacheDataModel, "userinfo.mustache");
    };

    private AuthorizationCode getAuthorizationCode(Request request) throws ParseException {
        var authorizationResponse = AuthorizationResponse.parse(URI.create("https:///?" + request.queryString()));
        if (!authorizationResponse.indicatesSuccess()) {
            var error = authorizationResponse.toErrorResponse().getErrorObject();
            logger.error("Failed authorization code request: {}", error);
            throw new RuntimeException("Failed authorization code request");
        }

        return authorizationResponse
                .toSuccessResponse()
                .getAuthorizationCode();
    }

    private AccessToken exchangeCodeForToken(AuthorizationCode authorizationCode) {
        URI resolve = URI.create(IPV_BACKCHANNEL_ENDPOINT).resolve(IPV_BACKCHANNEL_TOKEN_PATH);
        logger.info("token url is " + resolve);
        TokenRequest tokenRequest = new TokenRequest(
                resolve,
                new ClientID(IPV_CLIENT_ID),
                new AuthorizationCodeGrant(authorizationCode, URI.create(ORCHESTRATOR_REDIRECT_URL))
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

    public JSONObject getUserInfo(AccessToken accessToken) {
        var userInfoRequest = new UserInfoRequest(
                URI.create(IPV_BACKCHANNEL_ENDPOINT).resolve(IPV_BACKCHANNEL_USER_IDENTITY_PATH),
                (BearerAccessToken) accessToken
        );

        HTTPResponse userInfoHttpResponse = sendHttpRequest(userInfoRequest.toHTTPRequest());

        try {
            return userInfoHttpResponse.getContentAsJSONObject();
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse user info response to JSON");
        }
    }

    private List<Map<String, Object>> buildMustacheData(JSONObject credentials) throws ParseException {
        List<Map<String, Object>> moustacheDataModel = new ArrayList<>();

        for (String key : credentials.keySet()) {
            JSONObject criJson = JSONObjectUtils.getJSONObject(credentials, key);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            String attributesJson;
            if (JSONObjectUtils.containsKey(criJson, "attributes")) {
                attributesJson = gson.toJson(JsonParser.parseString(criJson.getAsString("attributes")));
            } else {
                throw new ParseException("Could not find attributes field in JSON");
            }

            String gpg45ScoreJson = null;
            if (JSONObjectUtils.containsKey(criJson, "gpg45Score")) {
                gpg45ScoreJson = gson.toJson(JsonParser.parseString(criJson.getAsString("gpg45Score")));
            }

            Map<String, Object> criMap = new HashMap<>();
            criMap.put("attributes", attributesJson);
            if (gpg45ScoreJson != null) {
                criMap.put("gpg45Score", gpg45ScoreJson);
            }
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
