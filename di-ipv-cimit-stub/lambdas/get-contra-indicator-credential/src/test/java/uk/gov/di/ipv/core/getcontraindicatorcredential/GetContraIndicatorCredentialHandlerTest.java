package uk.gov.di.ipv.core.getcontraindicatorcredential;

import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.getcontraindicatorcredential.domain.GetCiCredentialRequest;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({SystemStubsExtension.class, MockitoExtension.class})
class GetContraIndicatorCredentialHandlerTest {

    private static final String CIMIT_PRIVATE_KEY =
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgOXt0P05ZsQcK7eYusgIPsqZdaBCIJiW4imwUtnaAthWhRANCAAQT1nO46ipxVTilUH2umZPN7OPI49GU6Y8YkcqLxFKUgypUzGbYR2VJGM+QJXk0PI339EyYkt6tjgfS+RcOMQNO";

    @Mock private Context mockContext;
    @InjectMocks private GetContraIndicatorCredentialHandler classToTest;

    @SystemStub
    private final EnvironmentVariables environmentVariables =
            new EnvironmentVariables(
                    "CIMIT_SIGNING_KEY",
                    CIMIT_PRIVATE_KEY,
                    "CIMIT_COMPONENT_ID",
                    "https://cimit.stubs.account.gov.uk");

    @Test
    void shouldReturnSignedJwtWhenProvidedValidRequest() {
        GetCiCredentialRequest getCiCredentialRequest =
                GetCiCredentialRequest.builder()
                        .govukSigninJourneyId("govuk_signin_journey_id")
                        .ipAddress("ip_address")
                        .userId("user_id")
                        .build();

        String response = classToTest.handleRequest(getCiCredentialRequest, mockContext);
        assertNotNull(response);
    }
}
