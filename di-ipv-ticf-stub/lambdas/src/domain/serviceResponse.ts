import TicfResponse from "../domain/ticfResponse";

export default interface ServiceResponse {
  response: TicfResponse;
  statusCode?: number;
}
