import { mockClient } from "aws-sdk-client-mock";
import { DynamoDB, QueryCommand } from "@aws-sdk/client-dynamodb";
import { marshall } from "@aws-sdk/util-dynamodb";
import { getUserIdentity } from "../../src/services/sisService";

const dbMock = mockClient(DynamoDB);

const TEST_USER_ID = "userId";
const TEST_SI_JWT =
  "eyJraWQiOiJ0ZXN0LXNpZ25pbmcta2V5IiwidHlwIjoiSldUIiwiYWxnIjoiRVMyNTYifQ.eyJhdWQiOiJodHRwczovL3JldXNlLWlkZW50aXR5LmJ1aWxkLmFjY291bnQuZ292LnVrIiwic3ViIjoiZWFlMDFhYzI5MGE5ODRkMGVhN2MzM2NjNDVlMzZmMTIiLCJuYmYiOjE3NTA2ODIwMTgsImNyZWRlbnRpYWxzIjpbIk43UHhoZmtGa215VFFGS3lBWE15U19INk51Ri13RHpFa3RiX2RWdXJ1bFNSTU1YaG54aGJSMnJ4czlUYy1LUUIwaVhiMV85YUJJOFhDeTJBYkdRdkZRIiwiUzROSlBjaWltYmZ4MDhqczltOThoc3JLTDRiSkh0QlF5S0d0cmRJeklmWW1CUGpyVTlwYXpfdV8xaENySFo4aWp5UW81UlBtUWxNUC1fYzVldXZaSHciLCJBOU9IdUtJOE41aDRDNDU3UTRxdE52a1NGS2ZGZVZNNHNFR3dxUlBjU0hpUXlsemh4UnlxMDBlMURVUUxtU2RpZTlYSWswQ2ZpUVNBX3I3LW1tQ2JBdyIsInk0NHYwcEVBODh6dURoREZEQ0RjUGduOTZwOWJTRm9qeHZQQTFCeEdYTnhEMG5QelFONk1SaG1PWXBTUXg4TW92XzNLWUF4bmZ5aXdSemVBclhKa3FBIl0sImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkubG9jYWwuYWNjb3VudC5nb3YudWsiLCJjbGFpbXMiOnsiaHR0cHM6Ly92b2NhYi5hY2NvdW50Lmdvdi51ay92MS9jb3JlSWRlbnRpdHkiOnsibmFtZSI6W3sibmFtZVBhcnRzIjpbeyJ0eXBlIjoiR2l2ZW5OYW1lIiwidmFsdWUiOiJLRU5ORVRIIn0seyJ0eXBlIjoiRmFtaWx5TmFtZSIsInZhbHVlIjoiREVDRVJRVUVJUkEifV19XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5NjUtMDctMDgifV19LCJodHRwczovL3ZvY2FiLmFjY291bnQuZ292LnVrL3YxL2FkZHJlc3MiOlt7ImFkZHJlc3NDb3VudHJ5IjoiR0IiLCJhZGRyZXNzTG9jYWxpdHkiOiJCQVRIIiwiYnVpbGRpbmdOYW1lIjoiIiwiYnVpbGRpbmdOdW1iZXIiOiI4IiwicG9zdGFsQ29kZSI6IkJBMiA1QUEiLCJzdHJlZXROYW1lIjoiSEFETEVZIFJPQUQiLCJzdWJCdWlsZGluZ05hbWUiOiIiLCJ1cHJuIjoxMDAxMjAwMTIwNzcsInZhbGlkRnJvbSI6IjEwMDAtMDEtMDEifV0sImh0dHBzOi8vdm9jYWIuYWNjb3VudC5nb3YudWsvdjEvcGFzc3BvcnQiOlt7ImRvY3VtZW50TnVtYmVyIjoiMzIxNjU0OTg3IiwiZXhwaXJ5RGF0ZSI6IjIwMzAtMDEtMDEiLCJpY2FvSXNzdWVyQ29kZSI6IkdCUiJ9XX0sInZvdCI6IlAyIiwiaWF0IjoxNzUwNjgyMDE4fQ.nrbiwaOcvWM92TTAlORzerjjrrCuYD9fcxwEoXbf71J3YZUnwNW0KGUN5jaEvOysG0YWTXSLl_W4sN-Krf7PfQ"; // pragma: allowlist secret

const TEST_VTRS = ["P2"];

const MOCK_USER_IDENTITY = {
  userId: TEST_USER_ID,
  isValid: true,
  levelOfConfidence: "P2",
  storedIdentity: TEST_SI_JWT,
};

beforeEach(() => {
  dbMock.reset();
});

describe("getUserIdentity", () => {
  it("should return valid user identity object if it exists", async () => {
    // Arrange
    dbMock.on(QueryCommand).resolves({
      Items: [marshall(MOCK_USER_IDENTITY)],
    });

    // Act
    const res = await getUserIdentity(TEST_USER_ID, TEST_VTRS);

    // Assert
    expect(res).toEqual({
      content: MOCK_USER_IDENTITY.storedIdentity,
      vot: MOCK_USER_IDENTITY.levelOfConfidence,
      expired: false,
      isValid: MOCK_USER_IDENTITY.isValid,
      signatureValid: true,
      kidValid: true,
    });
  });

  it("should return malformed JSON if required properties are missing and isValid=false", async () => {
    // Arrange
    const malformedJson = { isValid: true };
    dbMock.on(QueryCommand).resolves({
      Items: [marshall(malformedJson)],
    });

    // Act
    const res = await getUserIdentity(TEST_USER_ID, TEST_VTRS);

    // Assert
    expect(res).toEqual({
      ...malformedJson,
      expired: false,
      kidValid: true,
      signatureValid: true,
      isValid: false,
    });
  });

  it("should return null if it no SI exists for user", async () => {
    // Arrange
    dbMock.on(QueryCommand).resolves({
      Items: [],
    });

    // Act
    const res = await getUserIdentity(TEST_USER_ID, TEST_VTRS);

    // Assert
    expect(res).toBeNull();
  });

  it("should return not valid and vot=P0 if level of confidence on sis record does not match any of the requested VTRs", async () => {
    // Arrange
    dbMock.on(QueryCommand).resolves({
      Items: [marshall({ ...MOCK_USER_IDENTITY, levelOfConfidence: "P1" })],
    });

    // Act
    const res = await getUserIdentity(TEST_USER_ID, TEST_VTRS);

    // Assert
    expect(res).toEqual({
      content: MOCK_USER_IDENTITY.storedIdentity,
      vot: "P0",
      expired: false,
      kidValid: true,
      signatureValid: true,
      isValid: false,
    });
  });

  it("should return strongest matched profile", async () => {
    // Arrange
    dbMock.on(QueryCommand).resolves({
      Items: [marshall({ ...MOCK_USER_IDENTITY, levelOfConfidence: "P2" })],
    });

    // Act
    const res = await getUserIdentity(TEST_USER_ID, ["P1"]);

    // Assert
    expect(res).toEqual({
      content: MOCK_USER_IDENTITY.storedIdentity,
      vot: "P1",
      expired: false,
      kidValid: true,
      signatureValid: true,
      isValid: true,
    });
  });
});
