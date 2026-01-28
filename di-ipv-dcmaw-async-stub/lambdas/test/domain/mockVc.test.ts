import { buildMockVc } from "../../src/domain/mockVc";
import {
  DocumentType,
  DrivingPermitCredentialSubject,
  EvidenceType,
  PassportCredentialSubject,
  TestUser,
} from "../../src/domain/managementEnqueueRequest";
import { USER_CLAIMS } from "../../src/data/vcUserClaims";
import { DOCUMENT_CLAIMS } from "../../src/data/vcDocumentClaims";
import { EVIDENCE_CLAIMS } from "../../src/data/vcEvidenceClaims";

jest.useFakeTimers().setSystemTime(new Date("2023-01-01"));

jest.mock(
  "../../src/common/config",
  () => () =>
    Promise.resolve({
      vcIssuer: "vc-issuer",
      vcAudience: "vc-audience",
    }),
);

const TEST_USER_ID = "testUserId";

describe("buildMockVc", () => {
  test("returns driving permit mock VC with future expiry date as 30 after current date if drivingPermitExpiryDate is not provided", async () => {
    // Act
    const vc = await buildMockVc(
      TEST_USER_ID,
      TestUser.kennethD,
      DocumentType.drivingPermit,
      EvidenceType.success,
      undefined,
      ["CI1", "CI2"],
    );

    // Assert
    expect(vc.sub).toEqual(TEST_USER_ID);
    expect(vc.nbf).toEqual(1672531200); // 2023-01-01T00:00:00

    const credentials = vc.vc
      .credentialSubject as DrivingPermitCredentialSubject;
    expect(credentials["name"]).toEqual(USER_CLAIMS[TestUser.kennethD].name);
    expect(credentials["birthDate"]).toEqual(
      USER_CLAIMS[TestUser.kennethD].birthDate,
    );

    const expectedExpiryDate = "2023-01-31";
    expect(credentials.drivingPermit).toEqual([
      {
        ...DOCUMENT_CLAIMS[DocumentType.drivingPermit].drivingPermit[0],
        expiryDate: expectedExpiryDate,
      },
    ]);

    expect(vc.vc.evidence[0]).toEqual(
      expect.objectContaining({
        ...EVIDENCE_CLAIMS[DocumentType.drivingPermit][EvidenceType.success],
        ci: ["CI1", "CI2"],
        txn: expect.any(String),
      }),
    );
  });

  test("returns driving permit mock VC with custom drivingPermitExpiryDate when provided", async () => {
    // Arrange
    const expectedExpiryDate = "2022-01-01";

    // Act
    const vc = await buildMockVc(
      TEST_USER_ID,
      TestUser.kennethD,
      DocumentType.drivingPermit,
      EvidenceType.success,
      expectedExpiryDate,
      undefined,
    );

    // Assert
    expect(vc.sub).toEqual(TEST_USER_ID);

    const credentials = vc.vc
      .credentialSubject as DrivingPermitCredentialSubject;
    expect(credentials["name"]).toEqual(USER_CLAIMS[TestUser.kennethD].name);
    expect(credentials["birthDate"]).toEqual(
      USER_CLAIMS[TestUser.kennethD].birthDate,
    );

    expect(credentials.drivingPermit).toEqual([
      {
        ...DOCUMENT_CLAIMS[DocumentType.drivingPermit].drivingPermit[0],
        expiryDate: expectedExpiryDate,
      },
    ]);

    expect(vc.vc.evidence[0]).toEqual(
      expect.objectContaining({
        ...EVIDENCE_CLAIMS[DocumentType.drivingPermit][EvidenceType.success],
        ci: [],
        txn: expect.any(String),
      }),
    );
  });

  test("returns mock passport vc for given test user", async () => {
    // Act
    const vc = await buildMockVc(
      TEST_USER_ID,
      TestUser.kennethD,
      DocumentType.ukChippedPassport,
      EvidenceType.success,
      undefined,
      ["CI1", "CI2"],
    );

    // Assert
    expect(vc.sub).toEqual(TEST_USER_ID);

    const credentials = vc.vc.credentialSubject as PassportCredentialSubject;
    expect(credentials["name"]).toEqual(USER_CLAIMS[TestUser.kennethD].name);
    expect(credentials["birthDate"]).toEqual(
      USER_CLAIMS[TestUser.kennethD].birthDate,
    );

    expect(credentials.passport).toEqual([
      DOCUMENT_CLAIMS[DocumentType.ukChippedPassport].passport[0],
    ]);

    expect(vc.vc.evidence[0]).toEqual(
      expect.objectContaining({
        ...EVIDENCE_CLAIMS[DocumentType.ukChippedPassport][
          EvidenceType.success
        ],
        ci: ["CI1", "CI2"],
        txn: expect.any(String),
      }),
    );
  });

  test("returns mock driving licence with custom nbf", async () => {
    // Act
    const expectedNbf = 1769587200; // 2026-01-28T08:00:00
    const vc = await buildMockVc(
      TEST_USER_ID,
      TestUser.kennethD,
      DocumentType.drivingPermit,
      EvidenceType.success,
      undefined,
      undefined,
      expectedNbf,
    );

    // Assert
    expect(vc.sub).toEqual(TEST_USER_ID);
    expect(vc.nbf).toEqual(expectedNbf);

    const credentials = vc.vc
      .credentialSubject as DrivingPermitCredentialSubject;
    expect(credentials["name"]).toEqual(USER_CLAIMS[TestUser.kennethD].name);
    expect(credentials["birthDate"]).toEqual(
      USER_CLAIMS[TestUser.kennethD].birthDate,
    );

    const expectedExpiryDate = "2023-01-31";
    expect(credentials.drivingPermit).toEqual([
      {
        ...DOCUMENT_CLAIMS[DocumentType.drivingPermit].drivingPermit[0],
        expiryDate: expectedExpiryDate,
      },
    ]);

    expect(vc.vc.evidence[0]).toEqual(
      expect.objectContaining({
        ...EVIDENCE_CLAIMS[DocumentType.drivingPermit][EvidenceType.success],
        ci: [],
        txn: expect.any(String),
      }),
    );
  });
});
