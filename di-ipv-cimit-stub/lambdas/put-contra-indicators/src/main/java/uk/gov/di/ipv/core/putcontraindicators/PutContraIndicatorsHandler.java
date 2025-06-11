package uk.gov.di.ipv.core.putcontraindicators;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringMapMessage;
import uk.gov.di.ipv.core.library.exceptions.FailedToParseRequestException;
import uk.gov.di.ipv.core.putcontraindicators.domain.PutContraIndicatorsRequest;
import uk.gov.di.ipv.core.putcontraindicators.domain.PutContraIndicatorsRequestBody;
import uk.gov.di.ipv.core.putcontraindicators.domain.PutContraIndicatorsResponse;
import uk.gov.di.ipv.core.putcontraindicators.exceptions.CiPutException;
import uk.gov.di.ipv.core.putcontraindicators.service.ContraIndicatorsService;

import java.util.Objects;

import static uk.gov.di.ipv.core.library.helpers.ApiGatewayProxyEventHelper.generateAPIGatewayProxyResponseEvent;
import static uk.gov.di.ipv.core.library.helpers.ApiGatewayProxyEventHelper.getNonRequiredHeaderByKey;

public class PutContraIndicatorsHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String GOVUK_SIGNIN_JOURNEY_ID_HEADER = "govuk-signin-journey-id";
    private static final String IP_ADDRESS_HEADER = "ip-address";
    private static final String SUCCESS_RESULT = "success";
    private static final String FAIL_RESULT = "fail";

    private final ContraIndicatorsService cimitService;

    public PutContraIndicatorsHandler() {
        this.cimitService = new ContraIndicatorsService();
    }

    public PutContraIndicatorsHandler(ContraIndicatorsService cimitService) {
        this.cimitService = cimitService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        LOGGER.info(new StringMapMessage().with("Function invoked:", "PutContraIndicators"));

        try {
            var parsedRequest = getParsedRequest(input);
            cimitService.addUserCis(parsedRequest);

            return generateAPIGatewayProxyResponseEvent(
                    200,
                    PutContraIndicatorsResponse.builder()
                            .result(SUCCESS_RESULT)
                            .reason(null)
                            .errorMessage(null)
                            .build());
        } catch (FailedToParseRequestException e) {
            LOGGER.error(
                    new StringMapMessage().with("Unable to parse input request", e.getMessage()));
            return generateAPIGatewayProxyResponseEvent(
                    400,
                    PutContraIndicatorsResponse.builder()
                            .result(FAIL_RESULT)
                            .reason(e.getClass().getSimpleName())
                            .errorMessage(e.getMessage())
                            .build());
        } catch (CiPutException e) {
            LOGGER.error(
                    new StringMapMessage()
                            .with(
                                    "CI Codes could not be inserted into the Cimit Stub Table.",
                                    e.getMessage()));

            return generateAPIGatewayProxyResponseEvent(
                    500,
                    PutContraIndicatorsResponse.builder()
                            .result(FAIL_RESULT)
                            .reason(e.getClass().getSimpleName())
                            .errorMessage(e.getMessage())
                            .build());
        } catch (Exception e) {
            LOGGER.error(
                    new StringMapMessage()
                            .with("Failed to insert CI due to unexpected error.", e.getMessage()));

            return generateAPIGatewayProxyResponseEvent(
                    500,
                    PutContraIndicatorsResponse.builder()
                            .result(FAIL_RESULT)
                            .reason(e.getClass().getSimpleName())
                            .errorMessage(e.getMessage())
                            .build());
        }
    }

    private PutContraIndicatorsRequest getParsedRequest(APIGatewayProxyRequestEvent input)
            throws FailedToParseRequestException {
        try {
            var requestBody = input.getBody();

            if (Objects.isNull(requestBody)) {
                throw new FailedToParseRequestException("Missing request body");
            }

            var parsedBody =
                    OBJECT_MAPPER.readValue(requestBody, PutContraIndicatorsRequestBody.class);
            var signedJwt = parsedBody.getSignedJwt();

            if (StringUtils.isBlank(signedJwt)) {
                throw new FailedToParseRequestException("signed_jwt is empty");
            }

            return PutContraIndicatorsRequest.builder()
                    .govukSigninJourneyId(
                            getNonRequiredHeaderByKey(GOVUK_SIGNIN_JOURNEY_ID_HEADER, input))
                    .ipAddress(getNonRequiredHeaderByKey(IP_ADDRESS_HEADER, input))
                    .signedJwt(signedJwt)
                    .build();

        } catch (JsonProcessingException e) {
            throw new FailedToParseRequestException(
                    String.format("Failed to parse request body: %s", e.getMessage()));
        }
    }
}
