package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.service.RequestedErrorResponseService;
import uk.gov.di.ipv.stub.cred.service.TokenService;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialHandlerTest {
    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "application/jwt;charset=UTF-8";
    private static final ValidationResult INVALID_REQUEST =
            new ValidationResult(false, OAuth2Error.INVALID_REQUEST);
    private static final ValidationResult INVALID_CLIENT =
            new ValidationResult(false, OAuth2Error.INVALID_CLIENT);

    @Mock private Context mockContext;
    @Mock private CredentialService mockCredentialService;
    @Mock private TokenService mockTokenService;
    @Mock private RequestedErrorResponseService mockRequestedErrorResponseService;
    private CredentialHandler resourceHandler;
    private AccessToken accessToken;

    @BeforeEach
    void setup() {
        accessToken = new BearerAccessToken();
        resourceHandler =
                new CredentialHandler(
                        mockCredentialService, mockTokenService, mockRequestedErrorResponseService);
    }

    @Test
    void shouldReturn201AndProtectedResourceWhenValidRequestReceived() throws Exception {
        when(mockTokenService.getPayload(accessToken.toAuthorizationHeader()))
                .thenReturn("aResourceId");
        when(mockTokenService.validateAccessToken(Mockito.anyString()))
                .thenReturn(ValidationResult.createValidResult());
        when(mockContext.header("Authorization")).thenReturn(accessToken.toAuthorizationHeader());
        when(mockCredentialService.getCredentialSignedJwt("aResourceId"))
                .thenReturn("A.VERIFIABLE.CREDENTIAL");

        resourceHandler.getResource(mockContext);

        verify(mockContext).contentType(DEFAULT_RESPONSE_CONTENT_TYPE);
        verify(mockContext).status(HttpStatus.CREATED);
        verify(mockContext).result("A.VERIFIABLE.CREDENTIAL");
        verify(mockTokenService, times(1)).getPayload(accessToken.toAuthorizationHeader());
        verify(mockTokenService).revoke(accessToken.toAuthorizationHeader());
    }

    @Test
    void shouldReturn400WhenAccessTokenIsNotProvided() throws Exception {
        when(mockTokenService.validateAccessToken(Mockito.any())).thenReturn(INVALID_REQUEST);

        resourceHandler.getResource(mockContext);

        verify(mockContext).status(HttpStatus.BAD_REQUEST.getCode());
        verify(mockContext).result("Invalid request");
    }

    @Test
    void shouldReturn400WhenIssuedAccessTokenDoesNotMatchRequestAccessToken() throws Exception {
        when(mockContext.header("Authorization")).thenReturn(accessToken.toAuthorizationHeader());
        when(mockTokenService.validateAccessToken(Mockito.any())).thenReturn(INVALID_CLIENT);

        resourceHandler.getResource(mockContext);

        verify(mockContext).status(HttpStatus.UNAUTHORIZED.getCode());
        verify(mockContext).result("Client authentication failed");
    }

    @Test
    void shouldReturn400WhenRequestAccessTokenIsNotValid() throws Exception {
        when(mockContext.header("Authorization")).thenReturn("invalid-token");
        when(mockTokenService.validateAccessToken(Mockito.any())).thenReturn(INVALID_CLIENT);

        resourceHandler.getResource(mockContext);

        verify(mockContext).status(HttpStatus.UNAUTHORIZED.getCode());
        verify(mockContext).result("Client authentication failed");
    }

    @Test
    void shouldReturnErrorWhenUserInfoErrorIsRequested() throws Exception {
        when(mockTokenService.validateAccessToken(Mockito.anyString()))
                .thenReturn(ValidationResult.createValidResult());
        when(mockContext.header("Authorization")).thenReturn(accessToken.toAuthorizationHeader());

        var expectedError =
                new UserInfoErrorResponse(
                        new ErrorObject(
                                "404",
                                String.format("UserInfo endpoint %s triggered by stub", "404"),
                                Integer.parseInt("404")));

        when(mockRequestedErrorResponseService.getUserInfoErrorByToken(any()))
                .thenReturn(expectedError);

        resourceHandler.getResource(mockContext);

        verify(mockContext).status(HttpStatus.NOT_FOUND.getCode());
        verify(mockContext).json(expectedError.getErrorObject().toJSONObject());
    }
}
