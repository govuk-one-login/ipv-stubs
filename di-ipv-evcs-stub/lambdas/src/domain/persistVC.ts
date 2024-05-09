import VcState from "./enums/vcState";
import VCMetadata from "./vcMetadata";
import VCProvenance from "./enums/vcProvenance";

export default interface PersistVC {
  vc: string;
  state: VcState;
  metadata?: VCMetadata;
  provenance?: VCProvenance;
}
