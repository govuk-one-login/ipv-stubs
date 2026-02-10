interface Evidence {
  contraIndicator: ContraIndicator[];
  type: "SecurityCheck";
}

export interface VcClaim {
  evidence: Evidence[];
  type: ["VerifiableCredential", "SecurityCheckCredential"];
}

interface MitigatingCredential {
  issuer: string;
  validFrom: string;
  txn: string;
  id: string;
}

interface Mitigation {
  code: string;
  mitigatingCredential: MitigatingCredential[];
}

export interface ContraIndicator {
  code: string;
  document: string | null;
  issuanceDate: string;
  issuers: string[];
  mitigation: Mitigation[];
  incompleteMitigation: Mitigation[];
  txn: string[];
}

export interface GetContraIndicatorCredentialRequest {
  userId: string;
  govukSigninJourneyId: string;
  ipAddress: string;
}

export interface GetContraIndicatorCredentialResponse {
  vc: string;
}

export interface CimitStubItem {
  userId: string;
  sortKey: string;
  contraIndicatorCode: string;
  issuer: string;
  issuanceDate: string;
  ttl: number;
  mitigations: string[];
  document: string;
  txn: string;
}
