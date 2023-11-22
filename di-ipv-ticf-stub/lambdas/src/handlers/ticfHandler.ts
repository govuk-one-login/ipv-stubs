import { Handler } from "aws-lambda";

import { apiResponses } from "../common/apiResponses";
import TicfRequest from "../domain/ticfRequest";
import TicfResponse from "../domain/ticfResponse";
import ticfService from "../services/ticfService";

export const handler: Handler = async (event: any) => {
  let ticfRequest: TicfRequest;
  try {
    ticfRequest = JSON.parse(event.body);
  } catch (error) {
    return apiResponses._400();
  }
  if (ticfRequest == null || ticfRequest.vot == null)
    return apiResponses._400({ errorMessage: "Pls. pass proper request." });

  // Process and get response
  let responseBody: TicfResponse | undefined;
  try {
    responseBody = await ticfService.processGetVCRequest(ticfRequest);
  } catch (error) {
    if (error instanceof Error) {
      return apiResponses._500({ errorMessage: error.message });
    }
  }
  if (!responseBody)
    return apiResponses._500({
      errorMessage: "Failed while processing request.",
    });

  return apiResponses._200(responseBody);
};
