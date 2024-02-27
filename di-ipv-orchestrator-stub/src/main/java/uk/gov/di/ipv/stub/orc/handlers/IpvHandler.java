package uk.gov.di.ipv.stub.orc.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;
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
import com.nimbusds.oauth2.sdk.util.StringUtils;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.orc.exceptions.OauthException;
import uk.gov.di.ipv.stub.orc.exceptions.OrchestratorStubException;
import uk.gov.di.ipv.stub.orc.utils.JwtBuilder;
import uk.gov.di.ipv.stub.orc.utils.ViewHelper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_BACKCHANNEL_ENDPOINT;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_BACKCHANNEL_TOKEN_PATH;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_BACKCHANNEL_USER_IDENTITY_PATH;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_ENDPOINT;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_CLIENT_ID;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_REDIRECT_URL;
import static uk.gov.di.ipv.stub.orc.utils.JwtBuilder.URN_UUID;
import static uk.gov.di.ipv.stub.orc.utils.JwtBuilder.buildClientAuthenticationClaims;

public class IpvHandler {

    private static final String CREDENTIALS_URL_PROPERTY =
            "https://vocab.account.gov.uk/v1/credentialJWT";
    private static final String JSON_PAYLOAD_PARAM = "jsonPayload";
    private static final String EVIDENCE_JSON_PAYLOAD_PARAM = "evidenceJsonPayload";
    private static final String DURING_MIGRATION = "duringMigration";

    private static final State ORCHESTRATOR_STUB_STATE = new State("orchestrator-stub-state");

    private final Logger logger = LoggerFactory.getLogger(IpvHandler.class);

    public Route doAuthorize =
            (Request request, Response response) -> {
                String environment = request.queryMap().get("targetEnvironment").value();

                response.cookie("targetEnvironment", environment);

                String errorType = request.queryMap().get("error").value();
                String userIdTextValue = request.queryMap().get("userIdText").value();
                String signInJourneyIdText = request.queryMap().get("signInJourneyIdText").value();
                List<String> vtr =
                        Arrays.stream(request.queryMap("vtrText").value().split(","))
                                .map(String::trim)
                                .filter(value -> !value.isEmpty())
                                .toList();
                String vot = request.queryMap().get("votText").value();
                String userEmailAddress = request.queryMap().get("emailAddress").value();
                String reproveIdentityString = request.queryMap().get("reproveIdentity").value();
                JwtBuilder.ReproveIdentityClaimValue reproveIdentityClaimValue =
                        StringUtils.isNotBlank(reproveIdentityString)
                                ? JwtBuilder.ReproveIdentityClaimValue.valueOf(
                                        reproveIdentityString)
                                : JwtBuilder.ReproveIdentityClaimValue.NOT_PRESENT;

                String credentialSubject = request.queryMap().value(JSON_PAYLOAD_PARAM);
                String evidence = request.queryMap().value(EVIDENCE_JSON_PAYLOAD_PARAM);
                boolean duringMigration =
                        Objects.equals(request.queryMap().value(DURING_MIGRATION), "checked");

                String userId = getUserIdValue(userIdTextValue);

                JWTClaimsSet claims =
                        JwtBuilder.buildAuthorizationRequestClaims(
                                userId,
                                signInJourneyIdText,
                                ORCHESTRATOR_STUB_STATE.getValue(),
                                vtr,
                                errorType,
                                userEmailAddress,
                                reproveIdentityClaimValue,
                                environment,
                                duringMigration,
                                credentialSubject,
                                evidence,
                                vot);

                SignedJWT signedJwt = JwtBuilder.createSignedJwt(claims);
                EncryptedJWT encryptedJwt = JwtBuilder.encryptJwt(signedJwt, environment);
                var authRequest =
                        new AuthorizationRequest.Builder(
                                        new ResponseType(ResponseType.Value.CODE),
                                        new ClientID(ORCHESTRATOR_CLIENT_ID))
                                .state(ORCHESTRATOR_STUB_STATE)
                                .scope(new Scope("openid"))
                                .redirectionURI(new URI(ORCHESTRATOR_REDIRECT_URL))
                                .endpointURI(
                                        getIpvEndpoint(environment).resolve("/oauth2/authorize"))
                                .requestObject(EncryptedJWT.parse(encryptedJwt.serialize()))
                                .build();

                response.redirect(authRequest.toURI().toString());
                return null;
            };

    private URI getIpvEndpoint(String environment) throws URISyntaxException {
        String url =
                switch (environment) {
                    case ("BUILD") -> "https://identity.build.account.gov.uk/";
                    case ("STAGING") -> "https://identity.staging.account.gov.uk/";
                    case ("INTEGRATION") -> "https://identity.integration.account.gov.uk/";
                    default -> IPV_ENDPOINT;
                };

        return new URI(url);
    }

    private URI getIpvBackchannelEndpoint(String environment) throws URISyntaxException {
        String url =
                switch (environment) {
                    case ("BUILD") -> "https://api.identity.build.account.gov.uk/";
                    case ("STAGING") -> "https://api.identity.staging.account.gov.uk/";
                    case ("INTEGRATION") -> "https://api.identity.integration.account.gov.uk/";
                    default -> IPV_BACKCHANNEL_ENDPOINT;
                };

        return new URI(url);
    }

    public Route doCallback =
            (Request request, Response response) -> {
                List<Map<String, Object>> mustacheData;
                Map<String, Object> moustacheDataModel = new HashMap<>();
                String targetBackend = request.cookie("targetEnvironment");

                try {
                    var authorizationCode = getAuthorizationCode(request);

                    var accessToken = exchangeCodeForToken(authorizationCode, targetBackend);

                    var userInfo = getUserInfo(accessToken, targetBackend);

                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    String userInfoJson = gson.toJson(userInfo);
                    moustacheDataModel.put("rawUserInfo", userInfoJson);

                    mustacheData = buildMustacheData(userInfo);
                    moustacheDataModel.put("data", mustacheData);
                } catch (OrchestratorStubException | ParseException | JsonSyntaxException e) {
                    List<Map<String, Object>> errorObject =
                            List.of(Map.of("error_message", e.getMessage()));
                    moustacheDataModel.put("error", errorObject);
                } catch (OauthException e) {
                    List<Map<String, Object>> errorObject =
                            List.of(
                                    Map.of(
                                            "error",
                                            e.getErrorObject().getCode(),
                                            "error_description",
                                            e.getErrorObject().getDescription()));
                    moustacheDataModel.put("error", errorObject);
                }

                return ViewHelper.render(moustacheDataModel, "userinfo.mustache");
            };

    private AuthorizationCode getAuthorizationCode(Request request)
            throws ParseException, OauthException {
        var authorizationResponse =
                AuthorizationResponse.parse(URI.create("https:///?" + request.queryString()));

        if (!authorizationResponse.indicatesSuccess()) {
            var error = authorizationResponse.toErrorResponse().getErrorObject();
            logger.error("Failed authorization code request: {}", error);
            throw new OauthException(error);
        }

        if (!ORCHESTRATOR_STUB_STATE.equals(authorizationResponse.getState())) {
            throw new OauthException(
                    OAuth2Error.INVALID_REQUEST.appendDescription(
                            " - missing or invalid state value"));
        }

        return authorizationResponse.toSuccessResponse().getAuthorizationCode();
    }

    private AccessToken exchangeCodeForToken(
            AuthorizationCode authorizationCode, String targetEnvironment)
            throws OrchestratorStubException, URISyntaxException {
        URI resolve =
                getIpvBackchannelEndpoint(targetEnvironment).resolve(IPV_BACKCHANNEL_TOKEN_PATH);
        logger.debug("token url is " + resolve);

        SignedJWT signedClientJwt;

        try {
            JWTClaimsSet claims = buildClientAuthenticationClaims(targetEnvironment);
            signedClientJwt = JwtBuilder.createSignedJwt(claims);
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

    public JSONObject getUserInfo(AccessToken accessToken, String targetBackend)
            throws URISyntaxException {
        var userInfoRequest =
                new UserInfoRequest(
                        getIpvBackchannelEndpoint(targetBackend)
                                .resolve(IPV_BACKCHANNEL_USER_IDENTITY_PATH),
                        (BearerAccessToken) accessToken);

        HTTPResponse userInfoHttpResponse = sendHttpRequest(userInfoRequest.toHTTPRequest());

        int statusCode = userInfoHttpResponse.getStatusCode();
        if (statusCode != HTTPResponse.SC_OK) {
            var errorMessage =
                    "User info request failed with status code "
                            + statusCode
                            + ": "
                            + userInfoHttpResponse.getContent();
            logger.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }

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

        List<String> vcJwts = (List<String>) credentials.get(CREDENTIALS_URL_PROPERTY);

        for (String vc : vcJwts) {
            SignedJWT signedJWT = SignedJWT.parse(vc);

            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            Map<String, Object> claims = claimsSet.toJSONObject();

            String json = gson.toJson(claims);

            Map<String, Object> criMap = new HashMap<>();
            criMap.put("VC", json);
            criMap.put("criType", claims.get("iss"));
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

    private String getUserIdValue(String userIdTextValue) {
        if (StringUtils.isNotBlank(userIdTextValue)) {
            return userIdTextValue;
        }

        return URN_UUID + UUID.randomUUID();
    }
}
