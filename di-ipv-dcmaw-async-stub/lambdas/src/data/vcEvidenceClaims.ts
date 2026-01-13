import { DocumentType, EvidenceType } from "../domain/managementEnqueueRequest";

export const EVIDENCE_CLAIMS = {
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
      strengthScore: 3,
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
