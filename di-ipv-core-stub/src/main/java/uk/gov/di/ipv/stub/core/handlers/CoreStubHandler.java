package uk.gov.di.ipv.stub.core.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.*;
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
import uk.gov.di.ipv.stub.core.config.uatuser.*;
import uk.gov.di.ipv.stub.core.utils.HandlerHelper;
import uk.gov.di.ipv.stub.core.utils.ViewHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
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

import static spark.utils.StringUtils.isBlank;
import static spark.utils.StringUtils.isNotBlank;

public class CoreStubHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreStubHandler.class);

    private final Map<String, CredentialIssuer> stateSession = new HashMap<>();
    private HandlerHelper handlerHelper;
    private Map<String, String> questionsMap = new HashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private String jwksResponse;

    public CoreStubHandler(HandlerHelper handlerHelper) {
        this.handlerHelper = handlerHelper;

        setQuestions();
        constructWellKnownJwksSet();
    }

    private void constructWellKnownJwksSet() {
        // Public signing key - not a secret
        String y = "8F8LnQ7wG9hxsT4ax0Aty7iMGIyiY_YGp3_qIZzKo1A"; // pragma: allowlist-secret
        String x = "k39uKacSukQBrMZrHDTBUZslivpXKDNZTg6inCHwrLc"; // pragma: allowlist-secret
        StringBuilder sb =
                new StringBuilder()
                        .append("{\n")
                        .append("  \"keys\": [\n")
                        .append("    {\n")
                        .append("      \"kty\": \"EC\",\n")
                        .append("      \"use\": \"sig\",\n")
                        .append("      \"crv\": \"P-256\",\n")
                        .append("      \"x\": ")
                        .append(x)
                        .append(",\n")
                        .append("      \"y\": ")
                        .append(y)
                        .append(",\n")
                        .append(
                                "      \"kid\": \"0020c60a8796188b88dab4540a918cf7c8d33c9dbe5642b231aad12f2ebffcf6\",\n")
                        .append("      \"alg\": \"ES256\"\n")
                        .append("    }\n")
                        .append("  ]\n")
                        .append("}");
        jwksResponse = sb.toString();
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

    // Used where sharedClaim is entered as raw JSON string from browser
    public Route sendRawSharedClaim =
            (Request request, Response response) -> {
                var credentialIssuer =
                        handlerHelper.findCredentialIssuer(
                                Objects.requireNonNull(request.queryParams("cri")));
                String queryString = request.queryParams("claimsText");
                SharedClaims sharedClaims;
                try {
                    sharedClaims =
                            (queryString == null || queryString.isEmpty())
                                    ? null
                                    : objectMapper.readValue(queryString, SharedClaims.class);
                    LOGGER.info("Raw JSON in form input mapped to shared claims");
                } catch (Exception e) {
                    LOGGER.error("Unable to map raw JSON in form input mapped to shared claims");
                    throw e;
                }
                sendAuthorizationRequest(request, response, credentialIssuer, sharedClaims);
                return null;
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
                var signedJWT =
                        SignedJWT.parse(
                                handlerHelper.getUserInfo(accessToken, credentialIssuer, state));
                if (handlerHelper.checkES256SignatureFormat(signedJWT)) {
                    if (!handlerHelper.verifySignedJwt(signedJWT, credentialIssuer)) {
                        throw new IllegalStateException(
                                "Unable to verify the returned JWT, format may be invalid.");
                    }
                    LOGGER.info("🚀 Successfully verified signedJWT is in concat format");
                }

                var userInfo = signedJWT.getJWTClaimsSet().toString();

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
                var postcode = request.queryParams("postcode");
                var strengthScore = request.queryParams("score");
                var scoringPolicy = request.queryParams("evidence_request");
                var verificationScore = request.queryParams("verification_score");
                var identityFraudScore = request.queryParams("identity_fraud_score");

                if (credentialIssuer.sendIdentityClaims()
                        && Objects.isNull(request.queryParams("postcode"))) {
                    saveEvidenceRequestToSessionIfPresent(
                            request,
                            strengthScore,
                            scoringPolicy,
                            verificationScore,
                            identityFraudScore);
                    return ViewHelper.render(
                            Map.of(
                                    "cri",
                                    credentialIssuer.id(),
                                    "criName",
                                    credentialIssuer.name()),
                            "user-search.mustache");
                } else if (postcode != null && !postcode.isBlank()) {
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

                // International Address Compatibility
                if (credentialIssuerId.contains("fraud-cri") && null != identity) {
                    identity = identity.withAddressCountry("GB");
                }

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
                var identityOnRecord =
                        fetchOrCreateIdentity(
                                queryParamsMap.value("rowNumber"), credentialIssuer.name());
                IdentityMapper identityMapper = new IdentityMapper();
                var identity = identityMapper.mapFormToIdentity(identityOnRecord, queryParamsMap);
                SharedClaims sharedClaims =
                        identityMapper.mapToSharedClaim(
                                identity, CoreStubConfig.CORE_STUB_CONFIG_AGED_DOB);
                sendAuthorizationRequest(request, response, credentialIssuer, sharedClaims);
                return null;
            };

    private <T> void sendAuthorizationRequest(
            Request request, Response response, CredentialIssuer credentialIssuer, T sharedClaims)
            throws ParseException, JOSEException, JsonProcessingException {
        State state = createNewState(credentialIssuer);
        request.session().attribute("state", state);
        EvidenceRequestClaims sessionEvidenceRequest =
                request.session().attribute("evidence_request");

        if (!Objects.isNull(sessionEvidenceRequest)) {
            LOGGER.info("✅  Removed evidence request from session to {}", sessionEvidenceRequest);
            request.session().removeAttribute("evidence_request");
        }

        String context = request.queryParams("context");
        String evidenceRequestClaimsText = request.queryParams("evidenceRequestClaimsText");

        EvidenceRequestClaims evidenceRequestClaims = null;
        try {
            evidenceRequestClaims =
                    (evidenceRequestClaimsText == null || evidenceRequestClaimsText.isEmpty())
                            ? null
                            : objectMapper.readValue(
                                    evidenceRequestClaimsText, EvidenceRequestClaims.class);
            LOGGER.info(
                    "Raw JSON {} in form input mapped to evidence request claims, ",
                    evidenceRequestClaimsText);
        } catch (Exception e) {
            LOGGER.error("Unable to map raw JSON in form input mapped to evidence request claims");
            throw e;
        }

        AuthorizationRequest authRequest;

        try {
            authRequest =
                    handlerHelper.createAuthorizationJAR(
                            state, credentialIssuer, sharedClaims, evidenceRequestClaims, context);
        } catch (JOSEException joseException) {
            LOGGER.error("JOSEException occurred," + joseException.getMessage());
            throw joseException;
        } catch (ParseException parseException) {
            LOGGER.error("ParseException occurred," + parseException.getMessage());
            throw parseException;
        } catch (Exception e) {
            LOGGER.error("Unknown exception occurred," + e.getMessage());
            throw e;
        }

        LOGGER.info("🚀 sending AuthorizationRequest for state {}", state);

        if (authRequest != null) {
            URI uri = authRequest.toURI();
            if (uri != null) {
                LOGGER.info("Redirecting to {}", uri);
                response.redirect(authRequest.toURI().toString());
            } else {
                String error = "AuthorizationRequest URI object is null";
                LOGGER.error(error);
                throw new RuntimeException(error);
            }
        } else {
            String error = "AuthorizationRequest object is null";
            LOGGER.error(error);
            throw new RuntimeException(error);
        }
    }

    private AuthorizationRequest createBackendAuthorizationRequest(
            CredentialIssuer credentialIssuer, JWTClaimsSet claimsSet)
            throws JOSEException, java.text.ParseException, MalformedURLException {
        AuthorizationRequest authRequest =
                handlerHelper.createBackEndAuthorizationJAR(credentialIssuer, claimsSet);
        LOGGER.info("🚀 Created AuthorizationRequest for state {}", claimsSet.getClaim("state"));
        return authRequest;
    }

    public Route editUser =
            (Request request, Response response) -> {
                var credentialIssuerId =
                        Objects.requireNonNull(request.queryParams("cri"), "cri required");
                boolean isHmrcKbvCri = credentialIssuerId.contains("hmrc-kbv-cri");
                var credentialIssuer = handlerHelper.findCredentialIssuer(credentialIssuerId);
                String rowNumber = request.queryParams("rowNumber");
                Identity identity = fetchOrCreateIdentity(rowNumber, credentialIssuerId);

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
                                Optional.ofNullable(rowNumber).orElse("0"),
                                "isHmrcKbvCri",
                                isHmrcKbvCri),
                        "edit-user.mustache");
            };

    public Route backendGenerateInitialClaimsSet =
            (Request request, Response response) -> {
                var credentialIssuerId = Objects.requireNonNull(request.queryParams("cri"));
                var credentialIssuer = handlerHelper.findCredentialIssuer(credentialIssuerId);

                Object claimIdentity = getClaimIdentity(request, credentialIssuerId);
                String context = request.queryParams("context");

                State state = createNewState(credentialIssuer);
                LOGGER.info("Created State {} for {}", state.toJSONString(), credentialIssuerId);

                // ClaimSets can go direct to JSON
                response.type("application/json");
                return handlerHelper.createJWTClaimsSets(
                        state,
                        credentialIssuer,
                        new ClientID(CoreStubConfig.CORE_STUB_CLIENT_ID),
                        claimIdentity,
                        getEvidenceRequestClaims(request),
                        context);
            };

    private SharedClaims getClaimIdentity(Request request, String credentialIssuerId) {
        String claimsTextParam = request.queryParams("claimsText");
        String rowNumberParam = request.queryParams("rowNumber");
        if (isBlank(rowNumberParam) && isBlank(claimsTextParam)) {
            return null;
        }
        if (isNotBlank(rowNumberParam)) {
            return getClaimIdentityByRowNumber(
                    Integer.parseInt(rowNumberParam),
                    request.queryParams("nino"),
                    credentialIssuerId);
        }
        return getClaimIdentityByClaimsText(claimsTextParam);
    }

    private SharedClaims getClaimIdentityByRowNumber(
            int rowNumber, String nino, String credentialIssuerId) {
        var identity = handlerHelper.findIdentityByRowNumber(rowNumber).withNino(nino);

        // International Address Compatibility
        if (credentialIssuerId.contains("fraud-cri") && null != identity) {
            identity = identity.withAddressCountry("GB");
        }

        return new IdentityMapper()
                .mapToSharedClaim(identity, CoreStubConfig.CORE_STUB_CONFIG_AGED_DOB);
    }

    private SharedClaims getClaimIdentityByClaimsText(String claimsText) {
        // claimsText used where sharedClaim is entered as raw JSON
        // string from browser for DL CRI
        try {
            return objectMapper.readValue(claimsText, SharedClaims.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid claimsText format", e);
        }
    }

    private EvidenceRequestClaims getEvidenceRequestClaims(Request request) {
        String scoringPolicy = request.queryParams("scoringPolicy");
        String strengthScore = request.queryParams("strengthScore");
        String verificationScore = request.queryParams("verificationScore");
        String identityFraudScore = request.queryParams("identityFraudScore");

        if (Objects.nonNull(strengthScore)
                || Objects.nonNull(scoringPolicy)
                || Objects.nonNull(verificationScore)
                || Objects.nonNull(identityFraudScore)) {
            return new EvidenceRequestClaims(
                    scoringPolicy,
                    Objects.isNull(strengthScore) ? null : Integer.parseInt(strengthScore),
                    Objects.isNull(verificationScore) ? null : Integer.parseInt(verificationScore),
                    Objects.isNull(identityFraudScore)
                            ? null
                            : Integer.parseInt(identityFraudScore));
        }
        return null;
    }

    public Route wellKnownJwksStub =
            (Request request, Response response) -> {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Received request for JWKS endpoint: {}", request.url());
                }
                response.type("application/json");
                return jwksResponse;
            };

    public Route backendGenerateInitialClaimsSetPostCode =
            (Request request, Response response) -> {
                var credentialIssuerId =
                        handlerHelper.findCredentialIssuer(
                                Objects.requireNonNull(request.queryParams("cri")));

                var postcode = request.queryParams("postcode");
                if (postcode == null || postcode.isBlank()) {
                    throw new IllegalStateException("Postcode cannot be blank");
                }

                PostcodeSharedClaims claimIdentity =
                        new IdentityMapper().mapToAddressSharedClaims(postcode);

                State state = createNewState(credentialIssuerId);
                LOGGER.info("Created State {} for {}", state.toJSONString(), credentialIssuerId);

                // ClaimSets can go direct to JSON
                response.type("application/json");
                return handlerHelper.createJWTClaimsSets(
                        state,
                        credentialIssuerId,
                        new ClientID(CoreStubConfig.CORE_STUB_CLIENT_ID),
                        claimIdentity);
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

    public Route editPostcode =
            (Request request, Response response) -> {
                LOGGER.info("editPostcode Start");
                var credentialIssuerId = Objects.requireNonNull(request.queryParams("cri"));
                return ViewHelper.render(
                        Map.of("cri", credentialIssuerId), "edit-postcode.mustache");
            };

    public Route evidenceRequest =
            (Request request, Response response) -> {
                LOGGER.info("checkEvidence Requested Start");
                var credentialIssuerId = Objects.requireNonNull(request.queryParams("cri"));
                return ViewHelper.render(
                        Map.of("cri", credentialIssuerId), "evidence-request.mustache");
            };

    private String createBackendSessionRequestJSONReply(AuthorizationRequest authorizationRequest) {
        // Splits the QueryString from the Auth URI. Turning the list of parameters
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

    private Identity fetchOrCreateIdentity(String rowNumber, String credentialIssuerId) {
        if (rowNumber != null && !rowNumber.isBlank() && !rowNumber.equals("0")) {
            Identity identity = handlerHelper.findIdentityByRowNumber(Integer.valueOf(rowNumber));
            // International Address Compatibility
            if (credentialIssuerId.contains("fraud-cri") && null != identity) {
                identity = identity.withAddressCountry("GB");
            }
            return identity;
        } else {
            return createNewIdentity();
        }
    }

    private Identity createNewIdentity() {
        Identity identity;
        UKAddress ukAddress = new UKAddress(null, null, null, null, null, null, null, null, null);
        FullName fullName = new FullName(null, null, null);
        Instant dob = Instant.ofEpochSecond(0);
        identity =
                new Identity(
                        0,
                        "",
                        "",
                        List.of(ukAddress),
                        new FindDateOfBirth(dob, dob),
                        fullName,
                        null,
                        null);
        return identity;
    }

    private State createNewState(CredentialIssuer credentialIssuer) {
        var state = new State();
        stateSession.put(state.getValue(), credentialIssuer);
        return state;
    }

    private static void saveEvidenceRequestToSessionIfPresent(
            Request request,
            String strengthScore,
            String scoringPolicy,
            String verificationScore,
            String identityFraudScore) {
        EvidenceRequestClaims evidenceRequest =
                new EvidenceRequestClaims(
                        scoringPolicy,
                        strengthScore == null ? null : Integer.parseInt(strengthScore),
                        verificationScore == null ? null : Integer.parseInt(verificationScore),
                        identityFraudScore == null ? null : Integer.parseInt(identityFraudScore));
        LOGGER.info("✅  Saving evidence request to session to {}", evidenceRequest);
        request.session().attribute("evidence_request", evidenceRequest);
    }
}
