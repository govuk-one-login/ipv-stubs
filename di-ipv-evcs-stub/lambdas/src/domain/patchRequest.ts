import { VcState } from "./enums/vcState";
import VCMetadata from "./vcMetadata";

export default interface PatchRequest {
  signature: string;
  state: VcState;
  metadata?: VCMetadata;
}
[];
