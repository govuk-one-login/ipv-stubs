import { jwtVerify, JWTVerifyResult, ResolvedKey, KeyLike } from "jose";
import { getParameter } from "@aws-lambda-powertools/parameters/ssm";
import { getUserIdFromBearerToken } from "../../src/utils/tokenVerifier";
import {
  InvalidAccessToken,
  InvalidAuthHeader,
} from "../../src/domain/exceptions";

jest.mock("jose", () => ({
  jwtVerify: jest.fn(),
  importSPKI: jest.fn(),
}));

jest.mock("@aws-lambda-powertools/parameters/ssm", () => ({
  getParameter: jest.fn(),
}));

const TEST_USER_ID = "userId";
const TEST_AUTH_HEADER = "Bearer someToken";

describe("getUserIdFromBearerToken", () => {
  it("should return valid userId if given valid token", async () => {
    // Arrange
    const jwtPayload = {
      payload: {
        sub: TEST_USER_ID,
      },
    } as JWTVerifyResult<unknown> & ResolvedKey<KeyLike>;
    jest.mocked(getParameter).mockResolvedValue("some-key");
    jest.mocked(jwtVerify).mockResolvedValue(jwtPayload);

    // Act
    const res = await getUserIdFromBearerToken(TEST_AUTH_HEADER);

    // Assert
    expect(res).toBe(TEST_USER_ID);
  });

  it("should return undefined if no user id present", async () => {
    // Arrange
    const jwtPayload = {
      payload: {},
    } as JWTVerifyResult<unknown> & ResolvedKey<KeyLike>;
    jest.mocked(getParameter).mockResolvedValue("some-key");
    jest.mocked(jwtVerify).mockResolvedValue(jwtPayload);

    // Act
    const res = await getUserIdFromBearerToken(TEST_AUTH_HEADER);

    // Assert
    expect(res).toBeUndefined();
  });

  it("should throw is token verification fails", async () => {
    // Arrange
    jest.mocked(getParameter).mockResolvedValue("some-key");
    jest
      .mocked(jwtVerify)
      .mockRejectedValue(new Error("Failed to verify token"));

    // Act/Assert
    await expect(getUserIdFromBearerToken(TEST_AUTH_HEADER)).rejects.toThrow(
      new InvalidAccessToken("Failed to verify bearer token"),
    );
  });

  it.each([
    {
      case: "invalid format",
      authHeader: "Bearer with extra bits",
      expectedException: new InvalidAuthHeader("Invalid auth header format"),
    },
    {
      case: "empty string",
      authHeader: "",
      expectedException: new InvalidAuthHeader("Invalid auth header format"),
    },
    {
      case: "invalid auth type",
      authHeader: "Basic token",
      expectedException: new InvalidAuthHeader(
        "Invalid auth header - must be Bearer type",
      ),
    },
  ])(
    "should throw if given auth header with $case",
    async ({ authHeader, expectedException }) => {
      // Act/Assert
      await expect(getUserIdFromBearerToken(authHeader)).rejects.toThrow(
        expectedException,
      );
    },
  );
});
