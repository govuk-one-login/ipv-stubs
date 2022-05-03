package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spark.Request;
import spark.Response;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.service.TokenService;
import uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialGenerator;

import javax.servlet.http.HttpServletResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CredentialHandlerTest {
    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "application/jwt;charset=UTF-8";

    @Mock private Response mockResponse;
    @Mock private Request mockRequest;
    @Mock private CredentialService mockCredentialService;
    @Mock private TokenService mockTokenService;
    @Mock private VerifiableCredentialGenerator mockVerifiableCredentialGenerator;
    @Mock private SignedJWT mockSignedJwt;
    private CredentialHandler resourceHandler;
    private AccessToken accessToken;

    @BeforeEach
    void setup() {
        accessToken = new BearerAccessToken();
        resourceHandler =
                new CredentialHandler(
                        mockCredentialService, mockTokenService, mockVerifiableCredentialGenerator);
    }

    @Test
    public void shouldReturn201AndProtectedResourceWhenValidRequestReceived() throws Exception {
        PlainJWT requestJWT =
                new PlainJWT(
                        new JWTClaimsSet.Builder()
                                .claim("sub", "https://subject.example.com")
                                .build());
        when(mockRequest.body()).thenReturn(requestJWT.serialize());
        when(mockTokenService.getPayload(accessToken.toAuthorizationHeader()))
                .thenReturn(UUID.randomUUID().toString());
        when(mockRequest.headers("Authorization")).thenReturn(accessToken.toAuthorizationHeader());
        when(mockSignedJwt.serialize()).thenReturn("A.VERIFIABLE.CREDENTIAL");
        when(mockVerifiableCredentialGenerator.generate(any())).thenReturn(mockSignedJwt);

        Object response = resourceHandler.getResource.handle(mockRequest, mockResponse);

        assertEquals("A.VERIFIABLE.CREDENTIAL", response);
        verify(mockResponse).type(DEFAULT_RESPONSE_CONTENT_TYPE);
        verify(mockResponse).status(HttpServletResponse.SC_CREATED);
        verify(mockTokenService, times(2)).getPayload(accessToken.toAuthorizationHeader());
        verify(mockTokenService).revoke(accessToken.toAuthorizationHeader());
    }

    @Test
    public void shouldReturn400WhenAccessTokenIsNotProvided() throws Exception {
        String result = (String) resourceHandler.getResource.handle(mockRequest, mockResponse);

        assertEquals("Invalid request", result);
        verify(mockResponse).status(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void shouldReturn400WhenIssuedAccessTokenDoesNotMatchRequestAccessToken()
            throws Exception {
        when(mockRequest.headers("Authorization")).thenReturn(accessToken.toAuthorizationHeader());

        when(mockTokenService.getPayload(accessToken.toAuthorizationHeader())).thenReturn(null);

        String result = (String) resourceHandler.getResource.handle(mockRequest, mockResponse);

        assertEquals("Client authentication failed", result);
        verify(mockResponse).status(HttpServletResponse.SC_UNAUTHORIZED);
        verify(mockTokenService).getPayload(accessToken.toAuthorizationHeader());
    }

    @Test
    public void shouldReturn400WhenRequestAccessTokenIsNotValid() throws Exception {
        when(mockRequest.headers("Authorization")).thenReturn("invalid-token");

        String result = (String) resourceHandler.getResource.handle(mockRequest, mockResponse);

        assertEquals("Client authentication failed", result);
        verify(mockResponse).status(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void shouldReturn400WhenRequestBodyIsMissing() throws Exception {
        when(mockRequest.body()).thenReturn("");
        when(mockTokenService.getPayload(accessToken.toAuthorizationHeader()))
                .thenReturn(UUID.randomUUID().toString());
        when(mockRequest.headers("Authorization")).thenReturn(accessToken.toAuthorizationHeader());

        String result = (String) resourceHandler.getResource.handle(mockRequest, mockResponse);

        assertEquals("Error: No body found in request", result);
        verify(mockResponse).status(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    void shouldReturn500WhenErrorGeneratingVerifiableCredential() throws Exception {
        PlainJWT requestJWT =
                new PlainJWT(
                        new JWTClaimsSet.Builder()
                                .claim("sub", "https://subject.example.com")
                                .build());
        when(mockRequest.body()).thenReturn(requestJWT.serialize());
        when(mockTokenService.getPayload(accessToken.toAuthorizationHeader()))
                .thenReturn(UUID.randomUUID().toString());
        when(mockRequest.headers("Authorization")).thenReturn(accessToken.toAuthorizationHeader());
        when(mockVerifiableCredentialGenerator.generate(any())).thenThrow(JOSEException.class);

        String response = (String) resourceHandler.getResource.handle(mockRequest, mockResponse);

        assertTrue(response.contains("Error: Unable to generate VC -"));
        verify(mockResponse).status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
