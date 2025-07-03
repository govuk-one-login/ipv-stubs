import { APIGatewayProxyEvent, APIGatewayProxyResultV2 } from "aws-lambda";
import { buildApiResponse } from "../common/apiResponses";
import { StatusCodes } from "../domain/enums";
import { processGetStoredIdentity } from "../services/evcsManagementService";

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
