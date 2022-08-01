package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import net.minidev.json.JSONObject;
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

import java.util.ArrayList;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWithIgnoringCase;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DocAppCredentialHandlerTest {
    private static final String JSON_RESPONSE_TYPE = "application/json;charset=UTF-8";

    @Mock private Response mockResponse;
    @Mock private Request mockRequest;
    @Mock private CredentialService mockCredentialService;
    @Mock private TokenService mockTokenService;
    @Mock private VerifiableCredentialGenerator mockVerifiableCredentialGenerator;
    @Mock private SignedJWT mockSignedJwt;
    private DocAppCredentialHandler resourceHandler;
    private AccessToken accessToken;

    @BeforeEach
    void setup() {
        accessToken = new BearerAccessToken();
        resourceHandler =
                new DocAppCredentialHandler(
                        mockCredentialService, mockTokenService, mockVerifiableCredentialGenerator);
    }

    @Test
    public void shouldReturn201AndUserInfoWhenValidRequestReceived() throws Exception {
        when(mockTokenService.getPayload(accessToken.toAuthorizationHeader()))
                .thenReturn(UUID.randomUUID().toString());
        when(mockRequest.headers("Authorization")).thenReturn(accessToken.toAuthorizationHeader());
        when(mockSignedJwt.serialize()).thenReturn("A.VERIFIABLE.CREDENTIAL");
        when(mockVerifiableCredentialGenerator.generate(any())).thenReturn(mockSignedJwt);

        JSONObject jsonResponse =
                JSONObjectUtils.parse(
                        resourceHandler.getResource.handle(mockRequest, mockResponse).toString());

        UserInfo docAppUserInfo = new UserInfo(jsonResponse);

        assertThat(
                docAppUserInfo.getSubject().getValue(),
                startsWithIgnoringCase("urn:fdc:gov.uk:2022:"));
        assertThat(
                docAppUserInfo.getClaim("https://vocab.account.gov.uk/v1/credentialJWT"),
                notNullValue());
        ArrayList verifiedCredentials =
                (ArrayList)
                        docAppUserInfo.getClaim("https://vocab.account.gov.uk/v1/credentialJWT");
        assertThat(verifiedCredentials.size(), equalTo(1));
        assertThat(verifiedCredentials.get(0), equalTo("A.VERIFIABLE.CREDENTIAL"));

        verify(mockResponse).type(JSON_RESPONSE_TYPE);
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
    void shouldReturn500WhenErrorGeneratingVerifiableCredential() throws Exception {
        when(mockTokenService.getPayload(accessToken.toAuthorizationHeader()))
                .thenReturn(UUID.randomUUID().toString());
        when(mockRequest.headers("Authorization")).thenReturn(accessToken.toAuthorizationHeader());
        when(mockVerifiableCredentialGenerator.generate(any())).thenThrow(JOSEException.class);

        String response = (String) resourceHandler.getResource.handle(mockRequest, mockResponse);

        assertTrue(response.contains("Error: Unable to generate VC -"));
        verify(mockResponse).status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
