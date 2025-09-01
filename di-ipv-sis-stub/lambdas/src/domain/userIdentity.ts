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
