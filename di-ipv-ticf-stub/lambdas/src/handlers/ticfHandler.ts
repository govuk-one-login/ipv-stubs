import { APIGatewayProxyEventV2, APIGatewayProxyResultV2 } from "aws-lambda";
import { buildApiResponse } from "../common/apiResponses";
import TicfRequest from "../domain/ticfRequest";
import TicfResponse from "../domain/ticfResponse";
import { processGetVCRequest } from "../services/ticfService";

export async function handler(event: APIGatewayProxyEventV2): Promise<APIGatewayProxyResultV2> {
  let ticfRequest;
  try {
    ticfRequest = parseRequest(event);
  } catch (error: any) { // eslint-disable-line @typescript-eslint/no-explicit-any
    console.error(error);
    return buildApiResponse({ errorMessage: error.message }, 400);
  }

  try {
    const response: TicfResponse = await processGetVCRequest(ticfRequest);
    console.info(`Returning ${JSON.stringify(response)}`);
    return buildApiResponse(response);
  } catch (error: any) { // eslint-disable-line @typescript-eslint/no-explicit-any
    console.error(error);
    return buildApiResponse({ errorMessage: error.message }, 500);
  }
}

function parseRequest(event: APIGatewayProxyEventV2): TicfRequest {
  if (!event.body) {
    throw new Error('Missing request body');
  }

  const ticfRequest = JSON.parse(event.body);

  if (
    !ticfRequest ||
    !ticfRequest.sub ||
    !ticfRequest.vot ||
    !ticfRequest.vtr ||
    !ticfRequest.vtm ||
    !ticfRequest.govuk_signin_journey_id
  ) {
    throw new Error('Invalid request');
  }

  return ticfRequest;
}