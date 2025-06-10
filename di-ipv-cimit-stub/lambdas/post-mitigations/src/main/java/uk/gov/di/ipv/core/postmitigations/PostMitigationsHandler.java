package uk.gov.di.ipv.core.postmitigations;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringMapMessage;
import uk.gov.di.ipv.core.library.exceptions.FailedToParseRequestException;
import uk.gov.di.ipv.core.library.service.CimitStubItemService;
import uk.gov.di.ipv.core.library.service.ConfigService;
import uk.gov.di.ipv.core.library.service.PendingMitigationService;
import uk.gov.di.ipv.core.postmitigations.domain.PostMitigationsRequest;
import uk.gov.di.ipv.core.postmitigations.domain.PostMitigationsRequestBodyDto;
import uk.gov.di.ipv.core.postmitigations.domain.PostMitigationsResponse;

import java.text.ParseException;
import java.util.Objects;

import static uk.gov.di.ipv.core.library.helpers.ApiGatewayProxyEventHelper.generateAPIGatewayProxyResponseEvent;
import static uk.gov.di.ipv.core.library.helpers.ApiGatewayProxyEventHelper.getRequiredHeaderByKey;

public class PostMitigationsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String GOVUK_SIGNIN_JOURNEY_ID_HEADER = "govuk-signin-journey-id";
    private static final String IP_ADDRESS_HEADER = "ip-address";
    public static final String FAILURE_RESPONSE = "fail";
    public static final String SUCCESS_RESPONSE = "success";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final CimitStubItemService cimitStubItemService;
    private final PendingMitigationService pendingMitigationService;

    public PostMitigationsHandler() {
        ConfigService configService = new ConfigService();
        this.pendingMitigationService = new PendingMitigationService(configService);
        this.cimitStubItemService = new CimitStubItemService(configService);
    }

    public PostMitigationsHandler(
            PendingMitigationService pendingMitigationService,
            CimitStubItemService cimitStubItemService) {
        this.pendingMitigationService = pendingMitigationService;
        this.cimitStubItemService = cimitStubItemService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context)
             {
        LOGGER.info(new StringMapMessage().with("Function invoked:", "PostMitigations"));

        String response;
        try {
            PostMitigationsRequest postMitigationsRequest = getParsedRequest(input);
            for (String vc : postMitigationsRequest.getSignedJwtVCs()) {
                JWTClaimsSet jwtClaimsSet = SignedJWT.parse(vc).getJWTClaimsSet();
                pendingMitigationService.completePendingMitigation(
                        jwtClaimsSet.getJWTID(), jwtClaimsSet.getSubject(), cimitStubItemService);
            }
            response = SUCCESS_RESPONSE;
            return generateAPIGatewayProxyResponseEvent(
                    200,
                    new PostMitigationsResponse(response, null, null));
        } catch (ParseException | FailedToParseRequestException e) {
            LOGGER.error(
                    new StringMapMessage().with("Unable to parse input request", e.getMessage()));
            response = FAILURE_RESPONSE;
            return generateAPIGatewayProxyResponseEvent(
                    400,
                    new PostMitigationsResponse(response, e.getClass().getSimpleName(), e.getMessage()));
        } catch (Exception e) {
            LOGGER.error(new StringMapMessage().with("Unexpected error from lambda", e.getMessage()));
            response = FAILURE_RESPONSE;
            return generateAPIGatewayProxyResponseEvent(
                    500,
                    new PostMitigationsResponse(response, e.getClass().getSimpleName(), e.getMessage()));
        }
    }

    private PostMitigationsRequest getParsedRequest(APIGatewayProxyRequestEvent input) throws FailedToParseRequestException {
        try {
            var requestBody = input.getBody();

            if (Objects.isNull(requestBody)) {
                throw new FailedToParseRequestException("Missing request body");
            }

            var parsedBody = MAPPER.readValue(requestBody, PostMitigationsRequestBodyDto.class);

            return PostMitigationsRequest.builder()
                    .govukSigninJourneyId(getRequiredHeaderByKey(GOVUK_SIGNIN_JOURNEY_ID_HEADER, input))
                    .ipAddress(getRequiredHeaderByKey(IP_ADDRESS_HEADER, input))
                    .signedJwtVCs(parsedBody.getSignedJwtVcs())
                    .build();
        } catch (JsonProcessingException e) {
            throw new FailedToParseRequestException("Failed to parse request body");
        }
    }

}
