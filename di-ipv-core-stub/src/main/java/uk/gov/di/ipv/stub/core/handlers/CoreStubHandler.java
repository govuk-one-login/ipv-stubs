package uk.gov.di.ipv.stub.core.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.Payload;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.core.config.CoreStubConfig;
import uk.gov.di.ipv.stub.core.config.credentialissuer.CredentialIssuer;
import uk.gov.di.ipv.stub.core.config.uatuser.DisplayIdentity;
import uk.gov.di.ipv.stub.core.config.uatuser.FindDateOfBirth;
import uk.gov.di.ipv.stub.core.config.uatuser.FullName;
import uk.gov.di.ipv.stub.core.config.uatuser.Identity;
import uk.gov.di.ipv.stub.core.config.uatuser.IdentityMapper;
import uk.gov.di.ipv.stub.core.config.uatuser.SharedClaims;
import uk.gov.di.ipv.stub.core.config.uatuser.UKAddress;
import uk.gov.di.ipv.stub.core.utils.HandlerHelper;
import uk.gov.di.ipv.stub.core.utils.ViewHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class CoreStubHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreStubHandler.class);

    private final Map<String, CredentialIssuer> stateSession = new HashMap<>();
    private HandlerHelper handlerHelper;
    private Map<String, String> questionsMap = new HashMap<>();

    public CoreStubHandler(HandlerHelper handlerHelper) {
        this.handlerHelper = handlerHelper;

        setQuestions();
    }

    private void setQuestions() {
        List<List<String>> records = new ArrayList<>();
        InputStream resourceAsStream =
                getClass().getClassLoader().getResourceAsStream("questions.csv");
        try (BufferedReader br =
                new BufferedReader(
                        new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                records.add(Arrays.asList(values));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (records.size() != 2) {
            throw new IllegalStateException("expected questions.csv to contain 2 rows");
        }

        List<String> qids = records.get(0);
        List<String> questions = records.get(1);
        if (qids.size() != questions.size()) {
            throw new IllegalStateException(
                    "questions.csv question ids and answer sizes don't match: %d to %d"
                            .formatted(qids.size(), questions.size()));
        }

        for (int i = 0; i < qids.size(); i++) {
            questionsMap.put(qids.get(i).toUpperCase(), questions.get(i));
        }
        LOGGER.info("✅  set %d questions".formatted(questionsMap.size()));
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
                LOGGER.info("👈 received callback for state {}", state);
                var credentialIssuer = stateSession.remove(state.getValue());
                var accessToken =
                        handlerHelper.exchangeCodeForToken(
                                authorizationCode, credentialIssuer, state);
                LOGGER.info("access token value: " + accessToken.getValue());
                var userInfo =
                        SignedJWT.parse(
                                        handlerHelper.getUserInfo(
                                                accessToken, credentialIssuer, state))
                                .getJWTClaimsSet()
                                .toString();

                String data = "{\"result\": \"hidden\"}";
                if (CoreStubConfig.CORE_STUB_SHOW_VC) {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonElement je = JsonParser.parseString(userInfo);
                    data = gson.toJson(je);
                }

                Map<String, Object> moustacheDataModel = new HashMap<>();
                moustacheDataModel.put("data", data);
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
                }else if(Objects.nonNull(request.queryParams("postcode"))){
                    var claimIdentity =
                            new IdentityMapper()
                                    .mapToAddressSharedClaims(request.queryParams("postcode"));
                    sendAuthorizationRequest(request, response, credentialIssuer, claimIdentity);
                    return null;
                } else {
                    sendAuthorizationRequest(request, response, credentialIssuer, null);
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
                var claimIdentity =
                        new IdentityMapper()
                                .mapToSharedClaim(
                                        identity, CoreStubConfig.CORE_STUB_CONFIG_AGED_DOB);
                sendAuthorizationRequest(request, response, credentialIssuer, claimIdentity);
                return null;
            };

    public Route answers =
            (Request request, Response response) -> {
                var name = Objects.requireNonNull(request.queryParams("name"));
                var credentialIssuerId = Objects.requireNonNull(request.queryParams("cri"));
                var rowNumber =
                        Integer.valueOf(Objects.requireNonNull(request.queryParams("rowNumber")));
                var identity = handlerHelper.findIdentityByRowNumber(rowNumber);
                var questionAndAnswers =
                        new IdentityMapper().mapToQuestionAnswers(identity, questionsMap);

                return ViewHelper.render(
                        Map.of(
                                "name",
                                name,
                                "cri",
                                credentialIssuerId,
                                "identity",
                                identity,
                                "questionAndAnswers",
                                questionAndAnswers),
                        "answers.mustache");
            };

    public Route updateUser =
            (Request request, Response response) -> {
                var credentialIssuer =
                        handlerHelper.findCredentialIssuer(
                                Objects.requireNonNull(request.queryParams("cri")));
                QueryParamsMap queryParamsMap = request.queryMap();
                var identityOnRecord = fetchOrCreateIdentity(queryParamsMap.value("rowNumber"));
                IdentityMapper identityMapper = new IdentityMapper();
                var identity = identityMapper.mapFormToIdentity(identityOnRecord, queryParamsMap);
                SharedClaims sharedClaims =
                        identityMapper.mapToSharedClaim(
                                identity, CoreStubConfig.CORE_STUB_CONFIG_AGED_DOB);
                sendAuthorizationRequest(request, response, credentialIssuer, sharedClaims);
                return null;
            };

    private <T> void sendAuthorizationRequest(
            Request request,
            Response response,
            CredentialIssuer credentialIssuer,
            T sharedClaims)
            throws JOSEException, java.text.ParseException {
        State state = createNewState(credentialIssuer);
        request.session().attribute("state", state);
        AuthorizationRequest authRequest =
                handlerHelper.createAuthorizationJAR(state, credentialIssuer, sharedClaims);
        LOGGER.info("🚀 sending AuthorizationRequest for state {}", state);
        response.redirect(authRequest.toURI().toString());
    }

    private AuthorizationRequest createBackendAuthorizationRequest(
            CredentialIssuer credentialIssuer, JWTClaimsSet claimsSet)
            throws JOSEException, java.text.ParseException {
        AuthorizationRequest authRequest =
                handlerHelper.createBackEndAuthorizationJAR(credentialIssuer, claimsSet);
        LOGGER.info("🚀 Created AuthorizationRequest for state {}", claimsSet.getClaim("state"));
        return authRequest;
    }

    public Route editUser =
            (Request request, Response response) -> {
                var credentialIssuerId =
                        Objects.requireNonNull(request.queryParams("cri"), "cri required");
                var credentialIssuer = handlerHelper.findCredentialIssuer(credentialIssuerId);
                String rowNumber = request.queryParams("rowNumber");
                Identity identity = fetchOrCreateIdentity(rowNumber);

                Map<String, UKAddress> addressMap = new HashMap<>();
                for (int i = 0; i < identity.addresses().size(); i++) {
                    addressMap.put("" + i, identity.addresses().get(i));
                }
                return ViewHelper.render(
                        Map.of(
                                "cri",
                                credentialIssuerId,
                                "criName",
                                credentialIssuer.name(),
                                "identity",
                                identity,
                                "addressMap",
                                addressMap,
                                "rowNumber",
                                Optional.ofNullable(rowNumber).orElse("0")),
                        "edit-user.mustache");
            };

    public Route backendGenerateInitialClaimsSet =
            (Request request, Response response) -> {
                var credentialIssuerId = Objects.requireNonNull(request.queryParams("cri"));
                var rowNumber =
                        Integer.valueOf(Objects.requireNonNull(request.queryParams("rowNumber")));
                var credentialIssuer = handlerHelper.findCredentialIssuer(credentialIssuerId);
                var identity = handlerHelper.findIdentityByRowNumber(rowNumber);
                var claimIdentity =
                        new IdentityMapper()
                                .mapToSharedClaim(
                                        identity, CoreStubConfig.CORE_STUB_CONFIG_AGED_DOB);

                State state = createNewState(credentialIssuer);
                LOGGER.info("Created State {} for {}", state.toJSONString(), credentialIssuerId);

                // ClaimSets can go direct to JSON
                response.type("application/json");
                return handlerHelper.createJWTClaimsSets(
                        state,
                        credentialIssuer,
                        claimIdentity,
                        new ClientID(CoreStubConfig.CORE_STUB_CLIENT_ID));
            };

    public Route createBackendSessionRequest =
            (Request request, Response response) -> {
                LOGGER.info("CreateBackendSessionRequest Start");
                var credentialIssuerId = Objects.requireNonNull(request.queryParams("cri"));
                var credentialIssuer = handlerHelper.findCredentialIssuer(credentialIssuerId);

                // The JSON data will have urls that will be escaped.
                LOGGER.info("Getting request body");
                var escapedData = Objects.requireNonNull(request.body(), "UTF-8");
                String data = escapedData.replace("\\", "");

                LOGGER.info("Parsing Request data to JWTClaimsSet {}", data);
                JWTClaimsSet claimsSet = JWTClaimsSet.parse(new Payload(data).toJSONObject());
                LOGGER.info("JWTClaimsSet Parsed!");

                AuthorizationRequest authorizationRequest =
                        createBackendAuthorizationRequest(credentialIssuer, claimsSet);

                // This uri can be pasted into a browser and Journey continued in the frontend.
                LOGGER.info("Auth URI {}", authorizationRequest.toURI());

                LOGGER.info("CreateBackendSessionRequest Complete");
                response.type("application/json");
                return createBackendSessionRequestJSONReply(authorizationRequest);
            };

    public Route createTokenRequestPrivateKeyJWT =
            (Request request, Response response) -> {
                LOGGER.info("createTokenRequestPrivateKeyJWT Start");
                var credentialIssuerId = Objects.requireNonNull(request.queryParams("cri"));
                var authorizationCode =
                        Objects.requireNonNull(request.queryParams("authorization_code"));

                var credentialIssuer = handlerHelper.findCredentialIssuer(credentialIssuerId);
                TokenRequest tokenRequest =
                        handlerHelper.createTokenRequest(
                                new AuthorizationCode(authorizationCode), credentialIssuer);
                return tokenRequest.toHTTPRequest().getQuery();
            };

    public Route editPostCode;

    private String createBackendSessionRequestJSONReply(AuthorizationRequest authorizationRequest) {
        // Splits the QueryString from the Auth URI.  Turning the list of parameters
        // (key1=value1&key2=value2 etc...) into a json object.
        String queryParams = authorizationRequest.toQueryString();
        String[] queryKVPairs = queryParams.split("&");

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (int qp = 0; qp < queryKVPairs.length; qp++) {

            String[] paramKV = queryKVPairs[qp].split("=");

            sb.append('"');
            sb.append(paramKV[0]);
            sb.append('"');

            sb.append(':');

            sb.append('"');
            sb.append(paramKV[1]);
            sb.append('"');

            if (qp < (queryKVPairs.length - 1)) {
                sb.append(',');
            }
        }
        sb.append('}');

        return sb.toString();
    }

    private Identity fetchOrCreateIdentity(String rowNumber) {
        if (rowNumber != null && !rowNumber.isBlank() && !rowNumber.equals("0")) {
            return handlerHelper.findIdentityByRowNumber(Integer.valueOf(rowNumber));
        } else {
            return createNewIdentity();
        }
    }

    private Identity createNewIdentity() {
        Identity identity;
        UKAddress ukAddress = new UKAddress(null, null, null, null, null, null, null, null);
        FullName fullName = new FullName(null, null);
        Instant dob = Instant.ofEpochSecond(0);
        identity =
                new Identity(
                        0,
                        "",
                        "",
                        List.of(ukAddress),
                        new FindDateOfBirth(dob, dob),
                        fullName,
                        null);
        return identity;
    }

    private State createNewState(CredentialIssuer credentialIssuer) {
        var state = new State();
        stateSession.put(state.getValue(), credentialIssuer);
        return state;
    }
}
