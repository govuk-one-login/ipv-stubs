import { VcDetails, isVcDetails } from "../vcDetails";

export interface PostVcsRequest {
  userId: string;
  govuk_signin_journey_id: string;
  vcs: VcDetails[];
}

export function isPostVcsRequest(value: unknown): value is PostVcsRequest {
  if (typeof value !== "object" || value === null) return false;
  const obj = value as Record<string, unknown>;
  return (
    typeof obj.userId === "string" &&
    typeof obj.govuk_signin_journey_id === "string" &&
    Array.isArray(obj.vcs) &&
    obj.vcs.every(isVcDetails)
  );
}
