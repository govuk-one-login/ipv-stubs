package uk.gov.di.ipv.core.putcontraindicatorcredential;

import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.putcontraindicatorcredential.domain.PutContraIndicatorCredentialRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class PutContraIndicatorCredentialHandlerTest {

    @Mock private Context mockContext;

    @InjectMocks private PutContraIndicatorCredentialHandler classToTest;

    @Test
    void shouldReturnGetCiResponseWhenProvidedValidRequest() {
        PutContraIndicatorCredentialRequest putContraIndicatorCredentialRequest =
                PutContraIndicatorCredentialRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .signedJwt("signed_jwts")
                        .build();

        assertEquals(
                "Success",
                classToTest.handleRequest(putContraIndicatorCredentialRequest, mockContext));
    }
}
