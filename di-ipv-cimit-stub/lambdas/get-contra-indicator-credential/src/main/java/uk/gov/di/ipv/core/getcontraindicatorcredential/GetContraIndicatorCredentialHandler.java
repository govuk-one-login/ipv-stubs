package uk.gov.di.ipv.core.getcontraindicatorcredential;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringMapMessage;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.GetCiCredentialErrorResponse;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.GetCiCredentialRequest;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.GetCiCredentialResponse;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential.ContraIndicator;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential.Evidence;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential.MitigatingCredential;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential.Mitigation;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential.VcClaim;
import uk.gov.di.ipv.core.getcontraindicatorcredential.exceptions.FailedToParseRequestException;
import uk.gov.di.ipv.core.getcontraindicatorcredential.factory.ECDSASignerFactory;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.service.CimitStubItemService;
import uk.gov.di.ipv.core.library.service.ConfigService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import static java.util.Comparator.comparing;
import static uk.gov.di.ipv.core.library.vc.VerifiableCredentialConstants.VC_CLAIM;

public class GetContraIndicatorCredentialHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String IP_ADDRESS_HEADER = "ip-address";
    private static final String GOVUK_SIGNIN_JOURNEY_ID = "govuk-signin-journey-id";
    private static final String USER_ID_QUERY_PARAM = "user_id";
    private static final String ERROR_DESCRIPTION_LOG_KEY = "errorDescription";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JWSHeader JWT_HEADER =
            new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build();
    private final ConfigService configService;
    private final CimitStubItemService cimitStubItemService;
    private final ECDSASignerFactory signerFactory;

    public GetContraIndicatorCredentialHandler() {
        this.configService = new ConfigService();
        this.cimitStubItemService = new CimitStubItemService(configService);
        this.signerFactory = new ECDSASignerFactory();
    }

    public GetContraIndicatorCredentialHandler(
            ConfigService configService,
            CimitStubItemService cimitStubItemService,
            ECDSASignerFactory signerFactory) {
        this.configService = configService;
        this.cimitStubItemService = cimitStubItemService;
        this.signerFactory = signerFactory;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            LOGGER.info(
                    new StringMapMessage().with("Function invoked:", "GetContraIndicatorCredential"));

            var parsedRequest = getParsedRequest(input);
            SignedJWT signedJWT = generateJWT(parsedRequest.getUserId());
            var ciCredential = new GetCiCredentialResponse(signedJWT.serialize());

            return generateAPIGatewayProxyResponseEvent(200, ciCredential);
        } catch (FailedToParseRequestException e) {
            LOGGER.error(
                    new StringMapMessage()
                        .with(
                                ERROR_DESCRIPTION_LOG_KEY,
                            "Failed tp parse request. Error message:"
                                    + e.getMessage()));
            return generateAPIGatewayProxyResponseEvent(
                    400,
                    new GetCiCredentialErrorResponse(e.getClass().getSimpleName(), e.getMessage()));
        } catch (JOSEException | NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
            LOGGER.error(
                    new StringMapMessage()
                            .with(
                                    ERROR_DESCRIPTION_LOG_KEY,
                                    "Failed at stub during creation of signedJwt. Error message:"
                                            + e.getMessage()));

            // It is possible to catch an exception here with a null message. Log the stack trace so
            // that we can see what is going on.
            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            printWriter.flush();

            LOGGER.error(
                    new StringMapMessage()
                            .with(
                                    ERROR_DESCRIPTION_LOG_KEY,
                                    "Failed at stub during creation of signedJwt. Error trace:"
                                            + writer));

            return generateAPIGatewayProxyResponseEvent(500, new GetCiCredentialErrorResponse(
                    e.getClass().getSimpleName(),
                    "Failed at stub during creation of signedJwt. Error message: "
                            + e.getMessage()));
        }
    }

    private APIGatewayProxyResponseEvent generateAPIGatewayProxyResponseEvent(Integer statusCode, Object body) {
        var response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);

        try {
            response.setBody(OBJECT_MAPPER.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            LOGGER.error("Unable to generate error response");
            response.setStatusCode(500);
            response.setBody("Internal server error");
            response.setHeaders(Collections.emptyMap());
        }
        return response;
    }

    private GetCiCredentialRequest getParsedRequest (APIGatewayProxyRequestEvent input)
            throws FailedToParseRequestException {
        return GetCiCredentialRequest.builder()
                .userId(getQueryParamByKey(USER_ID_QUERY_PARAM, input))
                .govukSigninJourneyId(getHeaderByKey(GOVUK_SIGNIN_JOURNEY_ID, input))
                .ipAddress(getHeaderByKey(IP_ADDRESS_HEADER, input))
                .build();
    }

    private String getHeaderByKey(String key, APIGatewayProxyRequestEvent input)
            throws FailedToParseRequestException {
        var headers = input.getHeaders();
        if (Objects.isNull(headers)) {
            throw new FailedToParseRequestException("No headers present in request");
        }

        var headerValue = headers.get(key);

        if (StringUtils.isBlank(headerValue)) {
            throw new FailedToParseRequestException(String.format("%s in request headers is empty", key));
        }

        return headerValue;
    }

    private String getQueryParamByKey(String key, APIGatewayProxyRequestEvent input)
            throws FailedToParseRequestException {
        var queryParams = input.getQueryStringParameters();

        if (Objects.isNull(queryParams)) {
            throw new FailedToParseRequestException("No query params present in request");
        }

        var queryParamValue = queryParams.get(key);

        if (StringUtils.isBlank(queryParamValue)) {
            throw new FailedToParseRequestException(String.format("%s in request query params is empty", key));
        }

        return queryParamValue;
    }

    private SignedJWT generateJWT(String userId)
            throws JOSEException, NoSuchAlgorithmException, InvalidKeySpecException {
        SignedJWT signedJWT = new SignedJWT(JWT_HEADER, generateClaimsSet(userId));
        signedJWT.sign(signerFactory.getSigner(configService.getCimitSigningKey()));

        return signedJWT;
    }

    private JWTClaimsSet generateClaimsSet(String userId) {
        var now = Instant.now();
        return new JWTClaimsSet.Builder()
                .claim(JWTClaimNames.SUBJECT, userId)
                .claim(JWTClaimNames.ISSUER, configService.getCimitComponentId())
                .claim(JWTClaimNames.NOT_BEFORE, now.getEpochSecond())
                .claim(JWTClaimNames.EXPIRATION_TIME, now.plusSeconds(60L * 15L).getEpochSecond())
                .claim(
                        VC_CLAIM,
                        OBJECT_MAPPER.convertValue(
                                generateVc(userId), new TypeReference<Map<String, Object>>() {}))
                .build();
    }

    private VcClaim generateVc(String userId) {
        var contraIndicators = getContraIndicators(userId);
        return new VcClaim(List.of(new Evidence(contraIndicators)));
    }

    private List<ContraIndicator> getContraIndicators(String userId) {
        var ciFromDatabase =
                cimitStubItemService.getCIsForUserId(userId).stream()
                        .sorted(comparing(CimitStubItem::getIssuanceDate))
                        .toList();

        var ciForVc = new ArrayList<ContraIndicator>();
        for (var datebaseCi : ciFromDatabase) {
            ciForVc.stream()
                    .filter(vcCi -> ciShouldBeMerged(datebaseCi, vcCi))
                    .findFirst()
                    .ifPresentOrElse(
                            vcCi -> {
                                vcCi.getIssuers().add(datebaseCi.getIssuer());
                                vcCi.setIssuanceDate(datebaseCi.getIssuanceDate().toString());
                                vcCi.setMitigation(getMitigations(datebaseCi.getMitigations()));
                                vcCi.setTxn(List.of(datebaseCi.getTxn()));
                            },
                            () -> ciForVc.add(createContraIndicator(datebaseCi)));
        }
        return ciForVc;
    }

    private boolean ciShouldBeMerged(
            CimitStubItem ciBeingProcessed, ContraIndicator ciAlreadyProcessed) {
        boolean ciCodesMatch =
                ciBeingProcessed.getContraIndicatorCode().equals(ciAlreadyProcessed.getCode());
        boolean documentsMatch =
                Objects.equals(ciAlreadyProcessed.getDocument(), ciBeingProcessed.getDocument());

        return ciCodesMatch && documentsMatch;
    }

    private ContraIndicator createContraIndicator(CimitStubItem item) {
        if (item.getIssuer() == null) {
            throw new InvalidParameterException("Stub item has null issuer");
        }
        if (item.getTxn() == null) {
            throw new InvalidParameterException("Stub item has null txn");
        }
        return new ContraIndicator(
                item.getContraIndicatorCode(),
                item.getDocument(),
                item.getIssuanceDate().toString(),
                new TreeSet<>(List.of(item.getIssuer())),
                getMitigations(item.getMitigations()),
                List.of(),
                List.of(item.getTxn()));
    }

    private List<Mitigation> getMitigations(List<String> mitigationCodes) {
        if (mitigationCodes == null) {
            LOGGER.warn("Mitigations on CimitStubItem are null");
            return List.of();
        }
        return mitigationCodes.stream()
                .map(ciCode -> new Mitigation(ciCode, List.of(MitigatingCredential.EMPTY)))
                .toList();
    }
}
