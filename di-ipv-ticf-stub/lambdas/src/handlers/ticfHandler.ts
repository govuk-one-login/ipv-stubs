import { APIGatewayProxyEvent, APIGatewayProxyResult } from "aws-lambda";

import { apiResponses } from "../common/apiResponses";
import TicfRequest from "../domain/ticfRequest";
import TicfResponse from "../domain/ticfResponse";
import ticfService from "../services/ticfService";

export const handler = async (
  event: APIGatewayProxyEvent
): Promise<APIGatewayProxyResult> => {
  let ticfRequest: TicfRequest | undefined;
  try {
    if (event.body == null) {
      throw new Error(`Pls. pass proper request.`);
    }
    ticfRequest = JSON.parse(event.body);
  } catch (error) {
    console.info(`>>> Parse issue: ${error}`);
    if (error instanceof Error) {
      return apiResponses._400({ errorMessage: error.message });
    }
  }
  if (!ticfRequest || ticfRequest == null || ticfRequest.vot == null)
    return apiResponses._400({ errorMessage: "Pls. pass proper request with vot." });

  // Process and get response
  let responseBody: TicfResponse | undefined;
  try {
    responseBody = await ticfService.processGetVCRequest(ticfRequest);
  } catch (error) {
    console.error(error);
    if (error instanceof Error) {
      console.info(`>>> Error from service: ${error}`);
      return apiResponses._500({ errorMessage: error.message });
    }
  }
  if (!responseBody)
    return apiResponses._500({
      errorMessage: "Failed while processing request.",
    });

  return apiResponses._200(responseBody);
};
