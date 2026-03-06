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
import {
  StoredIdentityContents,
  UserIdentity,
} from "../../src/domain/userIdentity";

jest.mock("../../src/services/sisService", () => ({
  getUserIdentity: jest.fn(),
}));

jest.mock("../../src/utils/tokenVerifier", () => ({
  getUserIdFromBearerToken: jest.fn(),
}));

const TEST_USER_ID = "userId";
const TEST_SI_CONTENT: StoredIdentityContents = {
  sub: "some-sub",
  vot: "P2",
  vtm: "some-vtm",
  credentials: ["vc-sig1", "vc-sig2"],
  "https://vocab.account.gov.uk/v1/credentialJWT": ["vc1", "vc2"],
};
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
      content: TEST_SI_CONTENT,
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

  it("should return 404 if no SI record is found for user or if si record if malformed", async () => {
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

  it("should return 401 if missing header", async () => {
    // Arrange

    // Act
    const res = (await postUserIdentityHandler({
      headers: undefined,
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
