package uk.gov.di.ipv.core.putcontraindicators;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.putcontraindicators.domain.PutContraIndicatorsRequest;
import uk.gov.di.ipv.core.putcontraindicators.domain.PutContraIndicatorsResponse;
import uk.gov.di.ipv.core.putcontraindicators.exceptions.CiPutException;
import uk.gov.di.ipv.core.putcontraindicators.service.ContraIndicatorsService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class PutContraIndicatorsHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Gson gson = new Gson();
    @Mock private Context mockContext;

    @Mock private ContraIndicatorsService mockCimitService;

    @InjectMocks private PutContraIndicatorsHandler classToTest;

    @Test
    void shouldReturnSuccessForValidRequest() throws IOException, CiPutException {
        PutContraIndicatorsRequest putContraIndicatorsRequest =
                PutContraIndicatorsRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .signedJwt("signed_jwt")
                        .build();

        Gson gson = new Gson();
        String expectedResponse =
                gson.toJson(PutContraIndicatorsResponse.builder().result("success").build());

        doNothing().when(mockCimitService).addUserCis(putContraIndicatorsRequest);

        var response =
                makeRequest(
                        classToTest,
                        objectMapper.writeValueAsString(putContraIndicatorsRequest),
                        mockContext,
                        PutContraIndicatorsResponse.class);

        assertNotNull(response);
        assertEquals(expectedResponse, objectMapper.writeValueAsString(response));
    }

    @Test
    void shouldThrowExceptionForInvalidRequest() throws IOException, CiPutException {
        assertThrows(
                IOException.class,
                () -> {
                    makeRequest(
                            classToTest,
                            objectMapper.writeValueAsString(""),
                            mockContext,
                            PutContraIndicatorsResponse.class);
                });
    }

    @Test
    void shouldReturnExceptionWhenCimitServiceThrowsException() throws IOException {
        PutContraIndicatorsRequest putContraIndicatorsRequest =
                PutContraIndicatorsRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .signedJwt("signed_jwt")
                        .build();

        String expectedResponse =
                gson.toJson(PutContraIndicatorsResponse.builder().result("fail").build());

        doThrow(new CiPutException("Failed to the CIs to the Cimit Stub Table"))
                .when(mockCimitService)
                .addUserCis(putContraIndicatorsRequest);

        var response =
                makeRequest(
                        classToTest,
                        objectMapper.writeValueAsString(putContraIndicatorsRequest),
                        mockContext,
                        PutContraIndicatorsResponse.class);

        assertNotNull(response);
        assertEquals(expectedResponse, objectMapper.writeValueAsString(response));
    }

    private <T extends PutContraIndicatorsResponse> T makeRequest(
            RequestStreamHandler handler, String request, Context context, Class<T> classType)
            throws IOException {
        try (var inputStream = new ByteArrayInputStream(request.getBytes());
                var outputStream = new ByteArrayOutputStream()) {
            handler.handleRequest(inputStream, outputStream, context);
            return objectMapper.readValue(outputStream.toString(), classType);
        } catch (Exception ex) {
            throw ex;
        }
    }
}
