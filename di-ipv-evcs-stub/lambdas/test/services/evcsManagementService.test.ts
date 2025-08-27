import { mockClient } from "aws-sdk-client-mock";
import {
  DynamoDB,
  PutItemCommand,
  QueryCommand,
} from "@aws-sdk/client-dynamodb";
import { beforeEach } from "@jest/globals";
import EvcsStoredIdentityItem from "../../src/model/storedIdentityItem";
import { marshall } from "@aws-sdk/util-dynamodb";
import { Vot } from "../../src/domain/enums/vot";
import { StoredIdentityRecordType } from "../../src/domain/enums/StoredIdentityRecordType";
import {
  processCreateStoredIdentity,
  processGetStoredIdentity,
} from "../../src/services/evcsManagementService";
import "aws-sdk-client-mock-jest";
import { StatusCodes } from "../../src/domain/enums";
import { getSsmParameter } from "../../src/common/ssmParameter";

const dbMock = mockClient(DynamoDB);

jest.mock("../../src/common/ssmParameter", () => {
  const module = jest.requireActual("../../src/common/ssmParameter");

  return {
    __esModule: true,
    ...module,
    getSsmParameter: jest.fn(),
  };
});

const TEST_USER_ID = "test-user-id";
const TEST_VC_STRING =
  "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ"; // pragma: allowlist secret

const MOCK_TTL_CONFIG = "3600";
const MOCK_TTL = Math.floor(Date.now() / 1000) + parseInt(MOCK_TTL_CONFIG);

const GPG45_SI_RECORD: EvcsStoredIdentityItem = {
  storedIdentity: TEST_VC_STRING,
  userId: TEST_USER_ID,
  recordType: StoredIdentityRecordType.GPG45,
  levelOfConfidence: Vot.P2,
  isValid: true,
  ttl: MOCK_TTL,
};

const TEST_CREATE_SI_REQUEST = {
  userId: TEST_USER_ID,
  si: {
    jwt: TEST_VC_STRING,
    vot: Vot.P2,
  },
};

beforeEach(() => {
  dbMock.reset();
  jest.mocked(getSsmParameter).mockResolvedValue(MOCK_TTL_CONFIG);
});

describe("processGetStoredIdentity", () => {
  it("should return stored identities with a 200 response for user", async () => {
    // Arrange
    const expectedResponse = [{ ...GPG45_SI_RECORD, ttl: undefined }];
    dbMock.on(QueryCommand).resolves(generateQueryResponse([GPG45_SI_RECORD]));

    // Act
    const res = await processGetStoredIdentity(TEST_USER_ID);

    // Assert
    expect(dbMock).toHaveReceivedCommandWith(QueryCommand, {
      KeyConditionExpression: "userId = :userIdValue",
      ExpressionAttributeValues: {
        ":userIdValue": marshall(TEST_USER_ID),
      },
    });
    expect(res.storedIdentities).toEqual(expectedResponse);
  });

  it("should return empty vcs list if no stored identities are found for user", async () => {
    // Arrange
    dbMock.on(QueryCommand).resolves(generateQueryResponse([]));

    // Act
    const res = await processGetStoredIdentity(TEST_USER_ID);

    // Assert
    expect(dbMock).toHaveReceivedCommandWith(QueryCommand, {
      KeyConditionExpression: "userId = :userIdValue",
      ExpressionAttributeValues: {
        ":userIdValue": marshall(TEST_USER_ID),
      },
    });
    expect(res.storedIdentities).toEqual([]);
  });
});

describe("processCreateStoredIdentity", () => {
  it("should successfully create an SI given a valid request", async () => {
    // Act
    const res = await processCreateStoredIdentity(TEST_CREATE_SI_REQUEST);

    // Assert
    expect(res.statusCode).toEqual(StatusCodes.Accepted);
    expect(dbMock).toHaveReceivedCommandWith(PutItemCommand, {
      Item: marshall(GPG45_SI_RECORD, { removeUndefinedValues: true }),
    });
  });
});

function generateQueryResponse(storedIdentities: EvcsStoredIdentityItem[]) {
  return {
    Items: storedIdentities.map((si) => marshall(si)),
  };
}
