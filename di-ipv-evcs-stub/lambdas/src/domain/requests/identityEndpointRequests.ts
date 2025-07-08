import { VcDetails } from "../sharedTypes";
import { Vot } from "../enums/vot";

export interface StoredIdentityDetails {
  jwt: string;
  vot: Vot;
  metadata?: object;
}

export type PostIdentityRequest = Omit<PutRequest, "vcs" | "si"> & {
  si: StoredIdentityDetails;
};

export interface PutRequest {
  userId: string;
  vcs: VcDetails[];
  si?: StoredIdentityDetails;
}

export interface InvalidateIdentityRequest {
  userId: string;
}
