import { VCProvenance, VcState } from "./enums";

export interface VcDetails {
  vc: string;
  state: VcState;
  metadata?: object;
  provenance?: VCProvenance;
}

export function isVcDetails(value: unknown): value is VcDetails {
  if (typeof value !== "object" || value === null) return false;
  const obj = value as Record<string, unknown>;
  return (
    typeof obj.vc === "string" &&
    Object.values(VcState).includes(obj.state as VcState) &&
    (obj.metadata === undefined || typeof obj.metadata === "object") &&
    (obj.provenance === undefined ||
      Object.values(VCProvenance).includes(obj.provenance as VCProvenance))
  );
}
