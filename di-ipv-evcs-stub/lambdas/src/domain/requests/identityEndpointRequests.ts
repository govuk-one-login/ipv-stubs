import { VcDetails } from "../sharedTypes";
import { Vot } from "../enums/vot";

export interface StoredIdentityDetails {
  jwt: string;
  vot: Vot;
  metadata?: object;
  expired?: boolean; // For use through the management API
}

export type PostIdentityRequest = {
  userId: string;
  govuk_signin_journey_id?: string;
  vcs?: VcDetails[];
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
