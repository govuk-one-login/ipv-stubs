import { mockClient } from "aws-sdk-client-mock";
import { DynamoDB, QueryCommand } from "@aws-sdk/client-dynamodb";
import { beforeEach } from "@jest/globals";
import EvcsStoredIdentityItem from "../../src/model/storedIdentityItem";
import { marshall } from "@aws-sdk/util-dynamodb";
import { Vot } from "../../src/domain/enums/vot";
import { StoredIdentityRecordType } from "../../src/domain/enums/StoredIdentityRecordType";
import { processGetStoredIdentity } from "../../src/services/evcsManagementService";
import "aws-sdk-client-mock-jest";

const dbMock = mockClient(DynamoDB);

const TEST_USER_ID = "test-user-id";
const TEST_VC_STRING =
  "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ"; // pragma: allowlist secret

const GPG45_SI_RECORD: EvcsStoredIdentityItem = {
  storedIdentity: TEST_VC_STRING,
  userId: TEST_USER_ID,
  recordType: StoredIdentityRecordType.GPG45,
  levelOfConfidence: Vot.P2,
  isValid: true,
};

const HMRC_SI_RECORD: EvcsStoredIdentityItem = {
  storedIdentity: TEST_VC_STRING,
  userId: TEST_USER_ID,
  recordType: StoredIdentityRecordType.HMRC,
  levelOfConfidence: Vot.PCL250,
  isValid: true,
};

describe("processGetStoredIdentity", () => {
  beforeEach(() => {
    dbMock.reset();
  });

  it("should return stored identities with a 200 response for user", async () => {
    // Arrange
    const expectedResponse = [GPG45_SI_RECORD, HMRC_SI_RECORD];
    dbMock.on(QueryCommand).resolves(generateQueryResponse(expectedResponse));

    // Act
    const res = await processGetStoredIdentity(TEST_USER_ID);

    // Assert
    expect(dbMock).toHaveReceivedCommandWith(QueryCommand, {
      KeyConditionExpression: "userId = :userIdValue",
      ExpressionAttributeValues: {
        ":userIdValue": marshall(TEST_USER_ID),
      },
    });
    expect(res.vcs).toEqual(expectedResponse);
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
    expect(res.vcs).toEqual([]);
  });
});

function generateQueryResponse(storedIdentities: EvcsStoredIdentityItem[]) {
  return {
    Items: storedIdentities.map((si) => marshall(si)),
  };
}
