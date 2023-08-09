package uk.gov.di.ipv.core.stubmanagement;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.stubmanagement.exceptions.BadRequestException;
import uk.gov.di.ipv.core.stubmanagement.exceptions.DataNotFoundException;
import uk.gov.di.ipv.core.stubmanagement.model.UserCisRequest;
import uk.gov.di.ipv.core.stubmanagement.model.UserMitigationRequest;
import uk.gov.di.ipv.core.stubmanagement.service.UserService;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StubManagementHandlerTest {

    @Mock private UserService userService;
    @InjectMocks private StubManagementHandler stubManagementHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void shouldAddUserCisSuccessWhenValidCiRequestList() throws IOException {

        List<UserCisRequest> userCisRequests =
                List.of(
                        UserCisRequest.builder()
                                .code("code1")
                                .issuanceDate("2023-07-25T10:00:00Z")
                                .mitigations(List.of("V01", "V03"))
                                .build(),
                        UserCisRequest.builder()
                                .code("code2")
                                .issuanceDate("2023-07-25T10:00:00Z")
                                .mitigations(Collections.emptyList())
                                .build());

        APIGatewayProxyRequestEvent event =
                createTestEvent("POST", "/user/123/cis", userCisRequests);
        doNothing().when(userService).addUserCis(anyString(), anyList());

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("success"));
        verify(userService, times(1)).addUserCis(anyString(), anyList());
    }

    @Test
    public void shouldAUpdateUserCisSuccessWhenValidCiRequestList() throws IOException {
        UserCisRequest userCisRequest =
                UserCisRequest.builder()
                        .code("code1")
                        .issuanceDate("2023-07-25T10:00:00Z")
                        .mitigations(List.of("V01", "V03"))
                        .build();
        APIGatewayProxyRequestEvent event =
                createTestEvent("PUT", "/user/123/cis", Collections.singletonList(userCisRequest));
        doNothing().when(userService).updateUserCis(anyString(), anyList());

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("success"));
        verify(userService).updateUserCis(eq("123"), eq(Collections.singletonList(userCisRequest)));
    }

    @Test
    public void shouldAddUserMitigationSuccessWhenValidMitigationRequest() throws IOException {
        UserMitigationRequest userMitigationRequest =
                UserMitigationRequest.builder().mitigations(List.of("V01")).build();

        APIGatewayProxyRequestEvent event =
                createTestEvent("POST", "/user/123/mitigations/456", userMitigationRequest);
        doNothing().when(userService).addUserMitigation(anyString(), anyString(), any());

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("success"));
        verify(userService).addUserMitigation(eq("123"), eq("456"), eq(userMitigationRequest));
    }

    @Test
    public void shouldUpdateUserMitigationSuccessWhenValidMitigationRequest() throws IOException {
        UserMitigationRequest userMitigationRequest =
                UserMitigationRequest.builder().mitigations(List.of("V01")).build();
        APIGatewayProxyRequestEvent event =
                createTestEvent("PUT", "/user/123/mitigations/456", userMitigationRequest);
        doNothing().when(userService).updateUserMitigation(anyString(), anyString(), any());

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("success"));
        verify(userService).updateUserMitigation(eq("123"), eq("456"), eq(userMitigationRequest));
    }

    @Test
    public void shouldReturnBadRequestWhenInvalidEndpoint() throws IOException {
        APIGatewayProxyRequestEvent event = createTestEvent("POST", "/user/123/invalid", "{}");

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid URI."));
    }

    @Test
    public void shouldReturnBadRequestWhenInvalidRequestBodyForMitigation() throws IOException {
        APIGatewayProxyRequestEvent event =
                createTestEvent("POST", "/user/123/cis", "invalid json");

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid request body"));
    }

    @Test
    public void shouldReturnDataAlreadyExistForUserCis() throws IOException {
        UserCisRequest userCisRequest =
                UserCisRequest.builder()
                        .code("code1")
                        .issuanceDate("2023-07-25T10:00:00Z")
                        .mitigations(List.of("V01", "V03"))
                        .build();
        APIGatewayProxyRequestEvent event =
                createTestEvent("POST", "/user/123/cis", Collections.singletonList(userCisRequest));
        doThrow(new BadRequestException("User's CI Code cannot be null in all CIs"))
                .when(userService)
                .addUserCis(anyString(), any());

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("User's CI Code cannot be null in all CIs"));
    }

    @Test
    public void shouldReturnDataNotFoundForUserCis() throws IOException {
        UserCisRequest userCisRequest =
                UserCisRequest.builder()
                        .code("code1")
                        .issuanceDate("2023-07-25T10:00:00Z")
                        .mitigations(List.of("V01", "V03"))
                        .build();
        APIGatewayProxyRequestEvent event =
                createTestEvent("PUT", "/user/123/cis", Collections.singletonList(userCisRequest));
        doThrow(new DataNotFoundException("User and ContraIndicator not found."))
                .when(userService)
                .updateUserCis(anyString(), any());

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("User and ContraIndicator not found."));
    }

    @Test
    public void shouldReturnDataAlreadyExistForUserMitigations() throws IOException {
        UserMitigationRequest userMitigationRequest =
                UserMitigationRequest.builder().mitigations(List.of("V01")).build();
        APIGatewayProxyRequestEvent event =
                createTestEvent("POST", "/user/123/mitigations/456", userMitigationRequest);
        doThrow(new BadRequestException("User's CI Code cannot be null in all CIs"))
                .when(userService)
                .addUserMitigation(anyString(), anyString(), any());

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("User's CI Code cannot be null in all CIs"));
    }

    @Test
    public void shouldReturnDataNotFoundForUserMitigations() throws IOException {
        UserMitigationRequest userMitigationRequest =
                UserMitigationRequest.builder().mitigations(List.of("V01")).build();
        APIGatewayProxyRequestEvent event =
                createTestEvent("PUT", "/user/123/mitigations/456", userMitigationRequest);
        doThrow(new DataNotFoundException("User and ContraIndicator not found."))
                .when(userService)
                .updateUserMitigation(anyString(), anyString(), any());

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("User and ContraIndicator not found."));
    }

    @Test
    public void shouldReturnSuccessForValidPutRequestWithNoContent() throws IOException {
        APIGatewayProxyRequestEvent event = createTestEvent("PUT", "/user/123/cis", null);
        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(500, response.getStatusCode());
    }

    @Test
    public void shouldReturnSuccessForValidPutRequestWithEmptyContent() throws IOException {
        APIGatewayProxyRequestEvent event =
                createTestEvent("PUT", "/user/123/cis", Collections.emptyList());
        doNothing().when(userService).updateUserCis(anyString(), anyList());

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(200, response.getStatusCode());
        verify(userService).updateUserCis(eq("123"), eq(Collections.emptyList()));
    }

    @Test
    public void shouldReturnSuccessForValidPutRequestWithSingleContent() throws IOException {
        UserCisRequest userCisRequest =
                UserCisRequest.builder()
                        .code("code1")
                        .issuanceDate("2023-07-25T10:00:00Z")
                        .mitigations(List.of("V01", "V03"))
                        .build();

        APIGatewayProxyRequestEvent event =
                createTestEvent("PUT", "/user/123/cis", Collections.singletonList(userCisRequest));
        doNothing().when(userService).updateUserCis(anyString(), anyList());

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(200, response.getStatusCode());
        verify(userService).updateUserCis(eq("123"), eq(Collections.singletonList(userCisRequest)));
    }

    @Test
    public void shouldReturnInternalServerErrorForUnknownException() throws IOException {
        UserCisRequest userCisRequest =
                UserCisRequest.builder()
                        .code("code1")
                        .issuanceDate("2023-07-25T10:00:00Z")
                        .mitigations(List.of("V01", "V03"))
                        .build();
        APIGatewayProxyRequestEvent event =
                createTestEvent("POST", "/user/123/cis", Collections.singletonList(userCisRequest));
        doThrow(new RuntimeException("Unknown exception occurred."))
                .when(userService)
                .addUserCis(anyString(), anyList());

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Unknown exception occurred."));
    }

    @Test
    public void shouldReturnBadRequestForInvalidMethod() throws IOException {
        UserCisRequest userCisRequest =
                UserCisRequest.builder()
                        .code("code1")
                        .issuanceDate("2023-07-25T10:00:00Z")
                        .mitigations(List.of("V01", "V03"))
                        .build();
        APIGatewayProxyRequestEvent event =
                createTestEvent(
                        "PATCH", "/user/123/cis", Collections.singletonList(userCisRequest));

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Http Method is not supported."));
    }

    private APIGatewayProxyRequestEvent createTestEvent(String httpMethod, String path, Object body)
            throws JsonProcessingException {
        Map<String, String> pathParameters = new HashMap<>();
        pathParameters.put("userId", "123");
        pathParameters.put("ci", "456");

        APIGatewayProxyRequestEvent event =
                new APIGatewayProxyRequestEvent().withHttpMethod(httpMethod).withPath(path);

        event.setPathParameters(pathParameters);
        if (body != null) {
            event.setBody(objectMapper.writeValueAsString(body));
        }

        return event;
    }
}
