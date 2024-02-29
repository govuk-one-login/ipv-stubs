package uk.gov.di.ipv.core.stubmanagement;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.core.library.model.UserCisRequest;
import uk.gov.di.ipv.core.library.model.UserMitigationRequest;
import uk.gov.di.ipv.core.library.service.CimitStubItemService;
import uk.gov.di.ipv.core.library.service.ConfigService;
import uk.gov.di.ipv.core.library.service.PendingMitigationService;
import uk.gov.di.ipv.core.stubmanagement.exceptions.BadRequestException;
import uk.gov.di.ipv.core.stubmanagement.exceptions.DataNotFoundException;
import uk.gov.di.ipv.core.stubmanagement.service.UserService;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class StubManagementHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final UserService userService;
    private final PendingMitigationService pendingMitigationService;
    private final CimitStubItemService cimitStubItemService;

    private static final Pattern CIS_PATTERN = Pattern.compile("^/user/[-a-zA-Z0-9_:]+/cis$");
    private static final Pattern CIS_MITIGATIONS =
            Pattern.compile("^/user/[-a-zA-Z0-9_:]+/mitigations/[-a-zA-Z0-9_]+$");
    private static final List<String> SUPPORTED_MITIGATION_METHODS =
            List.of(HttpMethod.POST.toString(), HttpMethod.PUT.toString());

    private static final String USER_ID_PATH_PARAMS = "userId";
    private static final String CI_PATH_PARAMS = "ci";

    public StubManagementHandler() {
        this.userService = new UserService();
        ConfigService configService = new ConfigService();
        this.pendingMitigationService = new PendingMitigationService(configService);
        this.cimitStubItemService = new CimitStubItemService(configService);
    }

    public StubManagementHandler(
            UserService userService,
            PendingMitigationService pendingMitigationService,
            CimitStubItemService cimitStubItemService) {
        this.userService = userService;
        this.pendingMitigationService = pendingMitigationService;
        this.cimitStubItemService = cimitStubItemService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event, Context context) {
        String httpMethod = event.getHttpMethod();
        try {
            String path = URLDecoder.decode(event.getPath(), StandardCharsets.UTF_8);
            LOGGER.info("Received '{}' event with path '{}'", httpMethod, path);
            Map<String, String> pathParameters = event.getPathParameters();
            String userId =
                    URLDecoder.decode(
                            pathParameters.get(USER_ID_PATH_PARAMS), StandardCharsets.UTF_8);
            if (CIS_PATTERN.matcher(path).matches()) {
                List<UserCisRequest> userCisRequests =
                        objectMapper.readValue(
                                event.getBody(),
                                objectMapper
                                        .getTypeFactory()
                                        .constructCollectionType(List.class, UserCisRequest.class));
                if (httpMethod.equals(HttpMethod.POST.toString())) {
                    userService.addUserCis(userId, userCisRequests);
                } else if (httpMethod.equals(HttpMethod.PUT.toString())) {
                    userService.updateUserCis(userId, userCisRequests);
                } else {
                    return buildErrorResponse("Http Method is not supported.", 400);
                }
            } else if (CIS_MITIGATIONS.matcher(path).matches()) {
                String ci = pathParameters.get(CI_PATH_PARAMS);
                UserMitigationRequest userMitigationRequest =
                        objectMapper.readValue(event.getBody(), UserMitigationRequest.class);
                if (SUPPORTED_MITIGATION_METHODS.contains(httpMethod)) {
                    if (cimitStubItemService.getCiForUserId(userId, ci) == null) {
                        throw new DataNotFoundException("User and ContraIndicator not found.");
                    }
                    pendingMitigationService.persistPendingMitigation(
                            userMitigationRequest, ci, httpMethod);
                } else {
                    return buildErrorResponse("Http Method is not supported.", 400);
                }
            } else {
                return buildErrorResponse("Invalid URI.", 400);
            }
            return buildSuccessResponse();
        } catch (IOException e) {
            LOGGER.error(String.format("IOException : %s", e.getMessage()));
            return buildErrorResponse("Invalid request body.", 400);
        } catch (BadRequestException e) {
            LOGGER.error(e.getMessage());
            return buildErrorResponse(e.getMessage(), 400);
        } catch (DataNotFoundException e) {
            return buildErrorResponse(e.getMessage(), 404);
        } catch (Exception e) {
            LOGGER.info(String.format("Exception : %s", e.getMessage()));
            return buildErrorResponse(e.getMessage(), 500);
        }
    }

    private APIGatewayProxyResponseEvent buildSuccessResponse() {
        return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("success");
    }

    private APIGatewayProxyResponseEvent buildErrorResponse(String message, int statusCode) {
        return new APIGatewayProxyResponseEvent().withStatusCode(statusCode).withBody(message);
    }
}
