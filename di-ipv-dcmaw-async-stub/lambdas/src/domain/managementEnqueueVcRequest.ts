
export interface ManagementEnqueueVcRequest {
  user_id: string;
  test_user: TestUser;
  document_type: DocumentType;
  evidence_type: EvidenceType;
  ci: string[];
}

export enum TestUser {
  kennethD
}

export enum DocumentType {
  ukChippedPassport
}

export enum EvidenceType {
  success,
  fail,
}
