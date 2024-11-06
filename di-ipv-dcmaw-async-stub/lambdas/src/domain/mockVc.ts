import crypto from "crypto";
import {
  TestUser,
  DocumentType,
  EvidenceType,
} from "./managementEnqueueRequest";
import getConfig from "../common/config";

export async function buildMockVc(
  userId: string,
  testUser: TestUser,
  documentType: DocumentType,
  evidenceType: EvidenceType,
  ci: string[] = [],
) {
  const config = await getConfig();
  const timestamp = Math.round(new Date().getTime() / 1000);
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
      credentialSubject: {
        ...testUserClaims[testUser],
        ...documentClaims[documentType],
      },
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

const testUserClaims = {
  [TestUser.kennethD]: {
    birthDate: [
      {
        value: "1965-07-08",
      },
    ],
  },
};

const documentClaims = {
  [DocumentType.ukChippedPassport]: {
    passport: [
      {
        documentNumber: "321654987",
        expiryDate: "2030-01-01",
        icaoIssuerCode: "GBR",
      },
    ],
  },
};

const evidence = {
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
};
