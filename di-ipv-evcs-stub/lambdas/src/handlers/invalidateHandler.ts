import { APIGatewayProxyEvent, APIGatewayProxyResultV2 } from "aws-lambda";
import { buildApiResponse } from "../common/apiResponses";
import { invalidateUserSi } from "../services/evcsService";
import { getErrorMessage } from "../common/utils";
import { StatusCodes } from "../domain/enums";
import {
  InvalidateRequest,
  isInvalidateRequest,
} from "../domain/requests/invalidateRequest";

export async function invalidateStoredIdentityHandler(
  event: APIGatewayProxyEvent,
): Promise<APIGatewayProxyResultV2> {
  console.info(`---POST invalidate stored identity request received----`);

  let parsedInvalidateSiRequest;
  try {
    parsedInvalidateSiRequest = parseInvalidateIdentityRequest(event.body);
  } catch (error) {
    console.error(error);
    return buildApiResponse(StatusCodes.BadRequest, {
      message: getErrorMessage(error),
    });
  }

  const res = await invalidateUserSi(parsedInvalidateSiRequest.userId);

  return buildApiResponse(res.statusCode);
}

function parseInvalidateIdentityRequest(
  requestBody: string | null,
): InvalidateRequest {
  if (!requestBody) {
    throw new Error("Missing request body");
  }

  const invalidateRequest = JSON.parse(requestBody);
  if (!isInvalidateRequest(invalidateRequest)) {
    throw new Error("Request body is not a PostVcsRequest");
  }

  if (!invalidateRequest.userId) {
    throw new Error("User id is missing");
  }

  return invalidateRequest;
}
