export default interface EvcsStoredIdentityItem {
  userId: string;
  recordType: string;
  storedIdentity: string;
  levelOfConfidence: string;
  isValid: boolean;
  metadata?: object;
}
