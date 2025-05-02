export default interface EvcsVcItem {
  userId: string;
  vcSignature: string;
  vc: string;
  state: string;
  metadata?: object;
  provenance?: string;
  ttl: number;
}
