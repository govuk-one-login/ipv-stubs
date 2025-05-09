import { APIGatewayProxyEventV2, APIGatewayProxyResultV2 } from "aws-lambda";

import { buildApiResponse } from "../utils/response";
import persistFutureAisResponse from "../utils/dataLayer";
import { UserManagementRequest } from "../utils/types";
import BadRequestError from "../errors/BadRequestError";

const maxResponseDelaySeconds: number = 40;

export async function handler(
  event: APIGatewayProxyEventV2,
): Promise<APIGatewayProxyResultV2> {
  const userId = event.pathParameters?.userId;
  if (!userId) {
    return buildApiResponse({ errorMessage: "Missing userId." }, 400);
  }

  try {
    const userManagementRequest = parseAndValidateRequest(event);

    // Store response in DynamoDB
    await persistFutureAisResponse(
      decodeURIComponent(userId),
      userManagementRequest,
    );

    return buildApiResponse({ message: "Success" });
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
): UserManagementRequest {
  if (!event.body) {
    throw new BadRequestError("Missing request body");
  }

  const body: UserManagementRequest = JSON.parse(event.body);
  if (!body.intervention) {
    throw new BadRequestError(
      "Invalid request - intervention must be supplied to build a response",
    );
  }

  if (body.responseDelay && body.responseDelay > maxResponseDelaySeconds) {
    throw new BadRequestError(
      `Requested response delay (${body.responseDelay}) too large - must be less than ${maxResponseDelaySeconds}`,
    );
  }

  return body;
}
