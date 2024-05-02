
import VCMetadata from "../domain/vcMetadata";

export default interface EvcsVcItem {
  userId: string;
  vc: string;
  state: string;
  metadata: VCMetadata;
  provenance: string;
  ttl: number;
}