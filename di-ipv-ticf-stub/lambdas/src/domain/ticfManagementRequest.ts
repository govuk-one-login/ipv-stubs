import TicfEvidenceItem from "./ticfEvidenceItem";

export default interface TicfManagementRequest {
  evidence?: TicfEvidenceItem;
  responseDelay?: number;
  statusCode?: number;
}
