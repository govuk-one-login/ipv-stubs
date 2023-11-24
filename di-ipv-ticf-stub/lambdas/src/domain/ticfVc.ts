import { JWTPayload } from "jose";
import TicfEvidenceItem from "./ticfEvidenceItem";

export default interface TicfVc extends JWTPayload {
  vc: {
    type: string[];
    evidence: TicfEvidenceItem[];
  }
}
