import { VcState } from "./enums";

export interface VcPatchDetails {
  state: VcState;
  metadata?: object;
  signature: string;
}

export function isVcPatchDetails(value: unknown): value is VcPatchDetails {
  if (typeof value !== "object" || value === null) return false;
  const obj = value as Record<string, unknown>;
  return (
    typeof obj.signature === "string" &&
    Object.values(VcState).includes(obj.state as VcState) &&
    (obj.metadata === undefined || typeof obj.metadata === "object")
  );
}
