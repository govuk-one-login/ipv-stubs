package uk.gov.di.ipv.core.stubmanagement;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.core.stubmanagement.exceptions.DataAlreadyExistException;
import uk.gov.di.ipv.core.stubmanagement.exceptions.DataNotFoundException;
import uk.gov.di.ipv.core.stubmanagement.model.UserCisRequest;
import uk.gov.di.ipv.core.stubmanagement.model.UserMitigationRequest;
import uk.gov.di.ipv.core.stubmanagement.service.UserService;
import uk.gov.di.ipv.core.stubmanagement.service.impl.UserServiceImpl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class StubManagementHandler
        implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final UserService userService;

    private static final Pattern CIS_PATTERN = Pattern.compile("^/user/(\\d+)/cis$");
    private static final Pattern CIS_MITIGATIONS =
            Pattern.compile("^/user/(\\d+)/mitigations/(\\d+)$");

    private static final String USER_ID_PATH_PARAMS = "userId";
    private static final String CI_PATH_PARAMS = "ci";

    public StubManagementHandler() {
        userService = new UserServiceImpl();
    }

    public StubManagementHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        String httpMethod = event.getRequestContext().getHttp().getMethod();
        String path = event.getRequestContext().getHttp().getPath();

        Map<String, String> pathParameters = event.getPathParameters();
        String userId = pathParameters.get(USER_ID_PATH_PARAMS);

        try {
            if (httpMethod.equals(HttpMethod.POST.toString())
                    && CIS_PATTERN.matcher(path).matches()) {
                List<UserCisRequest> userCisRequests =
                        objectMapper.readValue(
                                event.getBody(),
                                objectMapper
                                        .getTypeFactory()
                                        .constructCollectionType(List.class, UserCisRequest.class));
                userService.addUserCis(userId, userCisRequests);
            } else if (httpMethod.equals(HttpMethod.PUT.toString())
                    && CIS_PATTERN.matcher(path).matches()) {
                List<UserCisRequest> userCisRequests =
                        objectMapper.readValue(
                                event.getBody(),
                                objectMapper
                                        .getTypeFactory()
                                        .constructCollectionType(List.class, UserCisRequest.class));
                userService.updateUserCis(userId, userCisRequests);
            } else if (httpMethod.equals(HttpMethod.POST.toString())
                    && CIS_MITIGATIONS.matcher(path).matches()) {
                String ci = pathParameters.get(CI_PATH_PARAMS);
                UserMitigationRequest userMitigationRequest =
                        objectMapper.readValue(event.getBody(), UserMitigationRequest.class);
                userService.addUserMitigation(userId, ci, userMitigationRequest);
            } else if (httpMethod.equals(HttpMethod.PUT.toString())
                    && CIS_MITIGATIONS.matcher(path).matches()) {
                String ci = pathParameters.get(CI_PATH_PARAMS);
                UserMitigationRequest userMitigationRequest =
                        objectMapper.readValue(event.getBody(), UserMitigationRequest.class);
                userService.updateUserMitigation(userId, ci, userMitigationRequest);
            } else {
                return buildErrorResponse("Invalid endpoint", 400);
            }
            return buildSuccessResponse(200);
        } catch (IOException e) {
            LOGGER.info("IOException :" + e.getMessage());
            return buildErrorResponse("Invalid request body", 400);
        } catch (DataAlreadyExistException e) {
            return buildErrorResponse(e.getMessage(), 409);
        } catch (DataNotFoundException e) {
            return buildErrorResponse(e.getMessage(), 404);
        } catch (Exception e) {
            LOGGER.info("Exception :" + e.getMessage());
            return buildErrorResponse(e.getMessage(), 500);
        }
    }

    private APIGatewayV2HTTPResponse buildSuccessResponse(int statusCode) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(statusCode)
                .withBody("success")
                .build();
    }

    private APIGatewayV2HTTPResponse buildErrorResponse(String message, int statusCode) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(statusCode)
                .withBody(message)
                .build();
    }
}
