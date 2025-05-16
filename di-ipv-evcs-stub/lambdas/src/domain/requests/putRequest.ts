import { VcDetails } from "../sharedTypes";
import { Vot } from "../enums/vot";

interface StoredIdentityDetails {
  jwt: string;
  vot: Vot;
  metadata?: object;
}

export interface PutRequest {
  userId: string;
  vcs: VcDetails[];
  si?: StoredIdentityDetails;
}
