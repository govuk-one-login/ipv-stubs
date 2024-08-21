import { APIGatewayProxyEventV2, APIGatewayProxyResultV2 } from "aws-lambda";

import { buildApiResponse } from "../../common/apiResponses";
import TicfEvidenceItem from "../../domain/ticfEvidenceItem";
import { persistUserEvidence } from "../services/userEvidenceService";
import TicfManagementRequest from "../../domain/ticfManagementRequest";

const maxResponseDelaySeconds: number = 40;

export async function handler(
  event: APIGatewayProxyEventV2,
): Promise<APIGatewayProxyResultV2> {
  const userId = event.pathParameters?.userId;
  if (!userId) {
    return buildApiResponse({ errorMessage: "Missing userId." }, 400);
  }
  const statusCode = event.pathParameters?.statusCode ?? "200";
  let ticfManagementRequest: TicfManagementRequest;
  try {
    ticfManagementRequest = parseRequest(event);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } catch (error: any) {
    console.error(error);
    return buildApiResponse({ errorMessage: error.message }, 400);
  }

  try {
    await persistUserEvidence(
      decodeURIComponent(userId),
      ticfManagementRequest,
      parseInt(statusCode),
    );
    return buildApiResponse({ message: "Success !!" });
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } catch (error: any) {
    console.error(error);
    return buildApiResponse({ errorMessage: error.message }, 500);
  }
}

function parseRequest(event: APIGatewayProxyEventV2): TicfEvidenceItem {
  if (!event.pathParameters?.statusCode && !event.body) {
    throw new Error("Missing request body");
  }

  let ticfManagementRequest: TicfManagementRequest = {};
  if (event.body) {
    ticfManagementRequest = JSON.parse(event.body);

    if (ticfManagementRequest && !ticfManagementRequest.type) {
      throw new Error("Invalid request");
    }

    if (
      ticfManagementRequest.responseDelay &&
      ticfManagementRequest.responseDelay > maxResponseDelaySeconds
    ) {
      throw new Error(
        `Requested response delay (${ticfManagementRequest.responseDelay}) too large - must be less than ${maxResponseDelaySeconds}`,
      );
    }
  }

  return ticfManagementRequest;
}
