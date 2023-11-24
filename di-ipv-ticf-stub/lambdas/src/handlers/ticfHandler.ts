import { APIGatewayProxyEvent, APIGatewayProxyResult } from "aws-lambda";
import { apiResponses } from "../common/apiResponses";
import TicfRequest from "../domain/ticfRequest";
import TicfResponse from "../domain/ticfResponse";
import { processGetVCRequest } from "../services/ticfService";

export const handler = async (
  event: APIGatewayProxyEvent
): Promise<APIGatewayProxyResult> => {
  let ticfRequest: TicfRequest | undefined;
  try {
    if (event.body === null) {
      throw new Error(`Pls. pass proper request.`);
    }
    ticfRequest = JSON.parse(event.body);
  } catch (error) {
    if (error instanceof Error) {
      return apiResponses._400({ errorMessage: error.message });
    }
  }
  if (
    !ticfRequest ||
    !ticfRequest.sub ||
    !ticfRequest.vot ||
    !ticfRequest.vtr ||
    !ticfRequest.vtm ||
    !ticfRequest.govuk_signin_journey_id
  )
    return apiResponses._400({ errorMessage: "Pls. pass proper request." });

  // Process and get response
  let responseBody: TicfResponse;
  try {
    responseBody = await processGetVCRequest(ticfRequest);
    return apiResponses._200(responseBody);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } catch (error: any) {
    return apiResponses._500({ errorMessage: error.message });
  }
};
