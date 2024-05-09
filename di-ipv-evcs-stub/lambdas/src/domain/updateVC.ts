import VcState from "./enums/vcState";
import VCMetadata from "./vcMetadata";

export default interface UpdateVC {
  signature: string;
  state: VcState;
  metadata?: VCMetadata;
}
