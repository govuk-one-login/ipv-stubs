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
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.service.CimitStubItemService;
import uk.gov.di.ipv.core.library.service.ConfigService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetContraIndicatorsHandlerTest {

    public static final String USER_ID = "user_id";
    public static final String CI_V_03 = "V03";
    private static final ObjectMapper mapper = new ObjectMapper();
    @Mock private Context mockContext;
    @Mock private ConfigService mockConfigService;
    @Mock private CimitStubItemService mockCimitStubItemService;
    @InjectMocks private GetContraIndicatorsHandler classToTest;

    @Test
    void shouldReturnEmptyCIsResponseWhenProvidedValidRequest() throws IOException {
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
    void shouldReturnGetCiResponseWhenProvidedValidRequest() throws IOException {
        GetCiRequest getCiRequest =
                GetCiRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .userId("user_id")
                        .build();
        List<CimitStubItem> cimitStubItems = new ArrayList<>();
        cimitStubItems.add(
                CimitStubItem.builder()
                        .userId(USER_ID)
                        .contraIndicatorCode(CI_V_03)
                        .issuanceDate(Instant.now())
                        .ttl(505L)
                        .build());
        when(mockCimitStubItemService.getCIsForUserId(USER_ID)).thenReturn(cimitStubItems);

        var response =
                makeRequest(
                        classToTest,
                        mapper.writeValueAsString(getCiRequest),
                        mockContext,
                        GetCiResponse.class);

        assertFalse(response.getContraIndicators().isEmpty());
        assertEquals(CI_V_03, response.getContraIndicators().get(0).getCi());
    }

    @Test
    void shouldReturnGetCiResponseWhenProvidedValidRequestAndNoCiInDb() throws IOException {
        GetCiRequest getCiRequest =
                GetCiRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .userId("user_id")
                        .build();
        List<CimitStubItem> cimitStubItems = new ArrayList<>();

        when(mockCimitStubItemService.getCIsForUserId(USER_ID)).thenReturn(Collections.emptyList());

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
