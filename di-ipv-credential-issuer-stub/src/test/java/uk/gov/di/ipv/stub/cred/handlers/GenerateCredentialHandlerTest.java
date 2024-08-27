package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.jwt.SignedJWT;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.stub.cred.domain.GenerateCredentialRequest;
import uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialGenerator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.stub.cred.handlers.GenerateCredentialHandler.JWT_CONTENT_TYPE;

@ExtendWith(MockitoExtension.class)
public class GenerateCredentialHandlerTest {
    @Mock private Context mockContext;
    @Mock private VerifiableCredentialGenerator mockCredentialGenerator;
    @InjectMocks private GenerateCredentialHandler generateCredentialHandler;

    @Test
    void shouldGenerateAndReturnCredential() throws Exception {
        // arrange
        var testCredentialRequest =
                new GenerateCredentialRequest(
                        "test-user-id",
                        "test-client-id",
                        "{\"subject\": \"foo\"}",
                        "{\"evidence\": \"bar\"}",
                        null);
        when(mockContext.bodyAsClass(GenerateCredentialRequest.class))
                .thenReturn(testCredentialRequest);
        var mockJwt = mock(SignedJWT.class);
        when(mockCredentialGenerator.generate(any())).thenReturn(mockJwt);
        var testVcString = "test-vc-string";
        when(mockJwt.toString()).thenReturn(testVcString);

        // act
        generateCredentialHandler.generateCredential(mockContext);

        // assert
        verify(mockContext).contentType(JWT_CONTENT_TYPE);
        verify(mockContext).status(HttpStatus.CREATED);
        verify(mockContext).result(testVcString);
    }
}
