package uk.gov.di.ipv.core.putcontraindicatorcredential;

import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.putcontraindicatorcredential.domain.PutContraIndicatorsRequest;

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

        assertEquals("Success", classToTest.handleRequest(putContraIndicatorsRequest, mockContext));
    }
}
