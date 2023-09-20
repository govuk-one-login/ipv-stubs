package uk.gov.di.ipv.core.stubmanagement;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.core.stubmanagement.exceptions.BadRequestException;
import uk.gov.di.ipv.core.stubmanagement.exceptions.DataNotFoundException;
import uk.gov.di.ipv.core.stubmanagement.model.UserCisRequest;
import uk.gov.di.ipv.core.stubmanagement.model.UserMitigationRequest;
import uk.gov.di.ipv.core.stubmanagement.service.UserService;
import uk.gov.di.ipv.core.stubmanagement.service.impl.UserServiceImpl;

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

    private static final Pattern CIS_PATTERN = Pattern.compile("^/user/[-a-zA-Z0-9_:]+/cis$");
    private static final Pattern CIS_MITIGATIONS =
            Pattern.compile("^/user/[-a-zA-Z0-9_:]+/mitigations/[-a-zA-Z0-9_]+$");

    private static final String USER_ID_PATH_PARAMS = "userId";
    private static final String CI_PATH_PARAMS = "ci";

    public StubManagementHandler() {
        userService = new UserServiceImpl();
    }

    public StubManagementHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event, Context context) {
        String httpMethod = event.getHttpMethod();
        try {
            String path = URLDecoder.decode(event.getPath(), StandardCharsets.UTF_8.toString());
            LOGGER.info("Received '{}' event with path '{}'", httpMethod, path);
            Map<String, String> pathParameters = event.getPathParameters();
            String userId =
                    URLDecoder.decode(
                            pathParameters.get(USER_ID_PATH_PARAMS),
                            StandardCharsets.UTF_8.toString());
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
                if (httpMethod.equals(HttpMethod.POST.toString())) {
                    userService.addUserMitigation(userId, ci, userMitigationRequest);
                } else if (httpMethod.equals(HttpMethod.PUT.toString())) {
                    userService.updateUserMitigation(userId, ci, userMitigationRequest);
                } else {
                    return buildErrorResponse("Http Method is not supported.", 400);
                }
            } else {
                return buildErrorResponse("Invalid URI.", 400);
            }
            return buildSuccessResponse();
        } catch (IOException e) {
            LOGGER.error("IOException :" + e.getMessage());
            return buildErrorResponse("Invalid request body.", 400);
        } catch (BadRequestException e) {
            LOGGER.error(e.getMessage());
            return buildErrorResponse(e.getMessage(), 400);
        } catch (DataNotFoundException e) {
            return buildErrorResponse(e.getMessage(), 404);
        } catch (Exception e) {
            LOGGER.info("Exception :" + e.getMessage());
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
