
import VCMetadata from "../domain/vcMetadata";

export default interface EvcsVcItem {
  userId: string;
  vc: string;
  vcSignature: string;
  state: string;
  metadata: VCMetadata;
  provenance: string;
  ttl: number;
}