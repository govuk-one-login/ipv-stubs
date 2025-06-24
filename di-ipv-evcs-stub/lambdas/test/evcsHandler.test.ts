import {
  APIGatewayProxyEvent,
  APIGatewayProxyEventHeaders,
  APIGatewayProxyEventPathParameters,
  APIGatewayProxyStructuredResultV2,
} from "aws-lambda";
import {
  createHandler,
  getHandler,
  postIdentityHandler,
  updateHandler,
} from "../src/handlers/evcsHandler";
import {
  processGetUserVCsRequest,
  processPatchUserVCsRequest,
  processPostUserVCsRequest,
  processPostIdentityRequest,
} from "../src/services/evcsService";
import { VcState, VCProvenance } from "../src/domain/enums";
import { getParameter } from "@aws-lambda-powertools/parameters/ssm";
import { APIGatewayProxyEventQueryStringParameters } from "aws-lambda/trigger/api-gateway-proxy";
import { PostIdentityRequest } from "../src/domain/requests";
import { Vot } from "../src/domain/enums/vot";

jest.mock("../src/services/evcsService", () => ({
  processGetUserVCsRequest: jest.fn(),
  processPatchUserVCsRequest: jest.fn(),
  processPostUserVCsRequest: jest.fn(),
  processPostIdentityRequest: jest.fn(),
}));

jest.mock("@aws-lambda-powertools/parameters/ssm", () => ({
  getParameter: jest.fn(),
}));

const EVCS_VERIFY_KEY =
  "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEE9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIMqVMxm2EdlSRjPkCV5NDyN9/RMmJLerY4H0vkXDjEDTg=="; // pragma: allowlist secret

const TEST_USER_ID: string = "urn:uuid:d1823066-2137-4380-b0ba-4b61947e08e6";
const TEST_VC_STRING =
  "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ"; // pragma: allowlist secret
const TEST_METADATA = {
  reason: "test-created",
  timestampMs: "1714478033959",
  txmaEventId: "txma-event-id",
  testProperty: "testProperty",
};

const TEST_POST_REQUEST = [
  {
    vc: "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ", // pragma: allowlist secret
    state: VcState.CURRENT,
    metadata: {
      reason: "test-created",
      timestampMs: "1714478033959",
      txmaEventId: "txma-event-id",
      testProperty: "testProperty",
    },
  },
  {
    vc: "zzJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.zf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ", // pragma: allowlist secret
    state: VcState.CURRENT,
    provenance: VCProvenance.MIGRATED,
  },
  {
    vc: "yyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.tf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ", // pragma: allowlist secret
    state: VcState.VERIFICATION,
  },
  {
    vc: "ddJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.df0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ", // pragma: allowlist secret
    state: VcState.PENDING_RETURN,
  },
];
const TEST_POST_INVALID_STATE_REQUEST = [
  {
    vc: "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ", // pragma: allowlist secret
    state: VcState.HISTORIC,
  },
];

const TEST_PATCH_REQUEST = [
  {
    signature:
      "qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ", // pragma: allowlist secret
    state: VcState.CURRENT,
    metadata: {
      reason: "updated",
      timestampMs: "1714478033959",
      txmaEventId: "txma-event-id",
    },
  },
  {
    signature:
      "tf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ", // pragma: allowlist secret
    state: VcState.ABANDONED,
  },
];

const TEST_PATCH_INVALID_STATE_REQUEST = [
  {
    signature:
      "qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ", // pragma: allowlist secret
    state: VcState.VERIFICATION,
  },
];

type RecursivePartial<T> = {
  [P in keyof T]?: RecursivePartial<T[P]>;
};

const buildPostIdentityRequest = (
  postIdentityRequest?: RecursivePartial<PostIdentityRequest>,
) => {
  return {
    userId: TEST_USER_ID,
    si: {
      jwt: TEST_VC_STRING,
      vot: Vot.P2,
      metadata: TEST_METADATA,
    },
    ...(postIdentityRequest ? postIdentityRequest : {}),
  };
};

const TEST_PATH_PARAM = {
  userId: TEST_USER_ID,
} as APIGatewayProxyEventPathParameters;

const TEST_HEADERS = {
  Authorization: `Bearer eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2V2Y3MuYnVpbGQuc3R1YnMuYWNjb3VudC5nb3YudWsiLCJzdWIiOiJ1cm46dXVpZDpkMTgyMzA2Ni0yMTM3LTQzODAtYjBiYS00YjYxOTQ3ZTA4ZTYiLCJhdWQiOiJodHRwczovL2V2Y3MuYnVpbGQuc3R1YnMuYWNjb3VudC5nb3YudWsiLCJqdGkiOiJ1cm46dXVpZDpiNmRkMjNkMy1mZjM3LTQzYzYtOTI3My01NTRkNjQzMjFiODMiLCJuYmYiOjE3MTUxNjU0NjksImlhdCI6MTcxMjU3MzQ2OX0.1-nRkV6ny9ThBGDbQ1sDCrJpYSe0tbOXEMJJNEoomVWjKsRL1RK6qdATkk-54p_c68Gzu1mN4FDM-buk1gXIPQ`, // pragma: allowlist secret
} as APIGatewayProxyEventHeaders;

const TEST_POST_EVENT = {
  body: JSON.stringify(TEST_POST_REQUEST),
  pathParameters: TEST_PATH_PARAM,
} as APIGatewayProxyEvent;

const TEST_PATCH_EVENT = {
  body: JSON.stringify(TEST_PATCH_REQUEST),
  pathParameters: TEST_PATH_PARAM,
} as APIGatewayProxyEvent;

const TEST_GET_EVENT = {
  pathParameters: TEST_PATH_PARAM,
  headers: TEST_HEADERS,
} as APIGatewayProxyEvent;

const createPostIdentityEvent = (
  requestBody: RecursivePartial<PostIdentityRequest>,
) =>
  ({
    body: JSON.stringify(requestBody),
  }) as APIGatewayProxyEvent;

describe("evcs handlers", () => {
  describe("create handler", () => {
    it("should return 202 for a valid request", async () => {
      // arrange
      jest.mocked(processPostUserVCsRequest).mockResolvedValueOnce({
        statusCode: 202,
        response: {},
      });

      // act
      const response = (await createHandler(
        TEST_POST_EVENT,
      )) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(response.statusCode).toBe(202);
      expect(processPostUserVCsRequest).toHaveBeenCalledWith(
        TEST_USER_ID,
        TEST_POST_REQUEST,
      );
    });

    it("should return 400 for a request with no user id", async () => {
      // act
      const response = (await createHandler({
        ...TEST_POST_EVENT,
        pathParameters: null,
      })) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(response.statusCode).toBe(400);
      expect(processPostUserVCsRequest).not.toHaveBeenCalled();
    });

    it("should return 400 for a request with no body", async () => {
      // act
      const response = (await createHandler({
        ...TEST_POST_EVENT,
        body: null,
      })) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(response.statusCode).toBe(400);
      expect(processPostUserVCsRequest).not.toHaveBeenCalled();
    });

    it("should return 400 for a request with invalid VC creation state", async () => {
      // act
      const response = (await createHandler({
        ...TEST_POST_EVENT,
        body: JSON.stringify([TEST_POST_INVALID_STATE_REQUEST]),
      })) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(response.statusCode).toBe(400);
      expect(processPostUserVCsRequest).not.toHaveBeenCalled();
    });
  });

  describe("update handler", () => {
    it("should return 200 for a valid request", async () => {
      // arrange
      jest.mocked(processPatchUserVCsRequest).mockResolvedValueOnce({
        statusCode: 200,
        response: {},
      });

      // act
      const response = (await updateHandler(
        TEST_PATCH_EVENT,
      )) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(response.statusCode).toBe(200);
      expect(processPatchUserVCsRequest).toHaveBeenCalledWith(
        TEST_USER_ID,
        TEST_PATCH_REQUEST,
      );
    });

    it("should return 400 for a request with no user id", async () => {
      // act
      const response = (await updateHandler({
        ...TEST_PATCH_EVENT,
        pathParameters: null,
      })) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(response.statusCode).toBe(400);
      expect(processPatchUserVCsRequest).not.toHaveBeenCalled();
    });

    it("should return 400 for a request with no body", async () => {
      // act
      const response = (await updateHandler({
        ...TEST_PATCH_EVENT,
        body: null,
      })) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(response.statusCode).toBe(400);
      expect(processPatchUserVCsRequest).not.toHaveBeenCalled();
    });

    it("should return 400 for a request with invalid VC update state", async () => {
      // act
      const response = (await updateHandler({
        ...TEST_PATCH_EVENT,
        body: JSON.stringify([TEST_PATCH_INVALID_STATE_REQUEST]),
      })) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(response.statusCode).toBe(400);
      expect(processPatchUserVCsRequest).not.toHaveBeenCalled();
    });
  });

  describe("post identity handler", () => {
    it("should return 202 for a valid request", async () => {
      // arrange
      jest.mocked(processPostIdentityRequest).mockResolvedValue({
        statusCode: 202,
        response: {},
      });
      const testRequest = buildPostIdentityRequest();

      // act
      const response = (await postIdentityHandler(
        createPostIdentityEvent(testRequest),
      )) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(response.statusCode).toBe(202);
      expect(processPostIdentityRequest).toHaveBeenCalledWith(testRequest);
    });

    it("should return a 500 if saving to EVCS fails", async () => {
      // arrange
      jest.mocked(processPostIdentityRequest).mockResolvedValue({
        statusCode: 500,
        response: {},
      });
      const testRequest = buildPostIdentityRequest();

      // act
      const response = (await postIdentityHandler(
        createPostIdentityEvent(testRequest),
      )) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(response.statusCode).toBe(500);
      expect(processPostIdentityRequest).toHaveBeenCalledWith(testRequest);
    });

    it.each([
      {
        request: buildPostIdentityRequest({ userId: undefined }),
        case: "userId is missing",
      },
      {
        request: buildPostIdentityRequest({ si: undefined }),
        case: "si is missing",
      },
      {
        request: buildPostIdentityRequest({
          si: { vot: undefined, jwt: TEST_VC_STRING },
        }),
        case: "si.vot is missing",
      },
      {
        request: buildPostIdentityRequest({
          si: { jwt: undefined, vot: Vot.P2 },
        }),
        case: "si.jwt is missing",
      },
      // TODO PYIC-8458: These tests should be uncommented in phase 2 when the
      //  /identity endpoint accepts a vcs list as the request parsing should
      // handle these criteria
      // {
      //   request: buildPutRequest({ vcs: undefined }),
      //   case: "vcs is missing",
      // },
      // {
      //   request: buildPutRequest({ vcs: [] }),
      //   case: "vcs is empty",
      // },
      // {
      //   request: buildPutRequest({
      //     vcs: [
      //       { vc: "some.vc.sig", state: VcState.CURRENT },
      //       { vc: "some.vc.sig", state: VcState.HISTORIC },
      //     ],
      //   }),
      //   case: "duplicate vcs with different states",
      // },
    ])("should return a 400 if $case", async ({ request }) => {
      // act
      const response = (await postIdentityHandler(
        createPostIdentityEvent(request),
      )) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(response.statusCode).toBe(400);
      expect(processPostIdentityRequest).not.toHaveBeenCalled();
    });
  });

  describe("get handler", () => {
    it("should return 200 for a valid request with encoded state values", async () => {
      // arrange
      const testResult = { vcs: [] };
      jest.mocked(processGetUserVCsRequest).mockResolvedValueOnce({
        statusCode: 200,
        response: testResult,
      });
      jest.mocked(getParameter).mockResolvedValueOnce(EVCS_VERIFY_KEY);

      // act
      const response = (await getHandler({
        ...TEST_GET_EVENT,
        queryStringParameters: {
          state: "CURRENT%2CPENDING_RETURN",
        },
      })) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(response.statusCode).toBe(200);
      expect(response.body).toBe(JSON.stringify(testResult));
      expect(processGetUserVCsRequest).toHaveBeenCalledWith(TEST_USER_ID, [
        "CURRENT",
        "PENDING_RETURN",
      ]);
    });

    it("should return 200 for a valid request with not encoded state values", async () => {
      // arrange
      const testResult = { vcs: [] };
      jest.mocked(processGetUserVCsRequest).mockResolvedValueOnce({
        statusCode: 200,
        response: testResult,
      });
      jest.mocked(getParameter).mockResolvedValueOnce(EVCS_VERIFY_KEY);

      // act
      const response = (await getHandler({
        ...TEST_GET_EVENT,
        queryStringParameters: {
          state: "CURRENT,PENDING_RETURN",
        },
      })) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(response.statusCode).toBe(200);
      expect(response.body).toBe(JSON.stringify(testResult));
      expect(processGetUserVCsRequest).toHaveBeenCalledWith(TEST_USER_ID, [
        "CURRENT",
        "PENDING_RETURN",
      ]);
    });

    it("should return 200 for a valid request with all states", async () => {
      // arrange
      const testResult = { vcs: [] };
      jest.mocked(processGetUserVCsRequest).mockResolvedValueOnce({
        statusCode: 200,
        response: testResult,
      });
      jest.mocked(getParameter).mockResolvedValueOnce(EVCS_VERIFY_KEY);

      // act
      const response = (await getHandler({
        ...TEST_GET_EVENT,
        queryStringParameters: {
          state: "ALL",
        },
      })) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(response.statusCode).toBe(200);
      expect(response.body).toBe(JSON.stringify(testResult));
      expect(processGetUserVCsRequest).toHaveBeenCalledWith(TEST_USER_ID, [
        "CURRENT",
        "PENDING",
        "VERIFICATION",
        "ABANDONED",
        "PENDING_RETURN",
        "HISTORIC",
        "VERIFICATION_ARCHIVED",
      ]);
    });

    it("should return 200 for a migration request with no access token", async () => {
      // arrange
      const testResult = { vcs: [] };
      jest.mocked(processGetUserVCsRequest).mockResolvedValueOnce({
        statusCode: 200,
        response: testResult,
      });
      jest.mocked(getParameter).mockResolvedValueOnce(EVCS_VERIFY_KEY);

      const event = {
        pathParameters: TEST_PATH_PARAM,
        path: `/migration/${TEST_USER_ID}`,
        queryStringParameters: {
          state: "CURRENT",
        } as APIGatewayProxyEventQueryStringParameters,
      } as APIGatewayProxyEvent;

      // act
      const response = (await getHandler(
        event,
      )) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(response.statusCode).toBe(200);
      expect(response.body).toBe(JSON.stringify(testResult));
      expect(processGetUserVCsRequest).toHaveBeenCalledWith(TEST_USER_ID, [
        "CURRENT",
      ]);
    });

    it("should return 400 for a request with no user id", async () => {
      // act
      const response = (await getHandler({
        ...TEST_GET_EVENT,
        pathParameters: null,
      })) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(response.statusCode).toBe(400);
      expect(processGetUserVCsRequest).not.toHaveBeenCalled();
    });

    it("should return 400 for a request with no access token", async () => {
      // act
      const response = (await getHandler({
        ...TEST_GET_EVENT,
        headers: {},
      })) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(response.statusCode).toBe(400);
      expect(processGetUserVCsRequest).not.toHaveBeenCalled();
    });

    it("should return 400 for a request with an invalid access token", async () => {
      // act
      const response = (await getHandler({
        ...TEST_GET_EVENT,
        headers: {
          authorization: "Bearer nonsense",
        },
      })) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(response.statusCode).toBe(400);
      expect(processGetUserVCsRequest).not.toHaveBeenCalled();
    });

    it("should return 400 for a request with a valid access token for a different user", async () => {
      // act
      const response = (await getHandler({
        ...TEST_GET_EVENT,
        pathParameters: {
          userId: "some-other-user",
        },
      })) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(response.statusCode).toBe(400);
      expect(processGetUserVCsRequest).not.toHaveBeenCalled();
    });

    it("should return 400 for a request with invalid VC state", async () => {
      // act
      const response = (await getHandler({
        ...TEST_GET_EVENT,
        queryStringParameters: {
          state: "INVALID",
        },
      })) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(response.statusCode).toBe(400);
      expect(processGetUserVCsRequest).not.toHaveBeenCalled();
    });
  });
});
