import { beforeEach } from "@jest/globals";
import { mockClient } from "aws-sdk-client-mock";
import {
  DynamoDB,
  QueryCommand,
  TransactWriteItemsCommand,
} from "@aws-sdk/client-dynamodb";
import { marshall } from "@aws-sdk/util-dynamodb";
import { getSsmParameter } from "../../src/common/ssmParameter";
import { processPutUserVCsRequest } from "../../src/services/evcsService";
import { PutRequest } from "../../src/domain/requests";
import { StatusCodes, VCProvenance, VcState } from "../../src/domain/enums";
import "aws-sdk-client-mock-jest";
import { config } from "../../src/common/config";

jest.useFakeTimers().setSystemTime(new Date("2025-01-01"));

jest.mock("../../src/common/ssmParameter", () => {
  const module = jest.requireActual("../../src/common/ssmParameter");

  return {
    __esModule: true,
    ...module,
    getSsmParameter: jest.fn(),
  };
});

const dbMock = mockClient(DynamoDB);

const TEST_USER_ID: string = "urn:uuid:d1823066-2137-4380-b0ba-4b61947e08e6";

const TEST_VC1_SIGNATURE =
  "qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ"; // pragma: allowlist secret"
const TEST_VC1 = `eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.${TEST_VC1_SIGNATURE}`; // pragma: allowlist secret

const TEST_VC2_SIGNATURE =
  "xdHyZUEwwi6TCiM38UyNdH-bHdB14Bxm6m1UcnXNRGVxqqGGDuqh087lL3mOFMwPEUldLE6NeJ9B8tVdHrkRyA"; // pragma: allowlist secret
const TEST_VC2 = `eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ1cm46dXVpZDo5NjI4OTgxNS0wZTUyLTQ4MDAtOTZkZi0xZmY3ZGU5ODFjZDQiLCJhdWQiOiJodHRwczovL2lkZW50aXR5LmJ1aWxkLmFjY291bnQuZ292LnVrIiwibmJmIjoxNzQ2MDkyNTk3LCJpc3MiOiJodHRwczovL2FkZHJlc3MtY3JpLnN0dWJzLmFjY291bnQuZ292LnVrIiwidmMiOnsidHlwZSI6WyJWZXJpZmlhYmxlQ3JlZGVudGlhbCIsIkFkZHJlc3NDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7ImFkZHJlc3MiOlt7ImFkZHJlc3NDb3VudHJ5IjoiR0IiLCJidWlsZGluZ05hbWUiOiIiLCJzdHJlZXROYW1lIjoiSEFETEVZIFJPQUQiLCJwb3N0YWxDb2RlIjoiQkEyIDVBQSIsImJ1aWxkaW5nTnVtYmVyIjoiOCIsImFkZHJlc3NMb2NhbGl0eSI6IkJBVEgiLCJ2YWxpZEZyb20iOiIyMDAwLTAxLTAxIn1dfX0sImp0aSI6InVybjp1dWlkOmMwNTVlYWVjLTAyZjUtNDQ1NC05NzkyLTYyYzljNWQ3YzAwNyJ9.${TEST_VC2_SIGNATURE}`; // pragma: allowlist secret

const TEST_VC3_SIGNATURE =
  "ggN9Y1utkbVunE5brcR3KmXntj5jK_jzccXlLnUO_2DMENTDuBUDlh4dkt7K-upx_n0Ygu125TzduNad41QzoA"; // pragma: allowlist secret
const TEST_VC3 = `eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ1cm46dXVpZDo5NjI4OTgxNS0wZTUyLTQ4MDAtOTZkZi0xZmY3ZGU5ODFjZDQiLCJhdWQiOiJodHRwczovL2lkZW50aXR5LmJ1aWxkLmFjY291bnQuZ292LnVrIiwibmJmIjoxNzQ2MDkyNjAzLCJpc3MiOiJodHRwczovL2ZyYXVkLWNyaS5zdHVicy5hY2NvdW50Lmdvdi51ayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJJZGVudGl0eUNoZWNrQ3JlZGVudGlhbCJdLCJjcmVkZW50aWFsU3ViamVjdCI6eyJuYW1lIjpbeyJuYW1lUGFydHMiOlt7InZhbHVlIjoiS2VubmV0aCIsInR5cGUiOiJHaXZlbk5hbWUifSx7InZhbHVlIjoiRGVjZXJxdWVpcmEiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XX1dLCJiaXJ0aERhdGUiOlt7InZhbHVlIjoiMTk2NS0wNy0wOCJ9XSwiYWRkcmVzcyI6W3siYWRkcmVzc0NvdW50cnkiOiJHQiIsImJ1aWxkaW5nTmFtZSI6IiIsInN0cmVldE5hbWUiOiJIQURMRVkgUk9BRCIsInBvc3RhbENvZGUiOiJCQTIgNUFBIiwiYnVpbGRpbmdOdW1iZXIiOiI4IiwiYWRkcmVzc0xvY2FsaXR5IjoiQkFUSCIsInZhbGlkRnJvbSI6IjIwMDAtMDEtMDEifV19LCJldmlkZW5jZSI6W3sidHlwZSI6IklkZW50aXR5Q2hlY2siLCJpZGVudGl0eUZyYXVkU2NvcmUiOjIsInR4biI6IjM0OTg4OTVhLTk0Y2MtNDlkZS04MDc0LTQ5OWU1MjczZDI2YyJ9XX0sImp0aSI6InVybjp1dWlkOjg5ZDY5MTAwLTRiOTctNDYxZi1hOTlkLTNlZDZjMzNkNGVhNCJ9.${TEST_VC3_SIGNATURE}`; // pragma: allowlist secret

const TEST_METADATA = {
  reason: "test-created",
  timestampMs: "1714478033959",
  txmaEventId: "txma-event-id",
  testProperty: "testProperty",
};

const MOCK_TTL = "3600";

describe("processPutUserVCsRequest", () => {
  beforeEach(() => {
    dbMock.reset();
    jest.mocked(getSsmParameter).mockResolvedValue(MOCK_TTL);
  });

  it("should return 200 response with new vc if successful transaction", async () => {
    // Arrange
    dbMock.on(QueryCommand).resolves(createQueryResponse([]));

    const putRequest: PutRequest = {
      userId: TEST_USER_ID,
      vcs: [
        {
          vc: TEST_VC1,
          state: VcState.CURRENT,
        },
      ],
      si: {
        jwt: TEST_VC2,
        vot: "P2",
      },
    };

    // Act
    const response = await processPutUserVCsRequest(putRequest);

    // Assert
    expect(response.statusCode).toBe(StatusCodes.Accepted);
    expect(dbMock).toHaveReceivedCommand(QueryCommand);

    expect(dbMock).toHaveReceivedCommandWith(TransactWriteItemsCommand, {
      TransactItems: [
        createStubUserVcPutItem({
          vcSignature: TEST_VC1_SIGNATURE,
          vc: TEST_VC1,
          state: VcState.CURRENT,
        }),
        createStoredIdentityPutItem({
          jwtSignature: TEST_VC2_SIGNATURE,
          storedIdentity: TEST_VC2,
          levelOfConfidence: "P2",
        }),
      ],
    });
  });

  it("should return 200 response with updated vc if successful transaction", async () => {
    // Arrange
    dbMock.on(QueryCommand).resolves(
      createQueryResponse([
        {
          vc: TEST_VC1,
          state: VcState.HISTORIC,
        },
      ]),
    );

    const putRequest: PutRequest = {
      userId: TEST_USER_ID,
      vcs: [
        {
          vc: TEST_VC1,
          state: VcState.CURRENT,
          metadata: TEST_METADATA,
          provenance: VCProvenance.EXTERNAL,
        },
      ],
      si: {
        jwt: TEST_VC2,
        vot: "P2",
        metadata: TEST_METADATA,
      },
    };

    // Act
    const response = await processPutUserVCsRequest(putRequest);

    // Assert
    expect(response.statusCode).toBe(StatusCodes.Accepted);
    expect(dbMock).toHaveReceivedCommand(QueryCommand);

    expect(dbMock).toHaveReceivedCommandWith(TransactWriteItemsCommand, {
      TransactItems: [
        createStubUserVcPutItem({
          vcSignature: TEST_VC1_SIGNATURE,
          vc: TEST_VC1,
          metadata: TEST_METADATA,
          state: VcState.CURRENT,
          provenance: VCProvenance.EXTERNAL,
        }),
        createStoredIdentityPutItem({
          jwtSignature: TEST_VC2_SIGNATURE,
          storedIdentity: TEST_VC2,
          levelOfConfidence: "P2",
          metadata: TEST_METADATA,
        }),
      ],
    });
  });

  it("should return 200 response if SI not provided", async () => {
    // Arrange
    dbMock.on(QueryCommand).resolves(createQueryResponse([]));

    const putRequest: PutRequest = {
      userId: TEST_USER_ID,
      vcs: [
        {
          vc: TEST_VC1,
          state: VcState.CURRENT,
        },
      ],
    };

    // Act
    const response = await processPutUserVCsRequest(putRequest);

    // Assert
    expect(response.statusCode).toBe(StatusCodes.Accepted);
    expect(dbMock).toHaveReceivedCommand(QueryCommand);

    expect(dbMock).toHaveReceivedCommandWith(TransactWriteItemsCommand, {
      TransactItems: [
        createStubUserVcPutItem({
          vcSignature: TEST_VC1_SIGNATURE,
          vc: TEST_VC1,
          state: VcState.CURRENT,
        }),
      ],
    });
  });

  it("should return 200 response, update existing VCs and add new ones if successful transaction", async () => {
    // Arrange
    dbMock.on(QueryCommand).resolves(
      createQueryResponse([
        { vc: TEST_VC1, state: VcState.CURRENT },
        { vc: TEST_VC2, state: VcState.PENDING_RETURN },
      ]),
    );

    const putRequest: PutRequest = {
      userId: TEST_USER_ID,
      vcs: [
        {
          vc: TEST_VC3,
          state: VcState.CURRENT,
        },
      ],
      si: {
        jwt: TEST_VC2,
        vot: "P2",
      },
    };

    // Act
    const response = await processPutUserVCsRequest(putRequest);

    // Assert
    expect(response.statusCode).toBe(StatusCodes.Accepted);
    expect(dbMock).toHaveReceivedCommand(QueryCommand);

    expect(dbMock).toHaveReceivedCommandWith(TransactWriteItemsCommand, {
      TransactItems: [
        createUserVcUpdateItem({
          vcSignature: TEST_VC1_SIGNATURE,
          state: VcState.HISTORIC,
        }),
        createUserVcUpdateItem({
          vcSignature: TEST_VC2_SIGNATURE,
          state: VcState.ABANDONED,
        }),
        createStubUserVcPutItem({
          vcSignature: TEST_VC3_SIGNATURE,
          vc: TEST_VC3,
          state: VcState.CURRENT,
          provenance: VCProvenance.ONLINE,
        }),
        createStoredIdentityPutItem({
          jwtSignature: TEST_VC2_SIGNATURE,
          storedIdentity: TEST_VC2,
          levelOfConfidence: "P2",
        }),
      ],
    });
  });

  it("should return 500 status code if it fails to get existing user VCs", async () => {
    // Arrange
    dbMock.on(QueryCommand).rejects(new Error("Failed to get existing VCs"));

    const putRequest: PutRequest = {
      userId: TEST_USER_ID,
      vcs: [
        {
          vc: TEST_VC3,
          state: VcState.CURRENT,
        },
      ],
    };

    // Act
    const response = await processPutUserVCsRequest(putRequest);

    // Assert
    expect(response.statusCode).toBe(StatusCodes.InternalServerError);
    expect(dbMock).not.toHaveReceivedCommand(TransactWriteItemsCommand);
  });

  it("should return 500 status code if it fails to complete the transaction", async () => {
    // Arrange
    dbMock.on(QueryCommand).resolves(createQueryResponse([]));
    dbMock
      .on(TransactWriteItemsCommand)
      .rejects(new Error("Failed to complete transaction"));

    const putRequest: PutRequest = {
      userId: TEST_USER_ID,
      vcs: [
        {
          vc: TEST_VC3,
          state: VcState.CURRENT,
        },
      ],
    };

    // Act
    const response = await processPutUserVCsRequest(putRequest);

    // Assert
    expect(response.statusCode).toBe(StatusCodes.InternalServerError);
    expect(dbMock).toHaveReceivedCommand(QueryCommand);
  });
});

function getTestTtl() {
  return Math.floor(Date.now() / 1000) + parseInt(MOCK_TTL);
}

function createQueryResponse(
  vcsForReturn: {
    vc: string;
    state: VcState;
  }[],
) {
  return {
    Items: vcsForReturn.map(({ vc, state }) =>
      marshall({
        userId: TEST_USER_ID,
        vc,
        state,
        metadata: TEST_METADATA,
      }),
    ),
  };
}

function createStubUserVcPutItem(putVcDetails: {
  vc: string;
  vcSignature: string;
  metadata?: object;
  state?: VcState;
  provenance?: VCProvenance;
}) {
  return {
    Put: {
      TableName: config.evcsStubUserVCsTableName,
      Item: marshall(
        {
          userId: TEST_USER_ID,
          vcSignature: putVcDetails.vcSignature,
          vc: putVcDetails.vc,
          metadata: putVcDetails.metadata,
          state: putVcDetails.state,
          provenance: putVcDetails.provenance || VCProvenance.ONLINE,
          ttl: getTestTtl(),
        },
        { removeUndefinedValues: true },
      ),
    },
  };
}

function createStoredIdentityPutItem(putSiDetails: {
  jwtSignature: string;
  storedIdentity: string;
  levelOfConfidence: string;
  metadata?: object;
}) {
  return {
    Put: {
      TableName: config.evcsStoredIdentityObjectTableName,
      Item: marshall(
        {
          userId: TEST_USER_ID,
          jwtSignature: putSiDetails.jwtSignature,
          storedIdentity: putSiDetails.storedIdentity,
          levelOfConfidence: putSiDetails.levelOfConfidence,
          isValid: true,
          metadata: putSiDetails.metadata,
        },
        { removeUndefinedValues: true },
      ),
    },
  };
}

function createUserVcUpdateItem(updateVcDetails: {
  vcSignature: string;
  state: VcState;
}) {
  return {
    Update: {
      TableName: config.evcsStubUserVCsTableName,
      Key: marshall({
        userId: TEST_USER_ID,
        vcSignature: updateVcDetails.vcSignature,
      }),
      UpdateExpression: "set #state = :stateValue, #metadata = :metadataValue",
      ExpressionAttributeNames: {
        "#state": "state",
        "#metadata": "metadata",
      },
      ExpressionAttributeValues: {
        ":stateValue": marshall(updateVcDetails.state),
        ":metadata": {
          M: marshall(TEST_METADATA),
        },
      },
    },
  };
}
