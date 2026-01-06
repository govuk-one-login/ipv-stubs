import crypto from "crypto";
import {
  TestUser,
  DocumentType,
  EvidenceType,
  isDrivingPermitCredentialSubject,
} from "./managementEnqueueRequest";
import getConfig from "../common/config";
import { DOCUMENT_CLAIMS } from "../data/vcDocumentClaims";
import { USER_CLAIMS } from "../data/vcUserClaims";
import { EVIDENCE_CLAIMS } from "../data/vcEvidenceClaims";

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

  const documentDetails = DOCUMENT_CLAIMS[documentType];
  const credentialSubject = {
    ...USER_CLAIMS[testUser],
    ...(isDrivingPermitCredentialSubject(documentDetails)
      ? {
          drivingPermit: [
            {
              ...documentDetails.drivingPermit[0],
              expiryDate:
                drivingPermitExpiryDate ??
                getFutureExpiryDateStringFromIssuedAt(timestamp),
            },
          ],
        }
      : documentDetails),
  };

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
          ...EVIDENCE_CLAIMS[documentType][evidenceType],
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

function getFutureExpiryDateStringFromIssuedAt(issuedAt: number): string {
  // Create a default future date, 30 days ahead of the VC issued at date
  return new Date((issuedAt + 30 * 24 * 60 * 60) * 1000)
    .toISOString()
    .split("T")[0];
}
