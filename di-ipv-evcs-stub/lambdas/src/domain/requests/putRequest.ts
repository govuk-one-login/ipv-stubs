import { VcDetails } from "../sharedTypes";

interface StoredIdentityDetails {
  jwt: string;
  vot: string;
  metadata?: object;
}

export interface PutRequest {
  userId: string;
  vcs: VcDetails[];
  si?: StoredIdentityDetails;
}
