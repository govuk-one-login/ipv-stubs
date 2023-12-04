import TicfEvidenceItem from "../../domain/ticfEvidenceItem";

export default interface UserEvidenceItem {
  userId: string;
  evidence: TicfEvidenceItem;
  ttl: number;
}
