import {
  APIGatewayProxyEvent,
  APIGatewayProxyStructuredResultV2,
} from "aws-lambda";
import { updateHandler } from "../../src/handlers/updateHandler";
import { processPatchUserVCsRequestV2 } from "../../src/services/evcsService";
import { VcState } from "../../src/domain/enums";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { PatchVcsRequest } from "../../src/domain/requests/patchVcsRequest";

vi.mock("../../src/services/evcsService", () => ({
  processPatchUserVCsRequestV2: vi.fn(),
}));

const TEST_PATCH_REQUEST: PatchVcsRequest = {
  userId: "testUserId",
  govuk_signin_journey_id: "testJourneyId",
  vcs: [
    {
      signature:
        "zf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ", // pragma: allowlist secret
      state: VcState.CURRENT,
      metadata: { prop1: "value1" },
    },
  ],
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe("create handler V2", () => {
  it("should return 202 for a valid request", async () => {
    // arrange
    vi.mocked(processPatchUserVCsRequestV2).mockResolvedValueOnce({
      statusCode: 202,
      response: {},
    });
    const event = {
      body: JSON.stringify(TEST_PATCH_REQUEST),
    } as APIGatewayProxyEvent;

    // act
    const response = (await updateHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(response.statusCode).toBe(202);
    expect(processPatchUserVCsRequestV2).toHaveBeenCalledWith(
      TEST_PATCH_REQUEST,
    );
  });

  it("should return 400 for a request with no body", async () => {
    // arrange
    const event = {
      body: null,
    } as APIGatewayProxyEvent;

    // act
    const response = (await updateHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(response.statusCode).toBe(400);
    expect(processPatchUserVCsRequestV2).not.toHaveBeenCalled();
  });

  it("should return 400 for an invalid request object", async () => {
    // arrange
    const request = { not: "a valid request" };
    const event = {
      body: JSON.stringify(request),
    } as APIGatewayProxyEvent;

    // act
    const response = (await updateHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(response.statusCode).toBe(400);
    expect(processPatchUserVCsRequestV2).not.toHaveBeenCalled();
  });

  it("should return 400 for a request with no user id", async () => {
    // arrange
    const request = structuredClone(TEST_PATCH_REQUEST);
    request.userId = "";
    const event = {
      body: JSON.stringify(request),
    } as APIGatewayProxyEvent;

    // act
    const response = (await updateHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(response.statusCode).toBe(400);
    expect(processPatchUserVCsRequestV2).not.toHaveBeenCalled();
  });

  it("should return 400 for a request with no VCs", async () => {
    // arrange
    const request = structuredClone(TEST_PATCH_REQUEST);
    request.vcs = [];
    const event = {
      body: JSON.stringify(request),
    } as APIGatewayProxyEvent;

    // act
    const response = (await updateHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(response.statusCode).toBe(400);
    expect(processPatchUserVCsRequestV2).not.toHaveBeenCalled();
  });

  // it("should return 400 for a request with invalid VC creation state", async () => {
  //     // arrange
  //     const request = structuredClone(TEST_PATCH_REQUEST_V2);
  //     request.vcs[0].state = VcState.HISTORIC;
  //     const event = {
  //         body: JSON.stringify(request),
  //     } as APIGatewayProxyEvent;
  //
  //     // act
  //     const response = (await updateHandler(event)) as APIGatewayProxyStructuredResultV2;
  //
  //     // assert
  //     expect(response.statusCode).toBe(400);
  //     expect(processPostUserVCsRequestV2).not.toHaveBeenCalled();
  // });

  it("should return 400 for a request with duplicate VCs in different states", async () => {
    // arrange
    const request = structuredClone(TEST_PATCH_REQUEST);
    const duplicateVc = structuredClone(TEST_PATCH_REQUEST.vcs[0]);
    request.vcs.push(duplicateVc);
    request.vcs[0].state = VcState.CURRENT;
    request.vcs[1].state = VcState.PENDING;
    const event = {
      body: JSON.stringify(request),
    } as APIGatewayProxyEvent;

    // act
    const response = (await updateHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(response.statusCode).toBe(400);
    expect(processPatchUserVCsRequestV2).not.toHaveBeenCalled();
  });
});
