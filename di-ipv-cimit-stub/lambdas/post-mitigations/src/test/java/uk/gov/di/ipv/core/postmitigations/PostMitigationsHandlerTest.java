package uk.gov.di.ipv.core.postmitigations;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.postmitigations.domain.PostMitigationsRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.di.ipv.core.postmitigations.PostMitigationsHandler.FAILURE_RESPONSE;

@ExtendWith(MockitoExtension.class)
class PostMitigationsHandlerTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Mock private Context mockContext;
    @InjectMocks private PostMitigationsHandler classToTest;

    @Test
    void shouldReturnGetCiResponseWhenProvidedValidRequest() throws IOException {
        PostMitigationsRequest postMitigationsRequest =
                PostMitigationsRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .signedJwtVCs(List.of("signed_jwts"))
                        .build();

        var response =
                makeRequest(
                        classToTest,
                        mapper.writeValueAsString(postMitigationsRequest),
                        mockContext,
                        String.class);

        assertEquals("Success", response);
    }

    @Test
    void shouldReturnFailureWhenProvidedInValidRequest() throws IOException {
        var response =
                makeRequest(classToTest, mapper.writeValueAsString(""), mockContext, String.class);

        assertEquals(FAILURE_RESPONSE, response);
    }

    private <T extends String> T makeRequest(
            RequestStreamHandler handler, String request, Context context, Class<T> classType)
            throws IOException {
        try (var inputStream = new ByteArrayInputStream(request.getBytes());
                var outputStream = new ByteArrayOutputStream()) {
            handler.handleRequest(inputStream, outputStream, context);
            return mapper.readValue(outputStream.toString(), classType);
        }
    }
}
