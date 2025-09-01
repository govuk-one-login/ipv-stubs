import { VcState, VCProvenance } from "./enums";

export interface VcDetails {
  vc: string;
  state: VcState;
  metadata?: object;
  provenance?: VCProvenance;
}

export type StoredIdentityResponse = {
  si: Omit<VCIncludingStateAndMetadata, "state">;
  vcs: VCIncludingStateAndMetadata[];
};

export interface VCIncludingStateAndMetadata {
  state?: string;
  vc: string;
  metadata?: Record<string, unknown>;
  signature?: string;
}
