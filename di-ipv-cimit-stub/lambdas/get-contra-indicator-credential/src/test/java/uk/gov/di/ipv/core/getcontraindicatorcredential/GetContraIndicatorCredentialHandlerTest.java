package uk.gov.di.ipv.core.getcontraindicatorcredential;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.GetCiCredentialRequest;
import uk.gov.di.ipv.core.library.service.ConfigService;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({SystemStubsExtension.class, MockitoExtension.class})
class GetContraIndicatorCredentialHandlerTest {

    private static final String CIMIT_PRIVATE_KEY =
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgOXt0P05ZsQcK7eYusgIPsqZdaBCIJiW4imwUtnaAthWhRANCAAQT1nO46ipxVTilUH2umZPN7OPI49GU6Y8YkcqLxFKUgypUzGbYR2VJGM+QJXk0PI339EyYkt6tjgfS+RcOMQNO";
    private static final String CIMIT_COMPONENT_ID = "https://cimit.stubs.account.gov.uk";

    private static final ObjectMapper mapper = new ObjectMapper();
    @Mock private Context mockContext;
    @Mock private ConfigService mockConfigService;
    @InjectMocks private GetContraIndicatorCredentialHandler classToTest;

    @BeforeEach
    void setUp() {
        classToTest = new GetContraIndicatorCredentialHandler(mockConfigService);
    }

    @SystemStub
    private final EnvironmentVariables environmentVariables =
            new EnvironmentVariables("CIMIT_COMPONENT_ID", "https://cimit.stubs.account.gov.uk");

    @Test
    void shouldReturnSignedJwtWhenProvidedValidRequest() throws IOException {
        when(mockConfigService.getCimitSigningKey()).thenReturn(CIMIT_PRIVATE_KEY);
        when(mockConfigService.getCimitComponentId()).thenReturn(CIMIT_COMPONENT_ID);

        GetCiCredentialRequest getCiCredentialRequest =
                GetCiCredentialRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .userId("user_id")
                        .build();

        var response =
                makeRequest(
                        classToTest,
                        mapper.writeValueAsString(getCiCredentialRequest),
                        mockContext,
                        String.class);

        verify(mockConfigService).getCimitSigningKey();
        verify(mockConfigService).getCimitComponentId();

        assertNotNull(response);
        assertTrue(!response.equals("Failure"));
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
