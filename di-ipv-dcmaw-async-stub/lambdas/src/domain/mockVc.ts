import crypto from "crypto";
import {
  TestUser,
  DocumentType,
  EvidenceType,
  isDrivingPermitCredentialSubject,
} from "./managementEnqueueRequest";
import getConfig from "../common/config";

export async function buildMockVc(
  userId: string,
  testUser: TestUser,
  documentType: DocumentType,
  evidenceType: EvidenceType,
  drivingPermitExpiryDate?: string,
  ci: string[] = [],
) {
  const config = await getConfig();
  const timestamp = Math.round(new Date().getTime() / 1000);

  const documentDetails = documentClaims[documentType];
  const baseCredentialSubject = {
    ...testUserClaims[testUser],
    ...documentDetails,
  };

  let credentialSubject;
  if (isDrivingPermitCredentialSubject(documentDetails)) {
    // Create a default future date, 30 days ahead of the VC issued at date
    const futureDate = new Date((timestamp + 30 * 24 * 60 * 60) * 1000)
      .toISOString()
      .split("T")[0];

    credentialSubject = {
      ...baseCredentialSubject,
      drivingPermit: [
        {
          ...documentDetails.drivingPermit[0],
          expiryDate: drivingPermitExpiryDate ?? futureDate,
        },
      ],
    };
  } else {
    credentialSubject = baseCredentialSubject;
  }

  return {
    jti: crypto.randomUUID(),
    iss: config.vcIssuer,
    aud: config.vcAudience,
    sub: userId,
    iat: timestamp,
    nbf: timestamp,
    vc: {
      "@context": [
        "https://www.w3.org/2018/credentials/v1",
        "https://vocab.account.gov.uk/contexts/identity-v1.jsonld",
      ],
      type: ["VerifiableCredential", "IdentityCheckCredential"],
      credentialSubject,
      evidence: [
        {
          ...evidence[documentType][evidenceType],
          txn: crypto.randomUUID(),
          ci,
        },
      ],
    },
  };
}

export async function buildMockVcFromSubjectAndEvidence(
  userId: string,
  credentialSubject: object,
  evidence: object,
  nbf?: number,
) {
  const config = await getConfig();
  const timestamp = Math.round(new Date().getTime() / 1000);
  return {
    jti: crypto.randomUUID(),
    iss: config.vcIssuer,
    aud: config.vcAudience,
    sub: userId,
    iat: timestamp,
    nbf: nbf || timestamp,
    vc: {
      "@context": [
        "https://www.w3.org/2018/credentials/v1",
        "https://vocab.account.gov.uk/contexts/identity-v1.jsonld",
      ],
      type: ["VerifiableCredential", "IdentityCheckCredential"],
      credentialSubject: credentialSubject,
      evidence: [evidence],
    },
  };
}

export const testUserClaims = {
  [TestUser.kennethD]: {
    name: [
      {
        nameParts: [
          {
            value: "Kenneth",
            type: "GivenName",
          },
          {
            value: "Decerqueira",
            type: "FamilyName",
          },
        ],
      },
    ],
    birthDate: [
      {
        value: "1965-07-08",
      },
    ],
  },
};

export const documentClaims = {
  [DocumentType.ukChippedPassport]: {
    passport: [
      {
        documentNumber: "321654987",
        expiryDate: "2030-01-01",
        icaoIssuerCode: "GBR",
      },
    ],
  },
  [DocumentType.drivingPermit]: {
    drivingPermit: [
      {
        expiryDate: "2033-01-18",
        issueNumber: "5",
        issuedBy: "DVLA",
        fullAddress: "8 HADLEY ROAD BATH BA2 5AA",
        personalNumber: "DECER607085K9123",
        issueDate: "2023-01-18",
      },
    ],
  },
};

export const evidence = {
  [DocumentType.ukChippedPassport]: {
    [EvidenceType.success]: {
      type: "IdentityCheck",
      strengthScore: 4,
      validityScore: 3,
      checkDetails: [
        {
          checkMethod: "vcrypt",
          identityCheckPolicy: "published",
          activityFrom: null,
        },
        {
          checkMethod: "bvr",
          biometricVerificationProcessLevel: 3,
        },
      ],
    },
    [EvidenceType.fail]: {
      type: "IdentityCheck",
      strengthScore: 4,
      validityScore: 0,
      failedCheckDetails: [
        {
          checkMethod: "vcrypt",
          identityCheckPolicy: "published",
          activityFrom: null,
        },
        {
          checkMethod: "bvr",
          biometricVerificationProcessLevel: 3,
        },
      ],
    },
  },
  [DocumentType.drivingPermit]: {
    [EvidenceType.success]: {
      type: "IdentityCheck",
      strengthScore: 3,
      validityScore: 2,
      activityHistoryScore: 1,
      checkDetails: [
        {
          checkMethod: "vcrypt",
          identityCheckPolicy: "published",
          activityFrom: null,
        },
        {
          checkMethod: "bvr",
          biometricVerificationProcessLevel: 3,
        },
      ],
    },
    [EvidenceType.fail]: {
      type: "IdentityCheck",
      strengthScore: 4,
      validityScore: 0,
      failedCheckDetails: [
        {
          checkMethod: "vcrypt",
          identityCheckPolicy: "published",
          activityFrom: null,
        },
        {
          checkMethod: "bvr",
          biometricVerificationProcessLevel: 3,
        },
      ],
    },
  },
};
