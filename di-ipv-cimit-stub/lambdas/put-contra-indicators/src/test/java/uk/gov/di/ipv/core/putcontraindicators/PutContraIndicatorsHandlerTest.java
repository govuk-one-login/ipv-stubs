package uk.gov.di.ipv.core.putcontraindicators;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.putcontraindicators.domain.PutContraIndicatorsRequestBody;
import uk.gov.di.ipv.core.putcontraindicators.domain.PutContraIndicatorsResponse;
import uk.gov.di.ipv.core.putcontraindicators.exceptions.CiPutException;
import uk.gov.di.ipv.core.putcontraindicators.service.ContraIndicatorsService;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class PutContraIndicatorsHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mock private Context mockContext;

    @Mock private ContraIndicatorsService mockCimitService;

    @InjectMocks private PutContraIndicatorsHandler putContraIndicatorsHandler;

    private static Stream<Arguments> provideValidRequests() {
        return Stream.of(
                Arguments.of(Map.of("ip-address", "ip-address")),
                Arguments.of(Map.of("govuk-signin-journey-id", "journeyId")),
                Arguments.of(Map.of("govuk-signin-journey-id", "journeyId", "ip-address", "ip-address")),
                Arguments.of(Map.of())
        );
    }

    @MethodSource("provideValidRequests")
    @ParameterizedTest
    void shouldReturnSuccessForValidRequest(Map<String, String> requestHeaders) throws Exception {
        var request = new APIGatewayProxyRequestEvent();
        request.setHeaders(requestHeaders);
        request.setBody(objectMapper.writeValueAsString(
                PutContraIndicatorsRequestBody.builder().signedJwt("signed_jwt").build()));

        doNothing().when(mockCimitService).addUserCis(any());

        var response = putContraIndicatorsHandler.handleRequest(request, mockContext);

        assertEquals(200, response.getStatusCode());
        var parsedResponse = objectMapper.readValue(response.getBody(), PutContraIndicatorsResponse.class);
        assertEquals("success", parsedResponse.getResult());
    }

    private static Stream<Arguments> provideInvalidRequests() {
        return Stream.of(
                Arguments.of(Map.of("govuk-signin-journey-id", "journeyId", "ip-address", "ip-address"),
                        PutContraIndicatorsRequestBody.builder().build()),
                Arguments.of(Map.of("govuk-signin-journey-id", "journeyId", "ip-address", "ip-address"), null)
        );
    }

    @MethodSource("provideInvalidRequests")
    @ParameterizedTest
    void shouldReturn400GivenAnInvalidRequest(
            Map<String, String> requestHeaders, PutContraIndicatorsRequestBody requestBody) throws Exception {
        var request = new APIGatewayProxyRequestEvent();
        request.setHeaders(requestHeaders);

        if (!Objects.isNull(requestBody)) {
            request.setBody(objectMapper.writeValueAsString(requestBody));
        }

        var response = putContraIndicatorsHandler.handleRequest(request, mockContext);

        assertEquals(400, response.getStatusCode());
        var parsedResponse = objectMapper.readValue(response.getBody(), PutContraIndicatorsResponse.class);
        assertEquals("fail", parsedResponse.getResult());
    }

    @Test
    void shouldReturnExceptionWhenCimitServiceThrowsException() throws IOException {
        var request = new APIGatewayProxyRequestEvent();
        request.setHeaders(Map.of("govuk-signin-journey-id", "journeyId", "ip-address", "ip-address"));
        request.setBody(objectMapper.writeValueAsString(
                PutContraIndicatorsRequestBody.builder().signedJwt("signed_jwt").build()));

        doThrow(new CiPutException("Failed to the CIs to the Cimit Stub Table"))
                .when(mockCimitService)
                .addUserCis(any());

        var response = putContraIndicatorsHandler.handleRequest(request, mockContext);

        assertEquals(500, response.getStatusCode());
        var parsedResponse = objectMapper.readValue(response.getBody(), PutContraIndicatorsResponse.class);
        assertEquals("fail", parsedResponse.getResult());
    }
}
