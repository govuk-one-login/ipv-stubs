import { APIGatewayProxyEventV2, APIGatewayProxyResultV2 } from "aws-lambda";
import { buildApiResponse } from "../common/apiResponse";
import getErrorMessage from "../common/errorReporting";
import { deleteState } from "../services/userStateService";

export async function handler(
  event: APIGatewayProxyEventV2,
): Promise<APIGatewayProxyResultV2> {
  try {
    if (event.body === undefined) {
      return buildApiResponse({ errorMessage: "No request body" }, 400);
    }

    const requestBody = JSON.parse(event.body);

    if (!requestBody.user_id) {
      return buildApiResponse(
        { errorMessage: "Missing user_id in request body" },
        400,
      );
    }

    await deleteState(requestBody.user_id);

    return buildApiResponse(
      {
        result: "success",
      },
      200,
    );
  } catch (error) {
    return buildApiResponse(
      { errorMessage: "Unexpected error: " + getErrorMessage(error) },
      500,
    );
  }
}
