import { DocumentType } from "../domain/managementEnqueueRequest";

export const DOCUMENT_CLAIMS = {
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
