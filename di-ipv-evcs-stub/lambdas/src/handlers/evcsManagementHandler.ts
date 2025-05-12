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
      return buildApiResponse(
        {
          message: "Missing userId",
        },
        StatusCodes.InternalServerError,
      );
    }

    const decodedUserId = decodeURIComponent(userId);

    const res = await processGetStoredIdentity(decodedUserId);

    return buildApiResponse(
      res.statusCode === StatusCodes.NotFound
        ? { message: "No stored identity found for user" }
        : res.response,
      res.statusCode,
    );
  } catch (error) {
    console.error(error);
    return buildApiResponse(
      { message: "Unable to get stored identity for user" },
      StatusCodes.InternalServerError,
    );
  }
}
