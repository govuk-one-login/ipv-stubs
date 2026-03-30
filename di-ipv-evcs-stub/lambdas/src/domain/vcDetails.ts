import { VCProvenance, VcState } from "./enums";

export interface VcDetails {
  vc: string;
  state: VcState;
  metadata?: object;
  provenance?: VCProvenance;
}
