import { getUserIdentity } from "../../src/services/sisService";
import { postUserIdentityHandler } from "../../src/handlers/sisHandler";
import {
  APIGatewayProxyEvent,
  APIGatewayProxyStructuredResultV2,
} from "aws-lambda";
import { getUserIdFromBearerToken } from "../../src/utils/tokenVerifier";
import {
  buildBadRequestResponse,
  buildForbiddenResponse,
  buildNotFoundResponse,
  buildServerErrorResponse,
  buildUnauthorisedResponse,
} from "../../src/domain/errorResponse";
import {
  InvalidAccessToken,
  InvalidAuthHeader,
} from "../../src/domain/exceptions";
import { UserIdentity } from "../../src/domain/userIdentity";

jest.mock("../../src/services/sisService", () => ({
  getUserIdentity: jest.fn(),
}));

jest.mock("../../src/utils/tokenVerifier", () => ({
  getUserIdFromBearerToken: jest.fn(),
}));

const TEST_USER_ID = "userId";
const TEST_SI_JWT =
  "eyJraWQiOiJ0ZXN0LXNpZ25pbmcta2V5IiwidHlwIjoiSldUIiwiYWxnIjoiRVMyNTYifQ.eyJhdWQiOiJodHRwczovL3JldXNlLWlkZW50aXR5LmJ1aWxkLmFjY291bnQuZ292LnVrIiwic3ViIjoiZWFlMDFhYzI5MGE5ODRkMGVhN2MzM2NjNDVlMzZmMTIiLCJuYmYiOjE3NTA2ODIwMTgsImNyZWRlbnRpYWxzIjpbIk43UHhoZmtGa215VFFGS3lBWE15U19INk51Ri13RHpFa3RiX2RWdXJ1bFNSTU1YaG54aGJSMnJ4czlUYy1LUUIwaVhiMV85YUJJOFhDeTJBYkdRdkZRIiwiUzROSlBjaWltYmZ4MDhqczltOThoc3JLTDRiSkh0QlF5S0d0cmRJeklmWW1CUGpyVTlwYXpfdV8xaENySFo4aWp5UW81UlBtUWxNUC1fYzVldXZaSHciLCJBOU9IdUtJOE41aDRDNDU3UTRxdE52a1NGS2ZGZVZNNHNFR3dxUlBjU0hpUXlsemh4UnlxMDBlMURVUUxtU2RpZTlYSWswQ2ZpUVNBX3I3LW1tQ2JBdyIsInk0NHYwcEVBODh6dURoREZEQ0RjUGduOTZwOWJTRm9qeHZQQTFCeEdYTnhEMG5QelFONk1SaG1PWXBTUXg4TW92XzNLWUF4bmZ5aXdSemVBclhKa3FBIl0sImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkubG9jYWwuYWNjb3VudC5nb3YudWsiLCJjbGFpbXMiOnsiaHR0cHM6Ly92b2NhYi5hY2NvdW50Lmdvdi51ay92MS9jb3JlSWRlbnRpdHkiOnsibmFtZSI6W3sibmFtZVBhcnRzIjpbeyJ0eXBlIjoiR2l2ZW5OYW1lIiwidmFsdWUiOiJLRU5ORVRIIn0seyJ0eXBlIjoiRmFtaWx5TmFtZSIsInZhbHVlIjoiREVDRVJRVUVJUkEifV19XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5NjUtMDctMDgifV19LCJodHRwczovL3ZvY2FiLmFjY291bnQuZ292LnVrL3YxL2FkZHJlc3MiOlt7ImFkZHJlc3NDb3VudHJ5IjoiR0IiLCJhZGRyZXNzTG9jYWxpdHkiOiJCQVRIIiwiYnVpbGRpbmdOYW1lIjoiIiwiYnVpbGRpbmdOdW1iZXIiOiI4IiwicG9zdGFsQ29kZSI6IkJBMiA1QUEiLCJzdHJlZXROYW1lIjoiSEFETEVZIFJPQUQiLCJzdWJCdWlsZGluZ05hbWUiOiIiLCJ1cHJuIjoxMDAxMjAwMTIwNzcsInZhbGlkRnJvbSI6IjEwMDAtMDEtMDEifV0sImh0dHBzOi8vdm9jYWIuYWNjb3VudC5nb3YudWsvdjEvcGFzc3BvcnQiOlt7ImRvY3VtZW50TnVtYmVyIjoiMzIxNjU0OTg3IiwiZXhwaXJ5RGF0ZSI6IjIwMzAtMDEtMDEiLCJpY2FvSXNzdWVyQ29kZSI6IkdCUiJ9XX0sInZvdCI6IlAyIiwiaWF0IjoxNzUwNjgyMDE4fQ.nrbiwaOcvWM92TTAlORzerjjrrCuYD9fcxwEoXbf71J3YZUnwNW0KGUN5jaEvOysG0YWTXSLl_W4sN-Krf7PfQ"; // pragma: allowlist secret

const TEST_GET_EVENT = {
  headers: {
    Authorization: `Bearer someToken`,
  },
  body: JSON.stringify({
    vtr: ["P2"],
    govukSigninJourneyId: "journeyId",
  }),
} as unknown as APIGatewayProxyEvent;

describe("getUserIdentityHandler", () => {
  it("should return 200 with valid response object given a valid request", async () => {
    // Arrange
    const expectedUserIdentity: UserIdentity = {
      vot: "P2",
      content: TEST_SI_JWT,
      isValid: true,
      expired: false,
      kidValid: true,
      signatureValid: true,
    };
    jest.mocked(getUserIdentity).mockResolvedValue(expectedUserIdentity);
    jest.mocked(getUserIdFromBearerToken).mockResolvedValueOnce(TEST_USER_ID);

    // Act
    const res = (await postUserIdentityHandler(
      TEST_GET_EVENT,
    )) as APIGatewayProxyStructuredResultV2;

    // Assert
    expect(res.statusCode).toBe(200);
    expect(res.body).toBe(JSON.stringify(expectedUserIdentity));
  });

  it("should return 200 if getUserIdentity returns malformed JWT", async () => {
    // Arrange
    const expectedUserIdentity = {
      isValid: true,
      expired: false,
    };
    jest
      .mocked(getUserIdentity)
      .mockResolvedValue(expectedUserIdentity as UserIdentity);
    jest.mocked(getUserIdFromBearerToken).mockResolvedValueOnce(TEST_USER_ID);

    // Act
    const res = (await postUserIdentityHandler(
      TEST_GET_EVENT,
    )) as APIGatewayProxyStructuredResultV2;

    // Assert
    expect(res.statusCode).toBe(200);
    expect(res.body).toBe(JSON.stringify(expectedUserIdentity));
  });

  it("should return 404 if no SI record is found for user", async () => {
    // Arrange
    jest.mocked(getUserIdentity).mockResolvedValue(null);
    jest.mocked(getUserIdFromBearerToken).mockResolvedValueOnce(TEST_USER_ID);

    // Act
    const res = (await postUserIdentityHandler(
      TEST_GET_EVENT,
    )) as APIGatewayProxyStructuredResultV2;

    // Assert
    expect(res.statusCode).toBe(404);
    expect(res.body).toBe(JSON.stringify(buildNotFoundResponse()));
  });

  it.each([
    {
      case: "missing vtr in request body",
      testBody: { govukSigninJourneyId: "journeyId" },
      expectErrorMessage: "Missing vtr in request body",
    },
    {
      case: "empty vtr list in request body",
      testBody: { govukSigninJourneyId: "journeyId", vtr: [] },
      expectErrorMessage: "Missing vtr in request body",
    },
    {
      case: "missing govukSigninJourneyId in request body",
      testBody: { vtr: ["P2"] },
      expectErrorMessage: "Missing govukSigninJourneyId in request body",
    },
    {
      case: "missing  request body",
      testBody: undefined,
      expectErrorMessage: "Missing request body",
    },
  ])(
    "should return 400 if given $case",
    async ({ testBody, expectErrorMessage }) => {
      // Arrange
      jest.mocked(getUserIdFromBearerToken).mockResolvedValueOnce("userId");

      // Act
      const res = (await postUserIdentityHandler({
        ...TEST_GET_EVENT,
        body: testBody ? JSON.stringify(testBody) : null,
      })) as APIGatewayProxyStructuredResultV2;

      // Assert
      expect(res.statusCode).toBe(400);
      expect(res.body).toBe(
        JSON.stringify(buildBadRequestResponse(expectErrorMessage)),
      );
    },
  );

  it("should return 400 if no userId found", async () => {
    // Arrange
    jest.mocked(getUserIdFromBearerToken).mockResolvedValueOnce("");

    // Act
    const res = (await postUserIdentityHandler(
      TEST_GET_EVENT,
    )) as APIGatewayProxyStructuredResultV2;

    // Assert
    expect(res.statusCode).toBe(400);
    expect(res.body).toBe(
      JSON.stringify(buildBadRequestResponse("Missing user id")),
    );
  });

  it("should return 401 if missing auth header", async () => {
    // Arrange

    // Act
    const res = (await postUserIdentityHandler({
      headers: {},
    } as unknown as APIGatewayProxyEvent)) as APIGatewayProxyStructuredResultV2;

    // Assert
    expect(res.statusCode).toBe(401);
    expect(res.body).toBe(JSON.stringify(buildUnauthorisedResponse()));
  });

  it("should return 401 if getUserIdFromBearerToken throws InvalidAuthHeader", async () => {
    // Arrange
    jest
      .mocked(getUserIdFromBearerToken)
      .mockRejectedValue(new InvalidAuthHeader("Missing Authorization header"));

    // Act
    const res = (await postUserIdentityHandler(
      TEST_GET_EVENT,
    )) as APIGatewayProxyStructuredResultV2;

    // Assert
    expect(res.statusCode).toBe(401);
    expect(res.body).toBe(JSON.stringify(buildUnauthorisedResponse()));
  });

  it("should return 403 if getUserIdFromBearerToken throws InvalidAccessToken", async () => {
    // Arrange
    jest
      .mocked(getUserIdFromBearerToken)
      .mockRejectedValue(
        new InvalidAccessToken("Failed to verify bearer token"),
      );

    // Act
    const res = (await postUserIdentityHandler(
      TEST_GET_EVENT,
    )) as APIGatewayProxyStructuredResultV2;

    // Assert
    expect(res.statusCode).toBe(403);
    expect(res.body).toBe(JSON.stringify(buildForbiddenResponse()));
  });

  it("should return 500 if unknown error is thrown", async () => {
    // Arrange
    jest
      .mocked(getUserIdFromBearerToken)
      .mockRejectedValue(new Error("Failed to get parameter from SSM"));

    // Act
    const res = (await postUserIdentityHandler(
      TEST_GET_EVENT,
    )) as APIGatewayProxyStructuredResultV2;

    // Assert
    expect(res.statusCode).toBe(500);
    expect(res.body).toBe(JSON.stringify(buildServerErrorResponse()));
  });
});
