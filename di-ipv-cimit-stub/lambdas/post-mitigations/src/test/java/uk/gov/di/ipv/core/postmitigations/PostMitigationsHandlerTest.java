package uk.gov.di.ipv.core.postmitigations;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.core.library.service.CimitStubItemService;
import uk.gov.di.ipv.core.library.service.PendingMitigationService;
import uk.gov.di.ipv.core.postmitigations.domain.PostMitigationsRequestBody;
import uk.gov.di.ipv.core.postmitigations.domain.PostMitigationsResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static uk.gov.di.ipv.core.postmitigations.PostMitigationsHandler.FAILURE_RESPONSE;
import static uk.gov.di.ipv.core.postmitigations.PostMitigationsHandler.SUCCESS_RESPONSE;

@ExtendWith(MockitoExtension.class)
class PostMitigationsHandlerTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String VALID_JWT = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ1cm46dXVpZDo1ODg5MTg3NS02OWIzLTRjYzUtYWU3OS1hOTMxMzI0NTk3NDIiLCJhdWQiOiJodHRwczpcL1wvaWRlbnRpdHkuYnVpbGQuYWNjb3VudC5nb3YudWsiLCJuYmYiOjE2OTgwNzYwNDMsImlzcyI6Imh0dHBzOlwvXC9hZGRyZXNzLWNyaS5zdHVicy5hY2NvdW50Lmdvdi51ayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJJZGVudGl0eUNoZWNrQ3JlZGVudGlhbCJdLCJjcmVkZW50aWFsU3ViamVjdCI6eyJuYW1lIjpbeyJuYW1lUGFydHMiOlt7InR5cGUiOiJHaXZlbk5hbWUiLCJ2YWx1ZSI6Iktlbm5ldGgifSx7InR5cGUiOiJGYW1pbHlOYW1lIiwidmFsdWUiOiJEZWNlcnF1ZWlyYSJ9XX1dLCJiaXJ0aERhdGUiOlt7InZhbHVlIjoiMTk2NS0wNy0wOCJ9XSwiYWRkcmVzcyI6W3siYWRkcmVzc0NvdW50cnkiOiJHQiIsImJ1aWxkaW5nTmFtZSI6IiIsInN0cmVldE5hbWUiOiJIQURMRVkgUk9BRCIsInBvc3RhbENvZGUiOiJCQTIgNUFBIiwiYnVpbGRpbmdOdW1iZXIiOiI4IiwiYWRkcmVzc0xvY2FsaXR5IjoiQkFUSCIsInZhbGlkRnJvbSI6IjIwMDAtMDEtMDEifV19fSwianRpIjoidXJuOnV1aWQ6NmZhNTViZTAtODAwNC00YzdhLThiZWEtOGM2ODgwNmJjMWNjIn0.kEugKcCb1KNU-rDjaJ6jDcsPWtSHPbsM7PXm7N2o1OGT506-lFj23qEVxRQac-BSHKcVCk1FTKcE8FJwghRUEA"; // pragma: allowlist secret

    @Mock private Context mockContext;
    @Mock private PendingMitigationService mockPendingMitigationService;
    @Mock private CimitStubItemService mockCimitStubItemService;
    @InjectMocks private PostMitigationsHandler postMitigationsHandler;

    @Test
    void shouldReturnFailureWhenProvidedInvalidVCs() throws Exception {
        var request = new APIGatewayProxyRequestEvent();
        request.setHeaders(Map.of("govuk-signin-journey-id", "journeyId", "ip-address", "ip-address"));
        request.setBody(mapper.writeValueAsString(
                PostMitigationsRequestBody.builder().signedJwts(List.of("invalid_signed_jwt")).build()));

        var response = postMitigationsHandler.handleRequest(request, mockContext);

        PostMitigationsResponse parsedResponse = mapper.readValue(response.getBody(), new TypeReference<>() {});

        assertEquals(FAILURE_RESPONSE, parsedResponse.result());
        assertEquals(500, response.getStatusCode());
    }

    private static Stream<Arguments> provideInvalidRequests() {
        var validBody = PostMitigationsRequestBody.builder().signedJwts(List.of(VALID_JWT)).build();

        return Stream.of(
                Arguments.of(Map.of("ip-address", "ip-address"), validBody),
                Arguments.of(Map.of("govuk-signin-journey-id", "journeyId"), validBody),
                Arguments.of(Map.of("govuk-signin-journey-id", "journeyId", "ip-address", "ip-address"),
                        PostMitigationsRequestBody.builder().build()),
                Arguments.of(Map.of("govuk-signin-journey-id", "journeyId", "ip-address", "ip-address"),
                        PostMitigationsRequestBody.builder().signedJwts(List.of()).build()),
                Arguments.of(Map.of("govuk-signin-journey-id", "journeyId", "ip-address", "ip-address"), null)
        );
    }

    @MethodSource("provideInvalidRequests")
    @ParameterizedTest
    void shouldReturn400WithFailResultWhenJourneyGivenInvalidRequest(
            Map<String, String> requestHeaders, PostMitigationsRequestBody requestBody) throws IOException {
        var request = new APIGatewayProxyRequestEvent();
        request.setHeaders(requestHeaders);

        if (!Objects.isNull(requestBody)) {
            request.setBody(mapper.writeValueAsString(requestBody));
        }

        var response = postMitigationsHandler.handleRequest(request, mockContext);

        var parsedResponse = mapper.readValue(response.getBody(), PostMitigationsResponse.class);

        assertEquals(FAILURE_RESPONSE, parsedResponse.result());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    void shouldCompletePendingMitigations() throws Exception {
        var request = new APIGatewayProxyRequestEvent();
        request.setHeaders(Map.of("govuk-signin-journey-id", "journeyId", "ip-address", "ip-address"));
        request.setBody(mapper.writeValueAsString(
                PostMitigationsRequestBody.builder().signedJwts(
                        List.of(
                                "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ1cm46dXVpZDo1ODg5MTg3NS02OWIzLTRjYzUtYWU3OS1hOTMxMzI0NTk3NDIiLCJhdWQiOiJodHRwczpcL1wvaWRlbnRpdHkuYnVpbGQuYWNjb3VudC5nb3YudWsiLCJuYmYiOjE2OTgwNzYwNDMsImlzcyI6Imh0dHBzOlwvXC9hZGRyZXNzLWNyaS5zdHVicy5hY2NvdW50Lmdvdi51ayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJJZGVudGl0eUNoZWNrQ3JlZGVudGlhbCJdLCJjcmVkZW50aWFsU3ViamVjdCI6eyJuYW1lIjpbeyJuYW1lUGFydHMiOlt7InR5cGUiOiJHaXZlbk5hbWUiLCJ2YWx1ZSI6Iktlbm5ldGgifSx7InR5cGUiOiJGYW1pbHlOYW1lIiwidmFsdWUiOiJEZWNlcnF1ZWlyYSJ9XX1dLCJiaXJ0aERhdGUiOlt7InZhbHVlIjoiMTk2NS0wNy0wOCJ9XSwiYWRkcmVzcyI6W3siYWRkcmVzc0NvdW50cnkiOiJHQiIsImJ1aWxkaW5nTmFtZSI6IiIsInN0cmVldE5hbWUiOiJIQURMRVkgUk9BRCIsInBvc3RhbENvZGUiOiJCQTIgNUFBIiwiYnVpbGRpbmdOdW1iZXIiOiI4IiwiYWRkcmVzc0xvY2FsaXR5IjoiQkFUSCIsInZhbGlkRnJvbSI6IjIwMDAtMDEtMDEifV19fSwianRpIjoidXJuOnV1aWQ6NmZhNTViZTAtODAwNC00YzdhLThiZWEtOGM2ODgwNmJjMWNjIn0.kEugKcCb1KNU-rDjaJ6jDcsPWtSHPbsM7PXm7N2o1OGT506-lFj23qEVxRQac-BSHKcVCk1FTKcE8FJwghRUEA", // pragma: allowlist secret
                                "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ1cm46dXVpZDo1ODg5MTg3NS02OWIzLTRjYzUtYWU3OS1hOTMxMzI0NTk3NDIiLCJhdWQiOiJodHRwczpcL1wvaWRlbnRpdHkuYnVpbGQuYWNjb3VudC5nb3YudWsiLCJuYmYiOjE2OTgwNzYwMzMsImlzcyI6Imh0dHBzOlwvXC9kY21hdy1jcmkuc3R1YnMuYWNjb3VudC5nb3YudWsiLCJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiSWRlbnRpdHlDaGVja0NyZWRlbnRpYWwiXSwiY3JlZGVudGlhbFN1YmplY3QiOnsibmFtZSI6W3sibmFtZVBhcnRzIjpbeyJ0eXBlIjoiR2l2ZW5OYW1lIiwidmFsdWUiOiJLZW5uZXRoIn0seyJ0eXBlIjoiRmFtaWx5TmFtZSIsInZhbHVlIjoiRGVjZXJxdWVpcmEifV19XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5NjUtMDctMDgifV0sInBhc3Nwb3J0IjpbeyJleHBpcnlEYXRlIjoiMjAzMC0wMS0wMSIsImRvY3VtZW50TnVtYmVyIjoiMzIxNjU0OTg3In1dfSwiZXZpZGVuY2UiOlt7ImFjdGl2aXR5SGlzdG9yeVNjb3JlIjoxLCJjaGVja0RldGFpbHMiOlt7ImNoZWNrTWV0aG9kIjoidnJpIn0seyJiaW9tZXRyaWNWZXJpZmljYXRpb25Qcm9jZXNzTGV2ZWwiOjMsImNoZWNrTWV0aG9kIjoiYnZyIn1dLCJ2YWxpZGl0eVNjb3JlIjoyLCJzdHJlbmd0aFNjb3JlIjozLCJ0eXBlIjoiSWRlbnRpdHlDaGVjayIsInR4biI6IjMxMTUwM2IzLTA4MzEtNGY1OS1hOTQyLWEzNmJlOWI2MTlhNCJ9XX0sImp0aSI6InVybjp1dWlkOjRhZDY0OTAxLTE3MzUtNGIxZC1iYzhjLTAzODA4ZWQxMjgxNyJ9.VDnGLxpY_s6uXw1kDVSWRoKLoEAjfDv1iYZ1uC7YTk1uojPxhtn9RCfJRBFzAtTtEy1VwwcnOCBqkm9lQBJqtw", // pragma: allowlist secret
                                "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ1cm46dXVpZDo1ODg5MTg3NS02OWIzLTRjYzUtYWU3OS1hOTMxMzI0NTk3NDIiLCJhdWQiOiJodHRwczpcL1wvaWRlbnRpdHkuYnVpbGQuYWNjb3VudC5nb3YudWsiLCJuYmYiOjE2OTgwNzYwNTMsImlzcyI6Imh0dHBzOlwvXC9mcmF1ZC1jcmkuc3R1YnMuYWNjb3VudC5nb3YudWsiLCJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiSWRlbnRpdHlDaGVja0NyZWRlbnRpYWwiXSwiY3JlZGVudGlhbFN1YmplY3QiOnsibmFtZSI6W3sibmFtZVBhcnRzIjpbeyJ0eXBlIjoiR2l2ZW5OYW1lIiwidmFsdWUiOiJLZW5uZXRoIn0seyJ0eXBlIjoiRmFtaWx5TmFtZSIsInZhbHVlIjoiRGVjZXJxdWVpcmEifV19XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5NjUtMDctMDgifV0sImFkZHJlc3MiOlt7ImFkZHJlc3NDb3VudHJ5IjoiR0IiLCJidWlsZGluZ05hbWUiOiIiLCJzdHJlZXROYW1lIjoiSEFETEVZIFJPQUQiLCJwb3N0YWxDb2RlIjoiQkEyIDVBQSIsImJ1aWxkaW5nTnVtYmVyIjoiOCIsImFkZHJlc3NMb2NhbGl0eSI6IkJBVEgiLCJ2YWxpZEZyb20iOiIyMDAwLTAxLTAxIn1dfSwiZXZpZGVuY2UiOlt7ImlkZW50aXR5RnJhdWRTY29yZSI6MiwidHlwZSI6IklkZW50aXR5Q2hlY2siLCJ0eG4iOiJjMzYzNTQxNC03YjY2LTRjN2EtODJlNC1lZDgzNmVlNmVmZjEifV19LCJqdGkiOiJ1cm46dXVpZDo1MWRiMWQ2Ni1iNmRkLTQ0NWQtODdlNi00MzQwYjAyZGUyMWIifQ.6bL6VAKqVgEKwwg6YSISLln6in94GYcvmyoPQbK3jahobK9nxg4tCL5PRdq9i_XUPpQrnvX3I60m41U5-FlA5Q" // pragma: allowlist secret
                )).build()));

        var response = postMitigationsHandler.handleRequest(request, mockContext);

        verify(mockPendingMitigationService)
                .completePendingMitigation(
                        "urn:uuid:6fa55be0-8004-4c7a-8bea-8c68806bc1cc",
                        "urn:uuid:58891875-69b3-4cc5-ae79-a93132459742",
                        mockCimitStubItemService);
        verify(mockPendingMitigationService)
                .completePendingMitigation(
                        "urn:uuid:4ad64901-1735-4b1d-bc8c-03808ed12817",
                        "urn:uuid:58891875-69b3-4cc5-ae79-a93132459742",
                        mockCimitStubItemService);
        verify(mockPendingMitigationService)
                .completePendingMitigation(
                        "urn:uuid:51db1d66-b6dd-445d-87e6-4340b02de21b",
                        "urn:uuid:58891875-69b3-4cc5-ae79-a93132459742",
                        mockCimitStubItemService);

        PostMitigationsResponse parsedResponse = mapper.readValue(response.getBody(), new TypeReference<>() {});

        assertEquals(SUCCESS_RESPONSE, parsedResponse.result());
        assertEquals(200, response.getStatusCode());
    }
}
