import { APIGatewayProxyEventV2, APIGatewayProxyResultV2 } from "aws-lambda";

import { buildApiResponse } from "../../common/apiResponses";
import TicfEvidenceItem from "../../domain/ticfEvidenceItem";
import { persistUserEvidence } from "../services/userEvidenceService";

export async function handler(
  event: APIGatewayProxyEventV2
): Promise<APIGatewayProxyResultV2> {
  const userId = event.pathParameters?.userId;
  if (!userId) {
    return buildApiResponse({ errorMessage: "Missing userId." }, 400);
  }
  const statusCode = event.pathParameters?.statusCode ?? '200';
  let ticfEvidenceItemReq: TicfEvidenceItem;
  try {
    ticfEvidenceItemReq = parseRequest(event);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } catch (error: any) {
    console.error(error);
    return buildApiResponse({ errorMessage: error.message }, 400);
  }

  try {
    await persistUserEvidence(decodeURIComponent(userId), ticfEvidenceItemReq, parseInt(statusCode));
    return buildApiResponse({ message: "Success !!" });
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } catch (error: any) {
    console.error(error);
    return buildApiResponse({ errorMessage: error.message }, 500);
  }
}

function parseRequest(event: APIGatewayProxyEventV2): TicfEvidenceItem {
  let ticfEvidenceItemReq: TicfEvidenceItem = {};
    if (!event.pathParameters?.statusCode && !event.body) {
      throw new Error("Missing request body");
    }

    if (event.body) {
      ticfEvidenceItemReq = JSON.parse(event.body);

      if (ticfEvidenceItemReq && !ticfEvidenceItemReq.type) {
        throw new Error("Invalid request");
      }
    }

  return ticfEvidenceItemReq;
}
