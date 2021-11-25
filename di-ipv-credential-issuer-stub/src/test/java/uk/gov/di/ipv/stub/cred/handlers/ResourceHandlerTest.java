package uk.gov.di.ipv.stub.cred.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spark.Request;
import spark.Response;
import uk.gov.di.ipv.stub.cred.entity.ProtectedResource;
import uk.gov.di.ipv.stub.cred.service.ProtectedResourceService;
import uk.gov.di.ipv.stub.cred.service.TokenService;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResourceHandlerTest {
    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "application/json;charset=UTF-8";

    private Response mockResponse;
    private Request mockRequest;
    private ProtectedResourceService mockProtectedResourceService;
    private TokenService mockTokenService;
    private ResourceHandler resourceHandler;
    private AccessToken accessToken;
    private ProtectedResource testProtectedResource;

    @BeforeEach
    void setup() {
        mockResponse = mock(Response.class);
        mockRequest = mock(Request.class);
        mockProtectedResourceService = mock(ProtectedResourceService.class);
        mockTokenService = mock(TokenService.class);

        accessToken = new BearerAccessToken();
        when(mockTokenService.getPayload(accessToken.toAuthorizationHeader())).thenReturn(UUID.randomUUID().toString());

        Map<String, Object> jsonAttributes = Map.of(
                "id", "12345",
                "evidenceType", "test-passport",
                "evidenceID", "test-passport-abc-12345"
        );
        testProtectedResource = new ProtectedResource(jsonAttributes);
        when(mockProtectedResourceService.getProtectedResource(anyString())).thenReturn(testProtectedResource);

        resourceHandler = new ResourceHandler(mockProtectedResourceService, mockTokenService);
    }

    @Test
    public void shouldReturn200AndProtectedResourceWhenValidRequestReceived() throws Exception {
        when(mockRequest.headers("Authorization")).thenReturn(accessToken.toAuthorizationHeader());

        String result = (String) resourceHandler.getResource.handle(mockRequest, mockResponse);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jsonMap = objectMapper.readValue(result, Map.class);

        assertEquals(testProtectedResource.getJsonAttributes().get("id"), jsonMap.get("id"));
        assertEquals(testProtectedResource.getJsonAttributes().get("evidenceType"), jsonMap.get("evidenceType"));
        assertEquals(testProtectedResource.getJsonAttributes().get("evidenceID"), jsonMap.get("evidenceID"));
        verify(mockResponse).type(DEFAULT_RESPONSE_CONTENT_TYPE);
        verify(mockResponse).status(HttpServletResponse.SC_OK);
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
    public void shouldReturn400WhenIssuedAccessTokenDoesNotMatchRequestAccessToken() throws Exception {
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
}
