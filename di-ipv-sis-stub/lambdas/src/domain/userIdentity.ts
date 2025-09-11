import { JWTPayload } from "jose";

export interface UserIdentity {
  content: string;
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
  claims?: object;
  vot?: string;
  credentials?: string[];
  sub: string;
  aud: string;
}
