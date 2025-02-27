export interface ManagementEnqueueVcRequest {
  user_id: string;
  queue_name?: string;
  delay_seconds?: number;
}

export function isManagementEnqueueVcRequest(
  request: unknown,
): request is ManagementEnqueueVcRequest {
  const underTest = request as ManagementEnqueueVcRequest;
  return underTest.user_id !== undefined;
}

export interface ManagementEnqueueVcRequestIndividualDetails
  extends ManagementEnqueueVcRequest {
  test_user: TestUser;
  document_type: DocumentType;
  evidence_type: EvidenceType;
  ci?: string[];
}

export function isManagementEnqueueVcRequestIndividualDetails(
  request: unknown,
): request is ManagementEnqueueVcRequestIndividualDetails {
  const underTest = request as ManagementEnqueueVcRequestIndividualDetails;
  return (
    isManagementEnqueueVcRequest(underTest) &&
    underTest.test_user !== undefined &&
    underTest.document_type !== undefined &&
    underTest.evidence_type !== undefined
  );
}

export interface ManagementEnqueueVcRequestEvidenceAndSubject
  extends ManagementEnqueueVcRequest {
  credential_subject: object;
  evidence: object;
  nbf?: number;
  mitigated_cis: {
    mitigatedCis: string[];
    cimitStubUrl: string;
    cimitStubApiKey: string;
  };
}

export function isManagementEnqueueVcRequestEvidenceAndSubject(
  request: unknown,
): request is ManagementEnqueueVcRequestEvidenceAndSubject {
  const underTest = request as ManagementEnqueueVcRequestEvidenceAndSubject;
  return (
    isManagementEnqueueVcRequest(underTest) &&
    underTest.evidence !== undefined &&
    underTest.credential_subject !== undefined
  );
}

export interface ManagementEnqueueErrorRequest {
  user_id: string;
  queue_name?: string;
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
}
