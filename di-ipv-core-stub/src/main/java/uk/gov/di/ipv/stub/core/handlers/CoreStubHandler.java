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
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.stub.core.config.CoreStubConfig;
import uk.gov.di.ipv.stub.core.config.credentialissuer.CredentialIssuer;
import uk.gov.di.ipv.stub.core.config.uatuser.*;
import uk.gov.di.ipv.stub.core.utils.HandlerHelper;
import uk.gov.di.ipv.stub.core.utils.StringHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

import static uk.gov.di.ipv.stub.core.utils.StringHelper.isBlank;
import static uk.gov.di.ipv.stub.core.utils.StringHelper.isNotBlank;

public class CoreStubHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreStubHandler.class);

    private final Map<String, CredentialIssuer> stateSession = new HashMap<>();
    private HandlerHelper handlerHelper;
    private Map<String, String> questionsMap = new HashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

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

    public Handler serveHomePage =
            ctx -> ctx.render("home.mustache");

    public Handler showCredentialIssuer =
            ctx -> ctx.render("credential-issuers.mustache", Map.of("cris", CoreStubConfig.credentialIssuers));

    public Handler userSearch =
            ctx -> {
                var credentialIssuerId = Objects.requireNonNull(ctx.queryParam("cri"));

                var credentialIssuer = handlerHelper.findCredentialIssuer(credentialIssuerId);

                var results = handlerHelper.findByName(ctx.queryParam("name"));
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
                ctx.render("search-results.mustache", modelMap);
            };

    // Used where sharedClaim is entered as raw JSON string from browser for DL CRI
    public Handler sendRawSharedClaim =
            ctx -> {
                var credentialIssuer =
                        handlerHelper.findCredentialIssuer(
                                Objects.requireNonNull(ctx.queryParam("cri")));
                String queryString = ctx.queryParam("claimsText");
                SharedClaims sharedClaims;
                try {
                    sharedClaims = objectMapper.readValue(queryString, SharedClaims.class);
                    LOGGER.info("Raw JSON in form input mapped to shared claims");
                } catch (Exception e) {
                    LOGGER.error("Unable to map raw JSON in form input mapped to shared claims");
                    throw e;
                }
                sendAuthorizationRequest(ctx, credentialIssuer, sharedClaims);
            };

    public Handler doCallback =
            ctx -> {
                var authorizationResponse = handlerHelper.getAuthorizationResponse(ctx);
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

                ctx.render("userinfo.mustache", moustacheDataModel);
            };

    public Handler handleCredentialIssuerRequest =
            ctx -> {
                var credentialIssuer =
                        handlerHelper.findCredentialIssuer(
                                Objects.requireNonNull(ctx.queryParam("cri")));
                var postcode = ctx.queryParam("postcode");
                var strengthScore = ctx.queryParam("score");
                var scoringPolicy = ctx.queryParam("evidence_request");
                var verificationScore = ctx.queryParam("verification_score");

                if (credentialIssuer.sendIdentityClaims()
                        && Objects.isNull(ctx.queryParam("postcode"))) {
                    saveEvidenceRequestToSessionIfPresent(
                            ctx, strengthScore, scoringPolicy, verificationScore);
                    ctx.render(
                            "user-search.mustache",
                            Map.of(
                                    "cri",
                                    credentialIssuer.id(),
                                    "criName",
                                    credentialIssuer.name())
                    );
                } else if (postcode != null && !postcode.isBlank()) {
                    var claimIdentity =
                            new IdentityMapper()
                                    .mapToAddressSharedClaims(ctx.queryParam("postcode"));
                    sendAuthorizationRequest(ctx, credentialIssuer, claimIdentity);
                } else {
                    sendAuthorizationRequest(ctx, credentialIssuer, null);
                }
            };

    public Handler authorize =
            ctx -> {
                var credentialIssuerId = Objects.requireNonNull(ctx.queryParam("cri"));
                var rowNumber =
                        Integer.valueOf(Objects.requireNonNull(ctx.queryParam("rowNumber")));
                var credentialIssuer = handlerHelper.findCredentialIssuer(credentialIssuerId);
                var identity = handlerHelper.findIdentityByRowNumber(rowNumber);
                var claimIdentity =
                        new IdentityMapper()
                                .mapToSharedClaim(
                                        identity, CoreStubConfig.CORE_STUB_CONFIG_AGED_DOB);
                sendAuthorizationRequest(ctx, credentialIssuer, claimIdentity);
            };

    public Handler answers =
            ctx -> {
                var name = Objects.requireNonNull(ctx.queryParam("name"));
                var credentialIssuerId = Objects.requireNonNull(ctx.queryParam("cri"));
                var rowNumber =
                        Integer.valueOf(Objects.requireNonNull(ctx.queryParam("rowNumber")));
                var identity = handlerHelper.findIdentityByRowNumber(rowNumber);
                var questionAndAnswers =
                        new IdentityMapper().mapToQuestionAnswers(identity, questionsMap);

                ctx.render(
                        "answers.mustache",
                        Map.of(
                                "name",
                                name,
                                "cri",
                                credentialIssuerId,
                                "identity",
                                identity,
                                "questionAndAnswers",
                                questionAndAnswers)
                );
            };

    public Handler updateUser =
            ctx -> {
                var credentialIssuer =
                        handlerHelper.findCredentialIssuer(
                                Objects.requireNonNull(ctx.queryParam("cri")));
                var identityOnRecord = fetchOrCreateIdentity(ctx.queryParam("rowNumber"));
                IdentityMapper identityMapper = new IdentityMapper();
                var identity = identityMapper.mapFormToIdentity(identityOnRecord, ctx);
                SharedClaims sharedClaims =
                        identityMapper.mapToSharedClaim(
                                identity, CoreStubConfig.CORE_STUB_CONFIG_AGED_DOB);
                sendAuthorizationRequest(ctx, credentialIssuer, sharedClaims);
            };

    private <T> void sendAuthorizationRequest(
            Context ctx, CredentialIssuer credentialIssuer, T sharedClaims)
            throws ParseException, JOSEException, JsonProcessingException {
        State state = createNewState(credentialIssuer);
        ctx.sessionAttribute("state", state);
        EvidenceRequestClaims evidenceRequest = ctx.sessionAttribute("evidence_request");

        if (!Objects.isNull(evidenceRequest)) {
            LOGGER.info("✅  Retrieved evidence request from session to {}", evidenceRequest);
            ctx.req().getSession().removeAttribute("evidence_request");
        }

        var context = ctx.queryParam("context");

        AuthorizationRequest authRequest;

        try {
            authRequest =
                    handlerHelper.createAuthorizationJAR(
                            state, credentialIssuer, sharedClaims, evidenceRequest, context);
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
                ctx.redirect(authRequest.toURI().toString());
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
            throws JOSEException, java.text.ParseException {
        AuthorizationRequest authRequest =
                handlerHelper.createBackEndAuthorizationJAR(credentialIssuer, claimsSet);
        LOGGER.info("🚀 Created AuthorizationRequest for state {}", claimsSet.getClaim("state"));
        return authRequest;
    }

    public Handler editUser =
            ctx -> {
                var credentialIssuerId =
                        Objects.requireNonNull(ctx.queryParam("cri"), "cri required");
                boolean isHmrcKbvCri = credentialIssuerId.contains("hmrc-kbv-cri");
                var credentialIssuer = handlerHelper.findCredentialIssuer(credentialIssuerId);
                String rowNumber = ctx.queryParam("rowNumber");
                Identity identity = fetchOrCreateIdentity(rowNumber);

                Map<String, UKAddress> addressMap = new HashMap<>();
                for (int i = 0; i < identity.addresses().size(); i++) {
                    addressMap.put("" + i, identity.addresses().get(i));
                }
                ctx.render(
                        "edit-user.mustache",
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
                                isHmrcKbvCri)
                );
            };

    public Handler backendGenerateInitialClaimsSet =
            ctx -> {
                var credentialIssuerId = Objects.requireNonNull(ctx.queryParam("cri"));
                var credentialIssuer = handlerHelper.findCredentialIssuer(credentialIssuerId);

                Object claimIdentity = getClaimIdentity(ctx);
                String context = ctx.queryParam("context");

                State state = createNewState(credentialIssuer);
                LOGGER.info("Created State {} for {}", state.toJSONString(), credentialIssuerId);

                // ClaimSets can go direct to JSON
                ctx.contentType("application/json");
                ctx.result(handlerHelper.createJWTClaimsSets(
                        state,
                        credentialIssuer,
                        new ClientID(CoreStubConfig.CORE_STUB_CLIENT_ID),
                        claimIdentity,
                        getEvidenceRequestClaims(ctx),
                        context).toString());
            };

    private SharedClaims getClaimIdentity(Context ctx) {
        String claimsTextParam = ctx.queryParam("claimsText");
        String rowNumberParam = ctx.queryParam("rowNumber");
        if (isBlank(rowNumberParam) && isBlank(claimsTextParam)) {
            return null;
        }
        if (isNotBlank(rowNumberParam)) {
            return getClaimIdentityByRowNumber(
                    Integer.parseInt(rowNumberParam), ctx.queryParam("nino"));
        }
        return getClaimIdentityByClaimsText(claimsTextParam);
    }

    private SharedClaims getClaimIdentityByRowNumber(int rowNumber, String nino) {
        var identity = handlerHelper.findIdentityByRowNumber(rowNumber).withNino(nino);
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

    private EvidenceRequestClaims getEvidenceRequestClaims(Context ctx) {
        String scoringPolicy = ctx.queryParam("scoringPolicy");
        String strengthScore = ctx.queryParam("strengthScore");
        String verificationScore = ctx.queryParam("verificationScore");

        if (Objects.nonNull(strengthScore)
                || Objects.nonNull(scoringPolicy)
                || Objects.nonNull(verificationScore)) {

            return new EvidenceRequestClaims(
                    scoringPolicy,
                    Objects.isNull(strengthScore) ? null : Integer.parseInt(strengthScore),
                    Objects.isNull(verificationScore) ? null : Integer.parseInt(verificationScore));
        }
        return null;
    }

    public Handler backendGenerateInitialClaimsSetPostCode =
            ctx -> {
                var credentialIssuerId =
                        handlerHelper.findCredentialIssuer(
                                Objects.requireNonNull(ctx.queryParam("cri")));

                var postcode = ctx.queryParam("postcode");
                if (StringHelper.isBlank(postcode)) {
                    throw new IllegalStateException("Postcode cannot be blank");
                }

                PostcodeSharedClaims claimIdentity =
                        new IdentityMapper().mapToAddressSharedClaims(postcode);

                State state = createNewState(credentialIssuerId);
                LOGGER.info("Created State {} for {}", state.toJSONString(), credentialIssuerId);

                // ClaimSets can go direct to JSON
                ctx.contentType("application/json");
                ctx.result(handlerHelper.createJWTClaimsSets(
                        state,
                        credentialIssuerId,
                        new ClientID(CoreStubConfig.CORE_STUB_CLIENT_ID),
                        claimIdentity).toString());
            };
    public Handler createBackendSessionRequest =
            ctx -> {
                LOGGER.info("CreateBackendSessionRequest Start");
                var credentialIssuerId = Objects.requireNonNull(ctx.queryParam("cri"));
                var credentialIssuer = handlerHelper.findCredentialIssuer(credentialIssuerId);

                // The JSON data will have urls that will be escaped.
                LOGGER.info("Getting request body");
                var escapedData = Objects.requireNonNull(ctx.body(), "UTF-8");
                String data = escapedData.replace("\\", "");

                LOGGER.info("Parsing Request data to JWTClaimsSet {}", data);
                JWTClaimsSet claimsSet = JWTClaimsSet.parse(new Payload(data).toJSONObject());
                LOGGER.info("JWTClaimsSet Parsed!");

                AuthorizationRequest authorizationRequest =
                        createBackendAuthorizationRequest(credentialIssuer, claimsSet);

                // This uri can be pasted into a browser and Journey continued in the frontend.
                LOGGER.info("Auth URI {}", authorizationRequest.toURI());

                LOGGER.info("CreateBackendSessionRequest Complete");
                ctx.contentType("application/json");
                ctx.result(createBackendSessionRequestJSONReply(authorizationRequest));
            };

    public Handler createTokenRequestPrivateKeyJWT =
            ctx -> {
                LOGGER.info("createTokenRequestPrivateKeyJWT Start");
                var credentialIssuerId = Objects.requireNonNull(ctx.queryParam("cri"));
                var authorizationCode =
                        Objects.requireNonNull(ctx.queryParam("authorization_code"));

                var credentialIssuer = handlerHelper.findCredentialIssuer(credentialIssuerId);
                TokenRequest tokenRequest =
                        handlerHelper.createTokenRequest(
                                new AuthorizationCode(authorizationCode), credentialIssuer);
                ctx.result(tokenRequest.toHTTPRequest().getQuery());
            };

    public Handler editPostcode =
            ctx -> {
                LOGGER.info("editPostcode Start");
                var credentialIssuerId = Objects.requireNonNull(ctx.queryParam("cri"));
                ctx.render(
                        "edit-postcode.mustache", Map.of("cri", credentialIssuerId));
            };

    public Handler evidenceRequest =
            ctx -> {
                LOGGER.info("checkEvidence Requested Start");
                var credentialIssuerId = Objects.requireNonNull(ctx.queryParam("cri"));
                ctx.render(
                        "evidence-request.mustache", Map.of("cri", credentialIssuerId));
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
            Context ctx, String strengthScore, String scoringPolicy, String verificationScore) {
        if (Objects.nonNull(strengthScore)
                && Objects.nonNull(scoringPolicy)
                && Objects.nonNull(verificationScore)) {
            EvidenceRequestClaims evidenceRequest =
                    new EvidenceRequestClaims(
                            scoringPolicy,
                            Integer.parseInt(strengthScore),
                            Integer.parseInt(verificationScore));
            LOGGER.info("✅  Saving evidence request to session to {}", evidenceRequest);
            ctx.sessionAttribute("evidence_request", evidenceRequest);
        }
    }
}
