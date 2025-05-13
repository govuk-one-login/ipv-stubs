import { VcDetails } from "./sharedTypes";
import EvcsStoredIdentityItem from "../model/storedIdentityItem";

export default interface ServiceResponse {
  response?: object;
  statusCode?: number;
}

type VcFromGetResponse = Omit<VcDetails, "provenance">;

export interface GetResponse {
  response: {
    vcs: VcFromGetResponse[];
  };
  statusCode: number;
}

export interface GetStoredIdentity {
  vcs: Omit<EvcsStoredIdentityItem, "metadata">[];
}
