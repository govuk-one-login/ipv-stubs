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
  driving_permit_expiry_date?: string;
  ci?: string[];
  nbf?: number;
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

interface DrivingPermitDetails {
  expiryDate: string;
  issueNumber: string;
  issuedBy: string;
  fullAddress: string;
  personalNumber: string;
  issueDate: string;
}

export interface DrivingPermitCredentialSubject extends BaseCredentialSubject {
  drivingPermit: DrivingPermitDetails[];
}

export interface PassportCredentialSubject extends BaseCredentialSubject {
  passport: object;
}

export function isDrivingPermitCredentialSubject(
  documentDetails: unknown,
): documentDetails is DrivingPermitCredentialSubject {
  return "drivingPermit" in (documentDetails as DrivingPermitCredentialSubject);
}

interface BaseCredentialSubject {
  name: object;
  birthDate: object;
}

export enum TestUser {
  kennethD = "kennethD",
}

export enum DocumentType {
  ukChippedPassport = "ukChippedPassport",
  drivingPermit = "drivingPermit",
}

export enum EvidenceType {
  success = "success",
  fail = "fail",
}
