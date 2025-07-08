import { APIGatewayProxyEvent, APIGatewayProxyResultV2 } from "aws-lambda";
import { buildApiResponse } from "../common/apiResponses";
import { StatusCodes } from "../domain/enums";
import {
  CreateStoredIdentityRequest,
  processCreateStoredIdentity,
  processGetStoredIdentity,
} from "../services/evcsManagementService";
import { getErrorMessage } from "../common/utils";

export async function getUserStoredIdentityHandler(
  event: APIGatewayProxyEvent,
): Promise<APIGatewayProxyResultV2> {
  console.info("---Get user stored identity request received---");
  try {
    const userId = event.pathParameters?.userId;

    if (!userId) {
      return buildApiResponse(StatusCodes.BadRequest, {
        message: "Missing userId",
      });
    }

    const decodedUserId = decodeURIComponent(userId);

    const res = await processGetStoredIdentity(decodedUserId);

    if (res.storedIdentities.length === 0) {
      return buildApiResponse(StatusCodes.NotFound, {
        message: "No stored identity found for user",
      });
    }

    return buildApiResponse(StatusCodes.Success, res.storedIdentities);
  } catch (error) {
    console.error(error);
    return buildApiResponse(StatusCodes.InternalServerError, {
      message: "Unable to get stored identity for user",
    });
  }
}

export async function createUserStoredIdentityHandler(
  event: APIGatewayProxyEvent,
): Promise<APIGatewayProxyResultV2> {
  console.info("---Create user stored identity request received---");

  let parsedRequest;
  try {
    parsedRequest = parseCreateSiRequest(event);
  } catch (error) {
    console.error(error);
    return buildApiResponse(StatusCodes.BadRequest, {
      message: getErrorMessage(error),
    });
  }

  const res = await processCreateStoredIdentity(parsedRequest);

  return buildApiResponse(res.statusCode, res.response);
}

function parseCreateSiRequest(
  event: APIGatewayProxyEvent,
): CreateStoredIdentityRequest {
  const userId = event.pathParameters?.userId;

  if (!userId) {
    throw new Error("Missing user id");
  }

  const decodedUserId = decodeURIComponent(userId);

  if (!event.body) {
    throw new Error("Missing request body");
  }

  const parsedBody = JSON.parse(event.body);
  if (!parsedBody.si.jwt) {
    throw new Error("Missing si.jwt");
  }

  if (!parsedBody.si.vot) {
    throw new Error("Missing si.vot");
  }

  return {
    userId: decodedUserId,
    si: parsedBody.si,
  };
}
