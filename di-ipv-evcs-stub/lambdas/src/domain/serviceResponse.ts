import { VcDetails } from "./vcDetails";
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
  storedIdentities: Omit<EvcsStoredIdentityItem, "metadata">[];
}
