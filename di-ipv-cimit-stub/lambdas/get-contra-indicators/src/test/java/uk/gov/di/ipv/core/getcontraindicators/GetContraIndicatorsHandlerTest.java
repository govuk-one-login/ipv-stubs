package uk.gov.di.ipv.core.getcontraindicators;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.getcontraindicators.domain.GetCiRequest;
import uk.gov.di.ipv.core.getcontraindicators.domain.GetCiResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class GetContraIndicatorsHandlerTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    @Mock private Context mockContext;
    @InjectMocks private GetContraIndicatorsHandler classToTest;

    @Test
    void shouldReturnGetCiResponseWhenProvidedValidRequest() throws IOException {
        GetCiRequest getCiRequest =
                GetCiRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .userId("user_id")
                        .build();

        var response =
                makeRequest(
                        classToTest,
                        mapper.writeValueAsString(getCiRequest),
                        mockContext,
                        GetCiResponse.class);
        assertTrue(response.getContraIndicators().isEmpty());
    }

    @Test
    void shouldReturnFailureWhenProvidedInValidRequest() throws IOException {
        assertThrows(
                IOException.class,
                () -> {
                    makeRequest(
                            classToTest,
                            mapper.writeValueAsString(""),
                            mockContext,
                            GetCiResponse.class);
                });
    }

    private <T extends GetCiResponse> T makeRequest(
            RequestStreamHandler handler, String request, Context context, Class<T> classType)
            throws IOException {
        try (var inputStream = new ByteArrayInputStream(request.getBytes());
                var outputStream = new ByteArrayOutputStream()) {
            handler.handleRequest(inputStream, outputStream, context);
            return mapper.readValue(outputStream.toString(), classType);
        } catch (Exception ex) {
            throw ex;
        }
    }
}
