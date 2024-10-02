import { APIGatewayProxyEventV2, APIGatewayProxyResultV2 } from "aws-lambda";

import { buildApiResponse } from "../../common/apiResponses";
import { persistUserEvidence } from "../services/userEvidenceService";
import TicfManagementRequest from "../../domain/ticfManagementRequest";
import { BadRequestError } from "../../domain/errors";

const maxResponseDelaySeconds: number = 40;

export async function handler(
  event: APIGatewayProxyEventV2,
): Promise<APIGatewayProxyResultV2> {
  const userId = event.pathParameters?.userId;
  if (!userId) {
    return buildApiResponse({ errorMessage: "Missing userId." }, 400);
  }

  try {
    const ticfManagementRequest = parseAndValidateRequest(event);

    await persistUserEvidence(
      decodeURIComponent(userId),
      ticfManagementRequest,
    );
    return buildApiResponse({ message: "Success!!" });

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } catch (error: any) {
    console.error(error);
    return buildApiResponse(
      { errorMessage: error.message },
      error.statusCode || 500,
    );
  }
}

function parseAndValidateRequest(
  event: APIGatewayProxyEventV2,
): TicfManagementRequest {
  if (!event.body) {
    throw new BadRequestError("Missing request body");
  }

  const ticfManagementRequest: TicfManagementRequest = JSON.parse(event.body);

  if (
    !ticfManagementRequest.evidence?.type &&
    (!ticfManagementRequest.statusCode || ticfManagementRequest.statusCode == 200)
  ) {
    throw new BadRequestError("Invalid request - evidence type required if configuring a successful TICF response");
  }

  if (
    ticfManagementRequest.responseDelay &&
    ticfManagementRequest.responseDelay > maxResponseDelaySeconds
  ) {
    throw new BadRequestError(
      `Requested response delay (${ticfManagementRequest.responseDelay}) too large - must be less than ${maxResponseDelaySeconds}`,
    );
  }

  return ticfManagementRequest;
}
