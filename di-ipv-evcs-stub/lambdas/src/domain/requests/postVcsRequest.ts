import { VcDetails, isVcDetails } from "../vcDetails";
import EvcsVcItem from "../../model/evcsVcItem";

export interface PostVcsRequest {
  userId: string;
  govuk_signin_journey_id: string;
  vcs: VcDetails[];
}

export type EvcsItemForUpdate = Omit<EvcsVcItem, "vc">;

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
