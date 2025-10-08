package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.service.RequestedErrorResponseService;
import uk.gov.di.ipv.stub.cred.service.TokenService;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.stub.cred.fixtures.TestFixtures.DCMAW_VC;

@ExtendWith(MockitoExtension.class)
public class DocAppCredentialHandlerTest {
    private static final String JSON_RESPONSE_TYPE = "application/json;charset=UTF-8";
    private static final String SUBJECT = "urn:uuid:5d6d6833-8512-4e37-b5ea-be7de77948dd";
    private static final ValidationResult INVALID_REQUEST =
            new ValidationResult(false, OAuth2Error.INVALID_REQUEST);
    private static final ValidationResult INVALID_CLIENT =
            new ValidationResult(false, OAuth2Error.INVALID_CLIENT);

    @Mock private Context mockContext;
    @Mock private CredentialService mockCredentialService;
    @Mock private TokenService mockTokenService;
    @Mock private RequestedErrorResponseService mockRequestedErrorResponseService;
    @Captor private ArgumentCaptor<JSONObject> responseCaptor;
    private DocAppCredentialHandler resourceHandler;
    private AccessToken accessToken;

    @BeforeEach
    void setup() {
        accessToken = new BearerAccessToken();
        resourceHandler =
                new DocAppCredentialHandler(
                        mockCredentialService, mockTokenService, mockRequestedErrorResponseService);
    }

    @Test
    public void shouldReturn201AndUserInfoWhenValidRequestReceived() throws Exception {
        when(mockTokenService.getPayload(accessToken.toAuthorizationHeader()))
                .thenReturn("aResourceId");
        when(mockTokenService.validateAccessToken(Mockito.anyString()))
                .thenReturn(ValidationResult.createValidResult());
        when(mockContext.header("Authorization")).thenReturn(accessToken.toAuthorizationHeader());
        when(mockCredentialService.getCredentialSignedJwt("aResourceId")).thenReturn(DCMAW_VC);

        resourceHandler.getResource(mockContext);

        verify(mockContext).status(HttpStatus.CREATED);
        verify(mockContext).json(responseCaptor.capture());
        verify(mockTokenService, times(1)).getPayload(accessToken.toAuthorizationHeader());
        verify(mockTokenService).revoke(accessToken.toAuthorizationHeader());

        var docAppUserInfo = new UserInfo(responseCaptor.getValue());
        assertEquals(SUBJECT, docAppUserInfo.getSubject().getValue());
        assertNotNull(docAppUserInfo.getClaim("https://vocab.account.gov.uk/v1/credentialJWT"));
        List<?> verifiedCredentials =
                (List<?>) docAppUserInfo.getClaim("https://vocab.account.gov.uk/v1/credentialJWT");
        assertEquals(1, verifiedCredentials.size());
        assertEquals(DCMAW_VC, verifiedCredentials.get(0));
    }

    @Test
    public void shouldReturn400WhenAccessTokenIsNotProvided() throws Exception {
        when(mockTokenService.validateAccessToken(any())).thenReturn(INVALID_REQUEST);

        resourceHandler.getResource(mockContext);

        verify(mockContext).status(HttpStatus.BAD_REQUEST.getCode());
        verify(mockContext).result("Invalid request");
    }

    @Test
    public void shouldReturn400WhenIssuedAccessTokenDoesNotMatchRequestAccessToken()
            throws Exception {
        when(mockContext.header("Authorization")).thenReturn(accessToken.toAuthorizationHeader());
        when(mockTokenService.validateAccessToken(any())).thenReturn(INVALID_CLIENT);

        resourceHandler.getResource(mockContext);

        verify(mockContext).status(HttpStatus.UNAUTHORIZED.getCode());
        verify(mockContext).result("Client authentication failed");
    }

    @Test
    public void shouldReturn400WhenRequestAccessTokenIsNotValid() throws Exception {
        when(mockContext.header("Authorization")).thenReturn("invalid-token");
        when(mockTokenService.validateAccessToken(any())).thenReturn(INVALID_CLIENT);

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
