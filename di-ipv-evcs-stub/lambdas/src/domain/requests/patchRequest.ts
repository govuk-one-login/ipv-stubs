import { VcState } from "../enums";
import EvcsVcItem from "../../model/evcsVcItem";

export type EvcsItemForUpdate = Omit<EvcsVcItem, "vc">;

export interface PatchRequest {
  signature: string;
  state: VcState;
  metadata?: object;
}
