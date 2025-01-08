export interface ManagementEnqueueVcRequest {
  user_id: string;
  test_user: TestUser;
  document_type: DocumentType;
  evidence_type: EvidenceType;
  ci?: string[];
  delay_seconds?: number;
  queue_name?: string;
}

export interface ManagementEnqueueErrorRequest {
  user_id: string;
  error_code: string;
  error_description?: string;
  delay_seconds?: number;
}

export enum TestUser {
  kennethD = "kennethD",
}

export enum DocumentType {
  ukChippedPassport = "ukChippedPassport",
}

export enum EvidenceType {
  success = "success",
  fail = "fail",
  failWithCi = "failWithCi",
}
