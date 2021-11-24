package uk.gov.di.ipv.stub.cred.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spark.Request;
import spark.Response;
import spark.Session;
import uk.gov.di.ipv.stub.cred.entity.ProtectedResource;
import uk.gov.di.ipv.stub.cred.service.ProtectedResourceService;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResourceHandlerTest {
    private static final String DEFAULT_RESPONSE_CONTENT_TYPE = "application/json";

    private Response mockResponse;
    private Request mockRequest;
    private ProtectedResourceService mockProtectedResourceService;
    private Session testSession;
    private ResourceHandler resourceHandler;
    private AccessToken accessToken;
    private ProtectedResource testProtectedResource;

    @BeforeEach
    void setup() {
        mockResponse = mock(Response.class);
        mockRequest = mock(Request.class);
        mockProtectedResourceService = mock(ProtectedResourceService.class);

        testSession = mock(Session.class);
        accessToken = new BearerAccessToken();
        when(testSession.attribute("accessToken")).thenReturn("Bearer " + accessToken.getValue());
        when(mockRequest.session()).thenReturn(testSession);

        Map<String, Object> jsonAttributes = Map.of(
                "id", "12345",
                "evidenceType", "test-passport",
                "evidenceID", "test-passport-abc-12345"
        );
        testProtectedResource = new ProtectedResource(jsonAttributes);
        when(mockProtectedResourceService.getProtectedResource()).thenReturn(testProtectedResource);

        resourceHandler = new ResourceHandler(mockProtectedResourceService);
    }

    @Test
    public void shouldReturn200AndProtectedResourceWhenValidRequestReceived() throws Exception {
        when(mockRequest.headers("Authorization")).thenReturn("Bearer " + accessToken.getValue());

        String result = (String) resourceHandler.getResource.handle(mockRequest, mockResponse);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jsonMap = objectMapper.readValue(result, Map.class);

        assertEquals(testProtectedResource.getJsonAttributes().get("id"), jsonMap.get("id"));
        assertEquals(testProtectedResource.getJsonAttributes().get("evidenceType"), jsonMap.get("evidenceType"));
        assertEquals(testProtectedResource.getJsonAttributes().get("evidenceID"), jsonMap.get("evidenceID"));
        verify(mockResponse).type(DEFAULT_RESPONSE_CONTENT_TYPE);
        verify(mockResponse).status(HttpServletResponse.SC_OK);
    }

    @Test
    public void shouldReturn400WhenAccessTokenIsNotProvided() throws Exception {
        String result = (String) resourceHandler.getResource.handle(mockRequest, mockResponse);

        assertEquals("an access token must be provided via the Authorization header", result);
        verify(mockResponse).status(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void shouldReturn400WhenSessionAccessTokenIsNotFound() throws Exception {
        when(mockRequest.headers("Authorization")).thenReturn("Bearer " + accessToken.getValue());

        when(testSession.attribute("accessToken")).thenReturn(null);

        String result = (String) resourceHandler.getResource.handle(mockRequest, mockResponse);

        assertEquals("failed to find session token", result);
        verify(mockResponse).status(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void shouldReturn400WhenSessionAccessTokenDoesNotMatchRequestAccessToken() throws Exception {
        when(mockRequest.headers("Authorization")).thenReturn("Bearer " + accessToken.getValue());

        when(testSession.attribute("accessToken")).thenReturn("Bearer " + new BearerAccessToken());

        String result = (String) resourceHandler.getResource.handle(mockRequest, mockResponse);

        assertEquals("access token from request does not match access token found in session", result);
        verify(mockResponse).status(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void shouldReturn400WhenRequestAccessTokenIsNotValid() throws Exception {
        when(mockRequest.headers("Authorization")).thenReturn("invalid-token");

        when(testSession.attribute("accessToken")).thenReturn("invalid-token");

        String result = (String) resourceHandler.getResource.handle(mockRequest, mockResponse);

        assertEquals("failed to parse the access token", result);
        verify(mockResponse).status(HttpServletResponse.SC_BAD_REQUEST);
    }
}
