import { JWTPayload } from "jose";

export interface UserIdentity {
  content?: StoredIdentityContents;
  isValid: boolean;
  expired: boolean;
  vot: string;
  kidValid: true;
  signatureValid: true;
}

export interface UserIdentityRequestBody {
  govukSigninJourneyId: string;
  vtr: string[];
}

export interface StoredIdentityJwt extends JWTPayload {
  claims?: IdentityClaims;
  vot?: string;
  credentials: string[];
  sub: string;
  aud: string;
}

interface IdentityClaims {
  "https://vocab.account.gov.uk/v1/coreIdentity"?: object;
  "https://vocab.account.gov.uk/v1/address"?: object[];
  "https://vocab.account.gov.uk/v1/passport"?: object[];
  "https://vocab.account.gov.uk/v1/drivingPermit"?: object[];
  "https://vocab.account.gov.uk/v1/socialSecurityRecord"?: object[];
}

export interface StoredIdentityContents {
  sub: string;
  vot: string;
  vtm: string;
  credentials: string[];
  "https://vocab.account.gov.uk/v1/credentialJWT": string[];
  "https://vocab.account.gov.uk/v1/coreIdentity"?: object;
  "https://vocab.account.gov.uk/v1/address"?: object[];
  "https://vocab.account.gov.uk/v1/passport"?: object[];
  "https://vocab.account.gov.uk/v1/drivingPermit"?: object[];
  "https://vocab.account.gov.uk/v1/socialSecurityRecord"?: object[];
}
