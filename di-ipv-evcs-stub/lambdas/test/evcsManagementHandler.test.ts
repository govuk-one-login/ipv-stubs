import { getUserStoredIdentityHandler } from "../src/handlers/evcsManagementHandler";
import {
  APIGatewayProxyEvent,
  APIGatewayProxyEventPathParameters,
  APIGatewayProxyStructuredResultV2,
} from "aws-lambda";
import { processGetStoredIdentity } from "../src/services/evcsManagementService";
import { StoredIdentityRecordType } from "../src/domain/enums/StoredIdentityRecordType";
import { Vot } from "../src/domain/enums/vot";
import { StatusCodes } from "../src/domain/enums";

jest.mock("../src/services/evcsManagementService", () => ({
  processGetStoredIdentity: jest.fn(),
}));

const TEST_USER_ID = "test-user-id";
const TEST_VC_STRING =
  "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ"; // pragma: allowlist secret

describe("evcs management handlers", () => {
  describe("getUserStoredIdentityHandler", () => {
    it("should return 200 and stored identities with valid request", async () => {
      // Arrange
      const expectedGpg45Si = {
        userId: TEST_USER_ID,
        storedIdentity: TEST_VC_STRING,
        recordType: StoredIdentityRecordType.GPG45,
        isValid: true,
        levelOfConfidence: Vot.P2,
      };
      const expectedHmrcSi = {
        userId: TEST_USER_ID,
        storedIdentity: TEST_VC_STRING,
        recordType: StoredIdentityRecordType.HMRC,
        isValid: true,
        levelOfConfidence: Vot.PCL250,
      };
      jest.mocked(processGetStoredIdentity).mockResolvedValue({
        storedIdentities: [expectedGpg45Si, expectedHmrcSi],
      });
      const getRequest = generateGetStoredIdentityRequest(TEST_USER_ID);

      // Act
      const res = (await getUserStoredIdentityHandler(
        getRequest,
      )) as APIGatewayProxyStructuredResultV2;

      // Assert
      expect(res.statusCode).toBe(StatusCodes.Success);
      expect(res.body).toBe(JSON.stringify([expectedGpg45Si, expectedHmrcSi]));
    });

    it("should return 400 if missing userId", async () => {
      // Arrange
      const getRequest = generateGetStoredIdentityRequest();

      // Act
      const res = (await getUserStoredIdentityHandler(
        getRequest,
      )) as APIGatewayProxyStructuredResultV2;

      // Assert
      expect(res.statusCode).toBe(StatusCodes.BadRequest);
      expect(res.body).toBe(JSON.stringify({ message: "Missing userId" }));
    });

    it("should return 404 if no identity found for user", async () => {
      // Arrange
      const getRequest = generateGetStoredIdentityRequest(TEST_USER_ID);
      jest.mocked(processGetStoredIdentity).mockResolvedValue({
        storedIdentities: [],
      });

      // Act
      const res = (await getUserStoredIdentityHandler(
        getRequest,
      )) as APIGatewayProxyStructuredResultV2;

      // Assert
      expect(res.statusCode).toBe(StatusCodes.NotFound);
      expect(res.body).toBe(
        JSON.stringify({ message: "No stored identity found for user" }),
      );
    });

    it("should return 500 if processGetStoredIdentity throws an error", async () => {
      // Arrange
      const getRequest = generateGetStoredIdentityRequest(TEST_USER_ID);
      jest
        .mocked(processGetStoredIdentity)
        .mockRejectedValue(
          new Error("Something bad happened with retrieving the identity"),
        );

      // Act
      const res = (await getUserStoredIdentityHandler(
        getRequest,
      )) as APIGatewayProxyStructuredResultV2;

      // Assert
      expect(res.statusCode).toBe(StatusCodes.InternalServerError);
      expect(res.body).toBe(
        JSON.stringify({ message: "Unable to get stored identity for user" }),
      );
    });
  });
});

function generateGetStoredIdentityRequest(userId?: string) {
  return {
    pathParameters: { userId } as APIGatewayProxyEventPathParameters,
  } as APIGatewayProxyEvent;
}
