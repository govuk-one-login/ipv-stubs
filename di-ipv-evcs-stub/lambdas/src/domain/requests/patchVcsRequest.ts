import { VcPatchDetails, isVcPatchDetails } from "../vcPatchDetails";

export interface PatchVcsRequest {
  userId: string;
  govuk_signin_journey_id: string;
  vcs: VcPatchDetails[];
}

export function isPatchVcsRequest(value: unknown): value is PatchVcsRequest {
  if (typeof value !== "object" || value === null) return false;
  const obj = value as Record<string, unknown>;
  return (
    typeof obj.userId === "string" &&
    typeof obj.govuk_signin_journey_id === "string" &&
    Array.isArray(obj.vcs) &&
    obj.vcs.every(isVcPatchDetails)
  );
}
