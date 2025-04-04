package uk.gov.di.ipv.stub.orc.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import io.javalin.http.Context;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.stub.orc.exceptions.OauthException;
import uk.gov.di.ipv.stub.orc.exceptions.OrchestratorStubException;
import uk.gov.di.ipv.stub.orc.utils.EvcsAccessTokenGenerator;
import uk.gov.di.ipv.stub.orc.utils.JwtBuilder;

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

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static java.util.Objects.requireNonNullElse;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.AUTH_CLIENT_ID;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_BACKCHANNEL_ENDPOINT;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.IPV_ENDPOINT;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_CLIENT_ID;
import static uk.gov.di.ipv.stub.orc.config.OrchestratorConfig.ORCHESTRATOR_REDIRECT_URL;
import static uk.gov.di.ipv.stub.orc.utils.JwtBuilder.URN_UUID;
import static uk.gov.di.ipv.stub.orc.utils.JwtBuilder.buildClientAuthenticationClaims;

public class IpvHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(IpvHandler.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(INDENT_OUTPUT);

    private static final String CREDENTIALS_URL_PROPERTY =
            "https://vocab.account.gov.uk/v1/credentialJWT";
    private static final String TOKEN_PATH = "token";
    private static final String USER_IDENTITY_PATH = "user-identity";
    private static final String REVERIFICATION_PATH = "reverification";

    private static final String USER_ID_PARAM = "userIdText";
    private static final String JOURNEY_ID_PARAM = "signInJourneyIdText";
    private static final String VTR_PARAM = "vtrText";
    private static final String ENVIRONMENT_PARAM = "targetEnvironment";
    private static final String REPROVE_IDENTITY_PARAM = "reproveIdentity";
    private static final String EMAIL_ADDRESS_PARAM = "emailAddress";
    private static final String INHERITED_ID_INCLUDED_PARAM = "duringMigration";
    private static final String INHERITED_ID_VOT_PARAM = "votText";
    private static final String INHERITED_ID_SUBJECT_PARAM = "jsonPayload";
    private static final String INHERITED_ID_EVIDENCE_PARAM = "evidenceJsonPayload";
    private static final String MFA_RESET_PARAM = "mfaReset";
    private static final String ERROR_TYPE_PARAM = "error";
    private static final String CHECKBOX_CHECKED_VALUE = "checked";

    private static final String ENVIRONMENT_COOKIE = "targetEnvironment";

    private static final State ORCHESTRATOR_STUB_STATE = new State("orchestrator-stub-state");
    private static final State AUTH_STUB_STATE = new State("auth-stub-state");
    private static final Scope AUTH_STUB_SCOPE = new Scope("reverification");
    private static final Scope ORCHESTRATOR_STUB_SCOPE = new Scope("openid", "phone", "email");

    public void doAuthorize(Context ctx) throws Exception {
        var environment = ctx.queryParam(ENVIRONMENT_PARAM);
        if (environment != null) {
            ctx.cookie(ENVIRONMENT_COOKIE, environment);
        }
        ctx.redirect(getAuthorizeRedirect(ctx, null));
    }

    public void doAuthorizeError(Context ctx) throws Exception {
        var environment = ctx.queryParam(ENVIRONMENT_PARAM);
        if (environment != null) {
            ctx.cookie(ENVIRONMENT_COOKIE, environment);
        }
        var errorType = ctx.queryParam(ERROR_TYPE_PARAM);
        ctx.redirect(getAuthorizeRedirect(ctx, errorType));
    }

    private final EvcsAccessTokenGenerator evcsAccessTokenGenerator;

    public IpvHandler(EvcsAccessTokenGenerator evcsAccessTokenGenerator) {
        this.evcsAccessTokenGenerator = evcsAccessTokenGenerator;
    }

    private String getAuthorizeRedirect(Context ctx, String errorType) throws Exception {

        var environment = ctx.queryParam(ENVIRONMENT_PARAM);
        var userIdTextValue = stripIfNotNull(ctx.queryParam(USER_ID_PARAM));
        var signInJourneyIdText = stripIfNotNull(ctx.queryParam(JOURNEY_ID_PARAM));
        var userEmailAddress = stripIfNotNull(ctx.queryParam(EMAIL_ADDRESS_PARAM));
        var userId = getUserIdValue(userIdTextValue);
        var isMfaReset = Objects.equals(ctx.queryParam(MFA_RESET_PARAM), CHECKBOX_CHECKED_VALUE);

        JWTClaimsSet claims;
        Scope scope;
        String clientId;
        State state;
        if (isMfaReset) {
            scope = AUTH_STUB_SCOPE;
            clientId = AUTH_CLIENT_ID;
            state = AUTH_STUB_STATE;
            claims =
                    JwtBuilder.buildAuthorizationRequestClaims(
                            userId,
                            signInJourneyIdText,
                            state.getValue(),
                            null,
                            errorType,
                            userEmailAddress,
                            JwtBuilder.ReproveIdentityClaimValue.NOT_PRESENT,
                            environment,
                            false,
                            null,
                            null,
                            null,
                            scope,
                            clientId,
                            evcsAccessTokenGenerator.getAccessToken(environment, userId));
        } else {
            scope = ORCHESTRATOR_STUB_SCOPE;
            clientId = ORCHESTRATOR_CLIENT_ID;
            state = ORCHESTRATOR_STUB_STATE;
            var vtr =
                    Arrays.stream(requireNonNullElse(ctx.queryParam(VTR_PARAM), "").split(","))
                            .map(String::trim)
                            .filter(value -> !value.isEmpty())
                            .toList();

            var reproveIdentityString = ctx.queryParam(REPROVE_IDENTITY_PARAM);
            var reproveIdentityClaimValue =
                    StringUtils.isNotBlank(reproveIdentityString)
                            ? JwtBuilder.ReproveIdentityClaimValue.valueOf(reproveIdentityString)
                            : JwtBuilder.ReproveIdentityClaimValue.NOT_PRESENT;

            var includeInheritedId =
                    Objects.equals(
                            ctx.queryParam(INHERITED_ID_INCLUDED_PARAM), CHECKBOX_CHECKED_VALUE);
            var inheritedIdVot = ctx.queryParam(INHERITED_ID_VOT_PARAM);
            var inheritedIdSubject = ctx.queryParam(INHERITED_ID_SUBJECT_PARAM);
            var inheritedIdEvidence = ctx.queryParam(INHERITED_ID_EVIDENCE_PARAM);

            claims =
                    JwtBuilder.buildAuthorizationRequestClaims(
                            userId,
                            signInJourneyIdText,
                            state.getValue(),
                            vtr,
                            errorType,
                            userEmailAddress,
                            reproveIdentityClaimValue,
                            environment,
                            includeInheritedId,
                            inheritedIdSubject,
                            inheritedIdEvidence,
                            inheritedIdVot,
                            scope,
                            clientId,
                            evcsAccessTokenGenerator.getAccessToken(environment, userId));
        }

        SignedJWT signedJwt = JwtBuilder.createSignedJwt(claims, isMfaReset);
        EncryptedJWT encryptedJwt = JwtBuilder.encryptJwt(signedJwt, environment);
        var authRequest =
                new AuthorizationRequest.Builder(
                                new ResponseType(ResponseType.Value.CODE), new ClientID(clientId))
                        .state(state)
                        .scope(new Scope(scope))
                        .redirectionURI(new URI(ORCHESTRATOR_REDIRECT_URL))
                        .endpointURI(getIpvEndpoint(environment).resolve("/oauth2/authorize"))
                        .requestObject(EncryptedJWT.parse(encryptedJwt.serialize()))
                        .build();

        return authRequest.toURI().toString();
    }

    private String stripIfNotNull(String value) {
        return value == null ? null : value.strip();
    }

    private URI getIpvEndpoint(String environment) throws URISyntaxException {
        String url =
                switch (environment) {
                    case ("DEV") -> "https://dev.01.dev.identity.account.gov.uk/";
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
                    case ("DEV") -> "https://api.dev.01.dev.identity.account.gov.uk/";
                    case ("BUILD") -> "https://api.identity.build.account.gov.uk/";
                    case ("STAGING") -> "https://api.identity.staging.account.gov.uk/";
                    case ("INTEGRATION") -> "https://api.identity.integration.account.gov.uk/";
                    default -> IPV_BACKCHANNEL_ENDPOINT;
                };

        return new URI(url);
    }

    public void doCallback(Context ctx) {
        String targetBackend = ctx.cookie(ENVIRONMENT_COOKIE);
        var state = ctx.queryParam("state");
        var isAuthStub = AUTH_STUB_STATE.toString().equals(state);

        try {
            var authorizationCode = getAuthorizationCode(ctx);

            var accessToken = exchangeCodeForToken(authorizationCode, targetBackend, isAuthStub);

            if (ORCHESTRATOR_STUB_STATE.toString().equals(state)) {
                var userInfo = getUserInfo(accessToken, targetBackend, USER_IDENTITY_PATH);
                var userInfoJson = OBJECT_MAPPER.writeValueAsString(userInfo);
                var mustacheData = buildUserInfoMustacheData(userInfo);

                ctx.render(
                        "templates/user-info.mustache",
                        Map.of("rawUserInfo", userInfoJson, "data", mustacheData));
            } else if (isAuthStub) {
                var reverificationResult =
                        getUserInfo(accessToken, targetBackend, REVERIFICATION_PATH);
                var reverificationResultJson =
                        OBJECT_MAPPER.writeValueAsString(reverificationResult);

                ctx.render(
                        "templates/mfa-reset-result.mustache",
                        Map.of(
                                "rawUserInfo",
                                reverificationResultJson,
                                "success",
                                reverificationResult.get("success")));
            } else {
                throw new OrchestratorStubException(
                        "Unexpected callback result state for stub: " + state);
            }
        } catch (OauthException e) {
            var errorObject =
                    List.of(
                            Map.of(
                                    "error_code",
                                    e.getErrorObject().getCode(),
                                    "error_description",
                                    e.getErrorObject().getDescription()));

            ctx.render("templates/error.mustache", Map.of("error", errorObject));
        } catch (Exception e) {
            LOGGER.error("Error handling IPV callback", e);
            var errorObject =
                    List.of(
                            Map.of(
                                    "error_description",
                                    requireNonNullElse(e.getMessage(), "unknown")));

            ctx.render("templates/error.mustache", Map.of("error", errorObject));
        }
    }

    private AuthorizationCode getAuthorizationCode(Context ctx)
            throws ParseException, OauthException {
        var authorizationResponse =
                AuthorizationResponse.parse(URI.create("https:///?" + ctx.queryString()));
        var state = authorizationResponse.getState();

        if (!authorizationResponse.indicatesSuccess()) {
            var error = authorizationResponse.toErrorResponse().getErrorObject();
            LOGGER.error("Failed authorization code request: {}", error);
            throw new OauthException(error);
        }

        if (!ORCHESTRATOR_STUB_STATE.equals(state) && !AUTH_STUB_STATE.equals(state)) {
            throw new OauthException(
                    OAuth2Error.INVALID_REQUEST.appendDescription(
                            " - missing or invalid state value"));
        }

        return authorizationResponse.toSuccessResponse().getAuthorizationCode();
    }

    private AccessToken exchangeCodeForToken(
            AuthorizationCode authorizationCode, String targetEnvironment, boolean isAuthStub)
            throws OrchestratorStubException, URISyntaxException {
        var tokenUri = getIpvBackchannelEndpoint(targetEnvironment).resolve(TOKEN_PATH);
        LOGGER.debug("token url is " + tokenUri);

        SignedJWT signedClientJwt;
        try {
            JWTClaimsSet claims = buildClientAuthenticationClaims(targetEnvironment);
            signedClientJwt = JwtBuilder.createSignedJwt(claims, isAuthStub);
        } catch (JOSEException | InvalidKeySpecException | NoSuchAlgorithmException e) {
            LOGGER.error("Failed to generate orch client JWT", e);
            throw new OrchestratorStubException("Failed to generate orch client JWT");
        }

        TokenRequest tokenRequest =
                new TokenRequest(
                        tokenUri,
                        new PrivateKeyJWT(signedClientJwt),
                        new AuthorizationCodeGrant(
                                authorizationCode, URI.create(ORCHESTRATOR_REDIRECT_URL)));

        var httpTokenResponse = sendHttpRequest(tokenRequest.toHTTPRequest());
        TokenResponse tokenResponse = parseTokenResponse(httpTokenResponse);

        if (tokenResponse instanceof TokenErrorResponse) {
            TokenErrorResponse errorResponse = tokenResponse.toErrorResponse();
            LOGGER.error("Failed to get token: " + errorResponse.getErrorObject());
            throw new OrchestratorStubException(errorResponse.getErrorObject().getDescription());
        }

        return tokenResponse.toSuccessResponse().getTokens().getAccessToken();
    }

    public JSONObject getUserInfo(
            AccessToken accessToken, String targetBackend, String ipvBackchannelEndpointPath)
            throws URISyntaxException {
        var userInfoRequest =
                new UserInfoRequest(
                        getIpvBackchannelEndpoint(targetBackend)
                                .resolve(ipvBackchannelEndpointPath),
                        (BearerAccessToken) accessToken);

        HTTPResponse userInfoHttpResponse = sendHttpRequest(userInfoRequest.toHTTPRequest());

        int statusCode = userInfoHttpResponse.getStatusCode();
        if (statusCode != HTTPResponse.SC_OK) {
            var errorMessage =
                    "User info request failed with status code "
                            + statusCode
                            + ": "
                            + userInfoHttpResponse.getContent();
            LOGGER.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        try {
            return userInfoHttpResponse.getContentAsJSONObject();
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse user info response to JSON");
        }
    }

    private List<Map<String, Object>> buildUserInfoMustacheData(JSONObject credentials)
            throws java.text.ParseException, JsonProcessingException {
        List<Map<String, Object>> moustacheDataModel = new ArrayList<>();

        List<String> vcJwts = (List<String>) credentials.get(CREDENTIALS_URL_PROPERTY);

        for (String vc : vcJwts) {
            SignedJWT signedJWT = SignedJWT.parse(vc);

            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            Map<String, Object> claims = claimsSet.toJSONObject();

            Map<String, Object> criMap = new HashMap<>();
            criMap.put("VC", OBJECT_MAPPER.writeValueAsString(claims));
            criMap.put("criType", claims.get("iss"));
            moustacheDataModel.add(criMap);
        }

        return moustacheDataModel;
    }

    private HTTPResponse sendHttpRequest(HTTPRequest httpRequest) {
        try {
            return httpRequest.send();
        } catch (IOException | SerializeException exception) {
            LOGGER.error("Failed to send a http request", exception);
            throw new RuntimeException("Failed to send a http request", exception);
        }
    }

    private TokenResponse parseTokenResponse(HTTPResponse httpResponse) {
        try {
            return OIDCTokenResponseParser.parse(httpResponse);
        } catch (ParseException parseException) {
            LOGGER.error("Failed to parse token response");
            throw new RuntimeException("Failed to parse token response", parseException);
        }
    }

    private UserInfoResponse parseUserInfoResponse(HTTPResponse httpResponse) {
        try {
            return UserInfoResponse.parse(httpResponse);
        } catch (ParseException parseException) {
            LOGGER.error("Failed to parse user info response");
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
