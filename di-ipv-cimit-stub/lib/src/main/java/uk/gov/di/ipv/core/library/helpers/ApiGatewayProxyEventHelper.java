package uk.gov.di.ipv.core.library.helpers;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.utils.StringUtils;
import uk.gov.di.ipv.core.library.exceptions.FailedToParseRequestException;

import java.util.Collections;
import java.util.Objects;

public class ApiGatewayProxyEventHelper {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LogManager.getLogger();

    private ApiGatewayProxyEventHelper() {}

    public static APIGatewayProxyResponseEvent generateAPIGatewayProxyResponseEvent(Integer statusCode, Object body) {
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

    public static String getRequiredHeaderByKey(String key, APIGatewayProxyRequestEvent input)
            throws FailedToParseRequestException {
        return getHeaderByKey(key, input, true);
    }

    public static String getNonRequiredHeaderByKey(String key, APIGatewayProxyRequestEvent input)
            throws FailedToParseRequestException {
        return getHeaderByKey(key, input, false);
    }

    private static String getHeaderByKey(String key, APIGatewayProxyRequestEvent input, boolean isRequired)
            throws FailedToParseRequestException {
        var headers = input.getHeaders();
        if (Objects.isNull(headers)) {
            if (isRequired) {
                throw new FailedToParseRequestException("No headers present in request");
            }
            return null;
        }

        var headerValue = headers.get(key);

        if (isRequired && StringUtils.isBlank(headerValue)) {
            throw new FailedToParseRequestException(String.format("%s in request headers is empty", key));
        }

        return  headerValue;
    }

    public static String getRequiredQueryParamByKey(String key, APIGatewayProxyRequestEvent input)
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
}
