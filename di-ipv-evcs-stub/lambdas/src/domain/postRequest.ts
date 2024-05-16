import { VcState } from "./enums/vcState";
import VCProvenance from "./enums/vcProvenance";

export default interface PostRequest {
  vc: string;
  state: VcState;
  metadata?: object;
  provenance?: VCProvenance;
}
[];
