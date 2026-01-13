jest.mock("../../../src/common/pendingMitigationService", () => ({
  completePendingMitigation: jest.fn().mockImplementation(async () => {}),
}));

import * as pendingMitigationService from "../../../src/common/pendingMitigationService";

import {
  APIGatewayProxyEvent,
  APIGatewayProxyEventHeaders,
  APIGatewayProxyStructuredResultV2,
} from "aws-lambda";

import {
  postMitigationsHandler,
  PostMitigationsResponse,
  PostMitigationsRequest,
} from "../../../src/internal-api/post-mitigations/postMitigationsHandler";

const VALID_JWT =
  "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ1cm46dXVpZDo1ODg5MTg3NS02OWIzLTRjYzUtYWU3OS1hOTMxMzI0NTk3NDIiLCJhdWQiOiJodHRwczpcL1wvaWRlbnRpdHkuYnVpbGQuYWNjb3VudC5nb3YudWsiLCJuYmYiOjE2OTgwNzYwNDMsImlzcyI6Imh0dHBzOlwvXC9hZGRyZXNzLWNyaS5zdHVicy5hY2NvdW50Lmdvdi51ayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJJZGVudGl0eUNoZWNrQ3JlZGVudGlhbCJdLCJjcmVkZW50aWFsU3ViamVjdCI6eyJuYW1lIjpbeyJuYW1lUGFydHMiOlt7InR5cGUiOiJHaXZlbk5hbWUiLCJ2YWx1ZSI6Iktlbm5ldGgifSx7InR5cGUiOiJGYW1pbHlOYW1lIiwidmFsdWUiOiJEZWNlcnF1ZWlyYSJ9XX1dLCJiaXJ0aERhdGUiOlt7InZhbHVlIjoiMTk2NS0wNy0wOCJ9XSwiYWRkcmVzcyI6W3siYWRkcmVzc0NvdW50cnkiOiJHQiIsImJ1aWxkaW5nTmFtZSI6IiIsInN0cmVldE5hbWUiOiJIQURMRVkgUk9BRCIsInBvc3RhbENvZGUiOiJCQTIgNUFBIiwiYnVpbGRpbmdOdW1iZXIiOiI4IiwiYWRkcmVzc0xvY2FsaXR5IjoiQkFUSCIsInZhbGlkRnJvbSI6IjIwMDAtMDEtMDEifV19fSwianRpIjoidXJuOnV1aWQ6NmZhNTViZTAtODAwNC00YzdhLThiZWEtOGM2ODgwNmJjMWNjIn0.kEugKcCb1KNU-rDjaJ6jDcsPWtSHPbsM7PXm7N2o1OGT506-lFj23qEVxRQac-BSHKcVCk1FTKcE8FJwghRUEA"; // pragma: allowlist secret

const SUCCESS_RESPONSE = "success";
const FAILURE_RESPONSE = "fail";

const buildPostMitigationsRequest = (
  headers: APIGatewayProxyEventHeaders = {
    "govuk-signin-journey-id": "journeyId",
    "ip-address": "ip-address",
  },
  body?: PostMitigationsRequest,
): APIGatewayProxyEvent => {
  return {
    headers,
    body: body ? JSON.stringify(body) : undefined,
  } as APIGatewayProxyEvent;
};

beforeEach(() => {
  jest.resetAllMocks();
});

test("should return failure when provided invalid VCs", async () => {
  // Arrange
  const request = buildPostMitigationsRequest(
    { "govuk-signin-journey-id": "journeyId", "ip-address": "ip-address" },
    { signed_jwts: ["invalid_signed_jwt"] },
  );
  // Act
  const response = (await postMitigationsHandler(
    request,
  )) as APIGatewayProxyStructuredResultV2;
  // Assert
  expect(JSON.parse(response.body || "").errorMessage).toBe("Invalid JWT");
  expect(response.statusCode).toBe(500);
});

test.each([
  { case: "with ip-address only", headers: { "ip-address": "ip-address" } },
  {
    case: "with govuk-signin-journey-id only",
    headers: { "govuk-signin-journey-id": "journeyId" },
  },
  {
    case: "with both headers",
    headers: {
      "govuk-signin-journey-id": "journeyId",
      "ip-address": "ip-address",
    },
  },
  { case: "with no headers", headers: {} },
])(
  "should post mitigations given valid request - $case",
  async ({ headers }) => {
    // Arrange
    const request = buildPostMitigationsRequest(headers, {
      signed_jwts: [VALID_JWT],
    });
    // Act
    const response = (await postMitigationsHandler(
      request,
    )) as APIGatewayProxyStructuredResultV2;
    // Assert
    const parsedResponse = JSON.parse(
      response.body!,
    ) as PostMitigationsResponse;
    expect(parsedResponse.result).toBe(SUCCESS_RESPONSE);
    expect(response.statusCode).toBe(200);
  },
);

test.each([
  {
    case: "missing signed_jwts",
    headers: {
      "govuk-signin-journey-id": "journeyId",
      "ip-address": "ip-address",
    },
    body: {} as PostMitigationsRequest,
  },
  {
    case: "empty signed_jwts array",
    headers: {
      "govuk-signin-journey-id": "journeyId",
      "ip-address": "ip-address",
    },
    body: { signed_jwts: [] },
  },
  {
    case: "null body",
    headers: {
      "govuk-signin-journey-id": "journeyId",
      "ip-address": "ip-address",
    },
    body: undefined,
  },
])(
  "should return 400 with fail result when given invalid request - $case",
  async ({ headers, body }) => {
    // Arrange
    const request = buildPostMitigationsRequest(headers, body);

    // Act
    const response = (await postMitigationsHandler(
      request,
    )) as APIGatewayProxyStructuredResultV2;

    // Assert
    const parsedResponse = JSON.parse(
      response.body!,
    ) as PostMitigationsResponse;
    expect(parsedResponse.result).toBe(FAILURE_RESPONSE);
    expect(response.statusCode).toBe(400);
  },
);

test("should complete pending mitigations", async () => {
  // Arrange
  const mockCompletePendingMitigations = jest
    .spyOn(pendingMitigationService, "completePendingMitigation")
    .mockImplementation(async () => {});

  const request = buildPostMitigationsRequest(
    { "govuk-signin-journey-id": "journeyId", "ip-address": "ip-address" },
    {
      signed_jwts: [
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ1cm46dXVpZDo1ODg5MTg3NS02OWIzLTRjYzUtYWU3OS1hOTMxMzI0NTk3NDIiLCJhdWQiOiJodHRwczpcL1wvaWRlbnRpdHkuYnVpbGQuYWNjb3VudC5nb3YudWsiLCJuYmYiOjE2OTgwNzYwNDMsImlzcyI6Imh0dHBzOlwvXC9hZGRyZXNzLWNyaS5zdHVicy5hY2NvdW50Lmdvdi51ayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJJZGVudGl0eUNoZWNrQ3JlZGVudGlhbCJdLCJjcmVkZW50aWFsU3ViamVjdCI6eyJuYW1lIjpbeyJuYW1lUGFydHMiOlt7InR5cGUiOiJHaXZlbk5hbWUiLCJ2YWx1ZSI6Iktlbm5ldGgifSx7InR5cGUiOiJGYW1pbHlOYW1lIiwidmFsdWUiOiJEZWNlcnF1ZWlyYSJ9XX1dLCJiaXJ0aERhdGUiOlt7InZhbHVlIjoiMTk2NS0wNy0wOCJ9XSwiYWRkcmVzcyI6W3siYWRkcmVzc0NvdW50cnkiOiJHQiIsImJ1aWxkaW5nTmFtZSI6IiIsInN0cmVldE5hbWUiOiJIQURMRVkgUk9BRCIsInBvc3RhbENvZGUiOiJCQTIgNUFBIiwiYnVpbGRpbmdOdW1iZXIiOiI4IiwiYWRkcmVzc0xvY2FsaXR5IjoiQkFUSCIsInZhbGlkRnJvbSI6IjIwMDAtMDEtMDEifV19fSwianRpIjoidXJuOnV1aWQ6NmZhNTViZTAtODAwNC00YzdhLThiZWEtOGM2ODgwNmJjMWNjIn0.kEugKcCb1KNU-rDjaJ6jDcsPWtSHPbsM7PXm7N2o1OGT506-lFj23qEVxRQac-BSHKcVCk1FTKcE8FJwghRUEA", // pragma: allowlist secret
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ1cm46dXVpZDo1ODg5MTg3NS02OWIzLTRjYzUtYWU3OS1hOTMxMzI0NTk3NDIiLCJhdWQiOiJodHRwczpcL1wvaWRlbnRpdHkuYnVpbGQuYWNjb3VudC5nb3YudWsiLCJuYmYiOjE2OTgwNzYwMzMsImlzcyI6Imh0dHBzOlwvXC9kY21hdy1jcmkuc3R1YnMuYWNjb3VudC5nb3YudWsiLCJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiSWRlbnRpdHlDaGVja0NyZWRlbnRpYWwiXSwiY3JlZGVudGlhbFN1YmplY3QiOnsibmFtZSI6W3sibmFtZVBhcnRzIjpbeyJ0eXBlIjoiR2l2ZW5OYW1lIiwidmFsdWUiOiJLZW5uZXRoIn0seyJ0eXBlIjoiRmFtaWx5TmFtZSIsInZhbHVlIjoiRGVjZXJxdWVpcmEifV19XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5NjUtMDctMDgifV0sInBhc3Nwb3J0IjpbeyJleHBpcnlEYXRlIjoiMjAzMC0wMS0wMSIsImRvY3VtZW50TnVtYmVyIjoiMzIxNjU0OTg3In1dfSwiZXZpZGVuY2UiOlt7ImFjdGl2aXR5SGlzdG9yeVNjb3JlIjoxLCJjaGVja0RldGFpbHMiOlt7ImNoZWNrTWV0aG9kIjoidnJpIn0seyJiaW9tZXRyaWNWZXJpZmljYXRpb25Qcm9jZXNzTGV2ZWwiOjMsImNoZWNrTWV0aG9kIjoiYnZyIn1dLCJ2YWxpZGl0eVNjb3JlIjoyLCJzdHJlbmd0aFNjb3JlIjozLCJ0eXBlIjoiSWRlbnRpdHlDaGVjayIsInR4biI6IjMxMTUwM2IzLTA4MzEtNGY1OS1hOTQyLWEzNmJlOWI2MTlhNCJ9XX0sImp0aSI6InVybjp1dWlkOjRhZDY0OTAxLTE3MzUtNGIxZC1iYzhjLTAzODA4ZWQxMjgxNyJ9.VDnGLxpY_s6uXw1kDVSWRoKLoEAjfDv1iYZ1uC7YTk1uojPxhtn9RCfJRBFzAtTtEy1VwwcnOCBqkm9lQBJqtw", // pragma: allowlist secret
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ1cm46dXVpZDo1ODg5MTg3NS02OWIzLTRjYzUtYWU3OS1hOTMxMzI0NTk3NDIiLCJhdWQiOiJodHRwczpcL1wvaWRlbnRpdHkuYnVpbGQuYWNjb3VudC5nb3YudWsiLCJuYmYiOjE2OTgwNzYwNTMsImlzcyI6Imh0dHBzOlwvXC9mcmF1ZC1jcmkuc3R1YnMuYWNjb3VudC5nb3YudWsiLCJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiSWRlbnRpdHlDaGVja0NyZWRlbnRpYWwiXSwiY3JlZGVudGlhbFN1YmplY3QiOnsibmFtZSI6W3sibmFtZVBhcnRzIjpbeyJ0eXBlIjoiR2l2ZW5OYW1lIiwidmFsdWUiOiJLZW5uZXRoIn0seyJ0eXBlIjoiRmFtaWx5TmFtZSIsInZhbHVlIjoiRGVjZXJxdWVpcmEifV19XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5NjUtMDctMDgifV0sImFkZHJlc3MiOlt7ImFkZHJlc3NDb3VudHJ5IjoiR0IiLCJidWlsZGluZ05hbWUiOiIiLCJzdHJlZXROYW1lIjoiSEFETEVZIFJPQUQiLCJwb3N0YWxDb2RlIjoiQkEyIDVBQSIsImJ1aWxkaW5nTnVtYmVyIjoiOCIsImFkZHJlc3NMb2NhbGl0eSI6IkJBVEgiLCJ2YWxpZEZyb20iOiIyMDAwLTAxLTAxIn1dfSwiZXZpZGVuY2UiOlt7ImlkZW50aXR5RnJhdWRTY29yZSI6MiwidHlwZSI6IklkZW50aXR5Q2hlY2siLCJ0eG4iOiJjMzYzNTQxNC03YjY2LTRjN2EtODJlNC1lZDgzNmVlNmVmZjEifV19LCJqdGkiOiJ1cm46dXVpZDo1MWRiMWQ2Ni1iNmRkLTQ0NWQtODdlNi00MzQwYjAyZGUyMWIifQ.6bL6VAKqVgEKwwg6YSISLln6in94GYcvmyoPQbK3jahobK9nxg4tCL5PRdq9i_XUPpQrnvX3I60m41U5-FlA5Q", // pragma: allowlist secret
      ],
    },
  );

  // Act
  const response = (await postMitigationsHandler(
    request,
  )) as APIGatewayProxyStructuredResultV2;

  // Assert
  expect(mockCompletePendingMitigations).toHaveBeenCalledWith(
    "urn:uuid:6fa55be0-8004-4c7a-8bea-8c68806bc1cc",
    "urn:uuid:58891875-69b3-4cc5-ae79-a93132459742",
  );
  expect(mockCompletePendingMitigations).toHaveBeenCalledWith(
    "urn:uuid:4ad64901-1735-4b1d-bc8c-03808ed12817",
    "urn:uuid:58891875-69b3-4cc5-ae79-a93132459742",
  );
  expect(mockCompletePendingMitigations).toHaveBeenCalledWith(
    "urn:uuid:51db1d66-b6dd-445d-87e6-4340b02de21b",
    "urn:uuid:58891875-69b3-4cc5-ae79-a93132459742",
  );

  const parsedResponse = JSON.parse(response.body!) as PostMitigationsResponse;
  expect(parsedResponse.result).toBe(SUCCESS_RESPONSE);
  expect(response.statusCode).toBe(200);
});
