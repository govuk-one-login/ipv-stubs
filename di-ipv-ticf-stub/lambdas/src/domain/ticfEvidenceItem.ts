export default interface TicfEvidenceItem {
  type: string;
  intervention?: {
    interventionCode: string;
    interventionReason?: string;
  };
  ci?: string[];
  txn?: string;
}
