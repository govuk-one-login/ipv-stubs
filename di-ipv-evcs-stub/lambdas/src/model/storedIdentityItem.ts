export default interface EvcsStoredIdentityItem {
  userId: string;
  jwtSignature: string;
  storedIdentity: string;
  levelOfConfidence: string;
  isValid: boolean;
  metadata?: object;
}
