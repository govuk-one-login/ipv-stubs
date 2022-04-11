package uk.gov.di.ipv.stub.core.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.id.State;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.core.config.CoreStubConfig;
import uk.gov.di.ipv.stub.core.config.credentialissuer.CredentialIssuer;
import uk.gov.di.ipv.stub.core.config.uatuser.DisplayIdentity;
import uk.gov.di.ipv.stub.core.config.uatuser.IdentityMapper;
import uk.gov.di.ipv.stub.core.utils.HandlerHelper;
import uk.gov.di.ipv.stub.core.utils.ViewHelper;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CoreStubHandler {

    public static final String ES256 = "ES256";
    public static final String RS256 = "RS256";
    private final Map<String, CredentialIssuer> stateSession = new HashMap<>();
    private HandlerHelper handlerHelper;
    private RSAKey rsaSigningKey;
    private ECKey ecSigningKey;

    public CoreStubHandler(HandlerHelper handlerHelper, RSAKey rsaSigningKey, ECKey ecSigningKey) {
        this.handlerHelper = handlerHelper;
        this.rsaSigningKey = rsaSigningKey;
        this.ecSigningKey = ecSigningKey;
    }

    public Route serveHomePage =
            (Request request, Response response) -> ViewHelper.render(null, "home.mustache");

    public Route showCredentialIssuer =
            (Request request, Response response) ->
                    ViewHelper.render(
                            Map.of("cris", CoreStubConfig.credentialIssuers),
                            "credential-issuers.mustache");

    public Route userSearch =
            (Request request, Response response) -> {
                var credentialIssuerId = Objects.requireNonNull(request.queryParams("cri"));

                var credentialIssuer = handlerHelper.findCredentialIssuer(credentialIssuerId);

                var results = handlerHelper.findByName(request.queryParams("name"));
                int size = results.size();
                if (size > CoreStubConfig.CORE_STUB_MAX_SEARCH_RESULTS) {
                    throw new IllegalStateException("Too many matches: %d".formatted(size));
                }

                if (size == 0) {
                    throw new IllegalStateException("No matches");
                }

                var identityMapper = new IdentityMapper();
                var displayIdentities =
                        results.stream()
                                .map(identityMapper::mapToDisplayable)
                                .sorted(Comparator.comparingInt(DisplayIdentity::rowNumber))
                                .collect(Collectors.toList());

                var modelMap = new HashMap<String, Object>();
                modelMap.put("cri", credentialIssuer.id());
                modelMap.put("criName", credentialIssuer.name());
                modelMap.put("identities", displayIdentities);
                return ViewHelper.render(modelMap, "search-results.mustache");
            };

    public Route doCallback =
            (Request request, Response response) -> {
                var authorizationResponse = handlerHelper.getAuthorizationResponse(request);
                var authorizationCode =
                        authorizationResponse.toSuccessResponse().getAuthorizationCode();
                var state = authorizationResponse.toSuccessResponse().getState();
                var credentialIssuer = stateSession.remove(state.getValue());
                var accessToken =
                        handlerHelper.exchangeCodeForToken(
                                authorizationCode, credentialIssuer, rsaSigningKey, ecSigningKey);
                var userInfo = handlerHelper.getUserInfo(accessToken, credentialIssuer);

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Map jsonMap = gson.fromJson(userInfo.toJSONString(), Map.class);
                Map<String, Object> moustacheDataModel = new HashMap<>();
                moustacheDataModel.put("data", gson.toJson(jsonMap));
                moustacheDataModel.put("cri", credentialIssuer.id());
                moustacheDataModel.put("criName", credentialIssuer.name());

                return ViewHelper.render(moustacheDataModel, "userinfo.mustache");
            };

    public Route handleCredentialIssuerRequest =
            (Request request, Response response) -> {
                var credentialIssuer =
                        handlerHelper.findCredentialIssuer(
                                Objects.requireNonNull(request.queryParams("cri")));

                if (credentialIssuer.sendIdentityClaims()) {
                    return ViewHelper.render(
                            Map.of(
                                    "cri",
                                    credentialIssuer.id(),
                                    "criName",
                                    credentialIssuer.name()),
                            "user-search.mustache");
                } else {
                    SignedJWT jwt = createSignedClaimJwt(credentialIssuer, Map.of());
                    State state = createNewState(credentialIssuer);
                    AuthorizationRequest authRequest =
                            credentialIssuer.sendOAuthJAR()
                                    ? handlerHelper.createAuthorizationJAR(
                                            state, credentialIssuer, null, rsaSigningKey)
                                    : handlerHelper.createAuthorizationRequest(
                                            state, credentialIssuer, jwt);
                    response.redirect(authRequest.toURI().toString());
                    return null;
                }
            };

    public Route authorize =
            (Request request, Response response) -> {
                var credentialIssuerId = Objects.requireNonNull(request.queryParams("cri"));
                var rowNumber =
                        Integer.valueOf(Objects.requireNonNull(request.queryParams("rowNumber")));
                var credentialIssuer = handlerHelper.findCredentialIssuer(credentialIssuerId);
                var identity = handlerHelper.findIdentityByRowNumber(rowNumber);
                var claimIdentity = new IdentityMapper().mapToSharedClaim(identity);
                var jwt = createSignedClaimJwt(credentialIssuer, claimIdentity);
                var state = createNewState(credentialIssuer);
                var authRequest =
                        credentialIssuer.sendOAuthJAR()
                                ? handlerHelper.createAuthorizationJAR(
                                        state, credentialIssuer, claimIdentity, rsaSigningKey)
                                : handlerHelper.createAuthorizationRequest(
                                        state, credentialIssuer, jwt);
                response.redirect(authRequest.toURI().toString());
                return null;
            };

    private State createNewState(CredentialIssuer credentialIssuer) {
        var state = new State();
        stateSession.put(state.getValue(), credentialIssuer);
        return state;
    }

    private SignedJWT createSignedClaimJwt(CredentialIssuer credentialIssuer, Object claims)
            throws JOSEException {
        return switch (credentialIssuer.expectedAlgo()) {
            case RS256 -> handlerHelper.createRS256ClaimsJWT(claims, rsaSigningKey);
            case ES256 -> handlerHelper.createES256ClaimsJWT(claims, ecSigningKey);
            default -> throw new RuntimeException("Expected algorithm not supported");
        };
    }
}
