import { VcState } from "./enums/vcState";
import VCProvenance from "./enums/vcProvenance";

export interface VcDetails {
  vc: string;
  state: VcState;
  metadata?: object;
  provenance?: VCProvenance;
}
