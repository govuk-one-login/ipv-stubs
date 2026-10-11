import {
  APIGatewayProxyEvent,
  APIGatewayProxyStructuredResultV2,
} from "aws-lambda";
import { createHandler } from "../../src/handlers/createHandler";
import { processPostUserVCsRequestV2 } from "../../src/services/evcsService";
import { VcState, VCProvenance } from "../../src/domain/enums";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { PostVcsRequest } from "../../src/domain/requests/postVcsRequest";

vi.mock("../../src/services/evcsService", () => ({
  processPostUserVCsRequestV2: vi.fn(),
}));

const TEST_POST_REQUEST: PostVcsRequest = {
  userId: "testUserId",
  govuk_signin_journey_id: "testJourneyId",
  vcs: [
    {
      vc: "zzJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.zf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ", // pragma: allowlist secret
      state: VcState.CURRENT,
      provenance: VCProvenance.MIGRATED,
    },
  ],
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe("create handler V2", () => {
  it("should return 202 for a valid request", async () => {
    // arrange
    vi.mocked(processPostUserVCsRequestV2).mockResolvedValueOnce({
      statusCode: 202,
      response: {},
    });
    const event = {
      body: JSON.stringify(TEST_POST_REQUEST),
    } as APIGatewayProxyEvent;

    // act
    const response = (await createHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(response.statusCode).toBe(202);
    expect(processPostUserVCsRequestV2).toHaveBeenCalledWith(TEST_POST_REQUEST);
  });

  it("should return 400 for a request with no body", async () => {
    // arrange
    const event = {
      body: null,
    } as APIGatewayProxyEvent;

    // act
    const response = (await createHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(response.statusCode).toBe(400);
    expect(processPostUserVCsRequestV2).not.toHaveBeenCalled();
  });

  it("should return 400 for an invalid request object", async () => {
    // arrange
    const request = { not: "a valid request" };
    const event = {
      body: JSON.stringify(request),
    } as APIGatewayProxyEvent;

    // act
    const response = (await createHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(response.statusCode).toBe(400);
    expect(processPostUserVCsRequestV2).not.toHaveBeenCalled();
  });

  it("should return 400 for a request with no user id", async () => {
    // arrange
    const request = structuredClone(TEST_POST_REQUEST);
    request.userId = "";
    const event = {
      body: JSON.stringify(request),
    } as APIGatewayProxyEvent;

    // act
    const response = (await createHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(response.statusCode).toBe(400);
    expect(processPostUserVCsRequestV2).not.toHaveBeenCalled();
  });

  it("should return 400 for a request with no VCs", async () => {
    // arrange
    const request = structuredClone(TEST_POST_REQUEST);
    request.vcs = [];
    const event = {
      body: JSON.stringify(request),
    } as APIGatewayProxyEvent;

    // act
    const response = (await createHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(response.statusCode).toBe(400);
    expect(processPostUserVCsRequestV2).not.toHaveBeenCalled();
  });

  it("should return 400 for a request with duplicate VCs in different states", async () => {
    // arrange
    const request = structuredClone(TEST_POST_REQUEST);
    const duplicateVc = structuredClone(TEST_POST_REQUEST.vcs[0]);
    request.vcs.push(duplicateVc);
    request.vcs[0].state = VcState.CURRENT;
    request.vcs[1].state = VcState.PENDING;
    const event = {
      body: JSON.stringify(request),
    } as APIGatewayProxyEvent;

    // act
    const response = (await createHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(response.statusCode).toBe(400);
    expect(processPostUserVCsRequestV2).not.toHaveBeenCalled();
  });
});
