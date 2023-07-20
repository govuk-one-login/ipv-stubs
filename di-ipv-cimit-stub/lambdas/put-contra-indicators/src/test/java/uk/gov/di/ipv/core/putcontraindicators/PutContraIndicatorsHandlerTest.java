package uk.gov.di.ipv.core.putcontraindicators;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.putcontraindicators.domain.PutContraIndicatorsRequest;
import uk.gov.di.ipv.core.putcontraindicators.domain.PutContraIndicatorsResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class PutContraIndicatorsHandlerTest {

    @Mock private Context mockContext;

    @InjectMocks private PutContraIndicatorsHandler classToTest;

    @Test
    void shouldReturnGetCiResponseWhenProvidedValidRequest() {
        PutContraIndicatorsRequest putContraIndicatorsRequest =
                PutContraIndicatorsRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .signedJwt("signed_jwt")
                        .build();

        Gson gson = new Gson();
        String expectedResponse =
                gson.toJson(PutContraIndicatorsResponse.builder().result("success").build());

        String actualResponse = classToTest.handleRequest(putContraIndicatorsRequest, mockContext);

        assertEquals(expectedResponse, actualResponse);
    }
}
