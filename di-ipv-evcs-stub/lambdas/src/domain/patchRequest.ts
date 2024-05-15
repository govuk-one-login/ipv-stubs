import { VcState } from "./enums/vcState";

export default interface PatchRequest {
  signature: string;
  state: VcState;
  metadata?: object;
}
[];
