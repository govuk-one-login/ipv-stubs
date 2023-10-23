package uk.gov.di.ipv.core.stubmanagement;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.library.model.UserMitigationRequest;
import uk.gov.di.ipv.core.library.persistence.items.CimitStubItem;
import uk.gov.di.ipv.core.library.service.CimitStubItemService;
import uk.gov.di.ipv.core.library.service.PendingMitigationService;
import uk.gov.di.ipv.core.stubmanagement.exceptions.BadRequestException;
import uk.gov.di.ipv.core.stubmanagement.exceptions.DataNotFoundException;
import uk.gov.di.ipv.core.stubmanagement.model.UserCisRequest;
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

    @Mock private UserService mockUserService;
    @Mock private PendingMitigationService mockPendingMitigationService;
    @Mock private CimitStubItemService mockCimitStubItemService;
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
        doNothing().when(mockUserService).addUserCis(anyString(), anyList());

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("success"));
        verify(mockUserService, times(1)).addUserCis(anyString(), anyList());
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
        doNothing().when(mockUserService).updateUserCis(anyString(), anyList());

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("success"));
        verify(mockUserService)
                .updateUserCis(eq("123"), eq(Collections.singletonList(userCisRequest)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void shouldAddPendingMitigationWhenValidMitigationRequest(String method) throws IOException {
        when(mockCimitStubItemService.getCiForUserId("123", "456")).thenReturn(new CimitStubItem());

        UserMitigationRequest userMitigationRequest =
                UserMitigationRequest.builder().mitigations(List.of("V01")).build();

        APIGatewayProxyRequestEvent event =
                createTestEvent(method, "/user/123/mitigations/456", userMitigationRequest);

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("success"));
        verify(mockPendingMitigationService)
                .persistPendingMitigation(userMitigationRequest, "456", method);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void shouldReturn404IfCiItemNotFoundForMitigationRequest(String method) throws Exception {
        when(mockCimitStubItemService.getCiForUserId("123", "456")).thenReturn(null);

        UserMitigationRequest userMitigationRequest =
                UserMitigationRequest.builder().mitigations(List.of("V01")).build();

        APIGatewayProxyRequestEvent event =
                createTestEvent(method, "/user/123/mitigations/456", userMitigationRequest);

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("not found"));
        verify(mockPendingMitigationService, times(0))
                .persistPendingMitigation(userMitigationRequest, "456", method);
    }

    @Test
    void cisPatternShouldHandleDefaultUserIdFormat() throws Exception {
        String urlEncodedUserId = "urn%3Auuid%3Ac08630f8-330e-43f8-a782-21432a197fc5";
        List<UserCisRequest> userCisRequests =
                List.of(
                        UserCisRequest.builder()
                                .code("code1")
                                .issuanceDate("2023-07-25T10:00:00Z")
                                .mitigations(List.of("V01", "V03"))
                                .build());

        APIGatewayProxyRequestEvent event =
                createTestEvent(
                        "POST", String.format("/user/%s/cis", urlEncodedUserId), userCisRequests);
        doNothing().when(mockUserService).addUserCis(anyString(), anyList());

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("success"));
        verify(mockUserService, times(1)).addUserCis(anyString(), anyList());
    }

    @Test
    void mitigationsPatternShouldHandleDefaultUserIdFormat() throws Exception {
        when(mockCimitStubItemService.getCiForUserId("123", "456")).thenReturn(new CimitStubItem());

        String urlEncodedUserId = "urn%3Auuid%3Ac08630f8-330e-43f8-a782-21432a197fc5";
        UserMitigationRequest userMitigationRequest =
                UserMitigationRequest.builder().mitigations(List.of("V01")).build();

        APIGatewayProxyRequestEvent event =
                createTestEvent(
                        "POST",
                        String.format("/user/%s/mitigations/456", urlEncodedUserId),
                        userMitigationRequest);

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("success"));
        verify(mockPendingMitigationService)
                .persistPendingMitigation(userMitigationRequest, "456", "POST");
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
                .when(mockUserService)
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
                .when(mockUserService)
                .updateUserCis(anyString(), any());

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
        doNothing().when(mockUserService).updateUserCis(anyString(), anyList());

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(200, response.getStatusCode());
        verify(mockUserService).updateUserCis(eq("123"), eq(Collections.emptyList()));
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
        doNothing().when(mockUserService).updateUserCis(anyString(), anyList());

        APIGatewayProxyResponseEvent response =
                stubManagementHandler.handleRequest(event, mock(Context.class));

        assertEquals(200, response.getStatusCode());
        verify(mockUserService)
                .updateUserCis(eq("123"), eq(Collections.singletonList(userCisRequest)));
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
                .when(mockUserService)
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
