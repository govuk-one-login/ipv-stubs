package uk.gov.di.ipv.core.postmitigations;

import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.postmitigations.domain.PostMitigationsRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class PostMitigationsHandlerTest {

    @Mock private Context mockContext;

    @InjectMocks private PostMitigationsHandler classToTest;

    @Test
    void shouldReturnGetCiResponseWhenProvidedValidRequest() {
        PostMitigationsRequest postMitigationsRequest =
                PostMitigationsRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .signedJwtVCs(List.of("signed_jwts"))
                        .build();

        assertEquals("Success", classToTest.handleRequest(postMitigationsRequest, mockContext));
    }
}
