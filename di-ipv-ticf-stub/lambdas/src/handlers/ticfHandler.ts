import { APIGatewayProxyEventV2, APIGatewayProxyResultV2 } from "aws-lambda";
import { buildApiResponse } from "../common/apiResponses";
import TicfRequest from "../domain/ticfRequest";
import { processGetVCRequest } from "../services/ticfService";
import { BadRequestError } from "../domain/errors";

export async function handler(
  event: APIGatewayProxyEventV2,
): Promise<APIGatewayProxyResultV2> {
  try {
    const ticfRequest = parseRequest(event);
    const { response, statusCode } = await processGetVCRequest(ticfRequest);

    return buildApiResponse(response, statusCode);
  } catch (error: any) { // eslint-disable-line @typescript-eslint/no-explicit-any
    console.error(error);
    return buildApiResponse({ errorMessage: error.message }, error.statusCode || 500);
  }
}

function parseRequest(event: APIGatewayProxyEventV2): TicfRequest {
  if (!event.body) {
    throw new BadRequestError("Missing request body");
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
    throw new BadRequestError("Invalid request");
  }

  return ticfRequest;
}
