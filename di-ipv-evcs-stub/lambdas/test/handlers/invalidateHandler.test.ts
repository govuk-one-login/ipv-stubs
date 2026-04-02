import {
  APIGatewayProxyEvent,
  APIGatewayProxyStructuredResultV2,
} from "aws-lambda";
import { invalidateStoredIdentityHandler } from "../../src/handlers/invalidateHandler";
import { invalidateUserSi } from "../../src/services/evcsService";
import { InvalidateIdentityRequest } from "../../src/domain/requests";
import { describe, it, expect, vi, beforeEach } from "vitest";

vi.mock("../../src/common/ssmParameter");

vi.mock("../../src/services/evcsService", () => ({
  processGetUserVCsRequest: vi.fn(),
  processPatchUserVCsRequest: vi.fn(),
  processPostUserVCsRequest: vi.fn(),
  processGetIdentityRequest: vi.fn(),
  processPostIdentityRequest: vi.fn(),
  invalidateUserSi: vi.fn(),
}));

const TEST_USER_ID: string = "urn:uuid:d1823066-2137-4380-b0ba-4b61947e08e6";

type RecursivePartial<T> = {
  [P in keyof T]?: RecursivePartial<T[P]>;
};

const createEvent = <T>(
  requestBody: RecursivePartial<T>,
): APIGatewayProxyEvent =>
  ({
    body: JSON.stringify(requestBody),
  }) as APIGatewayProxyEvent;

describe("invalidate stored identity handler", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should return status code from EVCS Service for valid request", async () => {
    // Arrange
    vi.mocked(invalidateUserSi).mockResolvedValue({
      statusCode: 204,
    });
    const event = createEvent<InvalidateIdentityRequest>({
      userId: TEST_USER_ID,
    });

    // Act
    const res = (await invalidateStoredIdentityHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // Assert
    expect(res.statusCode).toEqual(204);
  });

  it("should return 400 if missing userId", async () => {
    // Arrange
    const event = createEvent<InvalidateIdentityRequest>({});

    // Act
    const res = (await invalidateStoredIdentityHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // Assert
    expect(res.statusCode).toEqual(400);
  });

  it("should return 400 if missing request body", async () => {
    // Arrange
    const event = {} as APIGatewayProxyEvent;

    // Act
    const res = (await invalidateStoredIdentityHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // Assert
    expect(res.statusCode).toEqual(400);
  });
});
