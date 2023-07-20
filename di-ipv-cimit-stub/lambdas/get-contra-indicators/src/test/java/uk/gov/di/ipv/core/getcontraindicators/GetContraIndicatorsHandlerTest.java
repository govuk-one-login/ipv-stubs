package uk.gov.di.ipv.core.getcontraindicators;

import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.getcontraindicators.domain.GetCiRequest;
import uk.gov.di.ipv.core.getcontraindicators.domain.GetCiResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class GetContraIndicatorsHandlerTest {
    @Mock private Context mockContext;
    @InjectMocks private GetContraIndicatorsHandler classToTest;

    @Test
    void shouldReturnGetCiResponseWhenProvidedValidRequest() {
        GetCiRequest getCiRequest =
                GetCiRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .userId("user_id")
                        .build();

        GetCiResponse actual = classToTest.handleRequest(getCiRequest, mockContext);
        assertTrue(actual.getContraIndicators().isEmpty());
    }
}
