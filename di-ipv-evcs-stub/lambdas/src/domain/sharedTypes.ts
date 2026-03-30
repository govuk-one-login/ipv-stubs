export type StoredIdentityResponse = {
  si: Omit<VCIncludingStateAndMetadata, "state">;
  vcs: Omit<VCIncludingStateAndMetadata, "unsignedVot">[];
};

export interface VCIncludingStateAndMetadata {
  state?: string;
  vc: string;
  metadata?: Record<string, unknown>;
  signature?: string;
  unsignedVot?: string;
}
