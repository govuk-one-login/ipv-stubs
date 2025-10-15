package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.jwt.SignedJWT;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.UnauthorizedResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.stub.cred.domain.GenerateCredentialRequest;
import uk.gov.di.ipv.stub.cred.service.ConfigService;
import uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialGenerator;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.stub.cred.handlers.GenerateCredentialHandler.JWT_CONTENT_TYPE;

@ExtendWith(MockitoExtension.class)
class GenerateCredentialHandlerTest {
    @Mock private Context mockContext;
    @Mock private VerifiableCredentialGenerator mockCredentialGenerator;
    @InjectMocks private GenerateCredentialHandler generateCredentialHandler;

    @Test
    void shouldGenerateAndReturnCredential() throws Exception {
        try (MockedStatic<ConfigService> mockedConfigService = mockStatic(ConfigService.class)) {
            // arrange
            var testCredentialRequest =
                    new GenerateCredentialRequest(
                            "test-user-id",
                            "test-client-id",
                            "{\"subject\": \"foo\"}",
                            "{\"evidence\": \"bar\"}",
                            null);
            var testApiKey = "test-api-key";
            when(mockContext.bodyAsClass(GenerateCredentialRequest.class))
                    .thenReturn(testCredentialRequest);
            when(mockContext.header("x-api-key")).thenReturn(testApiKey);
            var mockJwt = mock(SignedJWT.class);
            when(mockCredentialGenerator.generate(any())).thenReturn(mockJwt);
            var testVcString = "test-vc-string";
            when(mockJwt.serialize()).thenReturn(testVcString);
            mockedConfigService.when(ConfigService::getApiKey).thenReturn(testApiKey);

            // act
            generateCredentialHandler.generateCredential(mockContext);

            // assert
            verify(mockContext).contentType(JWT_CONTENT_TYPE);
            verify(mockContext).status(HttpStatus.CREATED);
            verify(mockContext).result(testVcString);
        }
    }

    @Test
    void shouldThrowUnauthorizedIfApiKeyIsInvalid() {
        try (MockedStatic<ConfigService> mockedConfigService = mockStatic(ConfigService.class)) {
            // arrange
            var testApiKey = "test-api-key";
            when(mockContext.header("x-api-key")).thenReturn("invalid-api-key");
            mockedConfigService.when(ConfigService::getApiKey).thenReturn(testApiKey);

            // act/assert
            assertThrows(
                    UnauthorizedResponse.class,
                    () -> generateCredentialHandler.generateCredential(mockContext));
        }
    }
}
