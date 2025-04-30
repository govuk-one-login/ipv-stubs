import { VcDetails } from "./sharedTypes";

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
