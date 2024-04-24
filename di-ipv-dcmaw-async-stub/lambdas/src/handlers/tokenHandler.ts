import { APIGatewayProxyEventV2, APIGatewayProxyResultV2 } from "aws-lambda";
import { buildApiResponse } from "../common/apiResponse";
import auth from "basic-auth";
import getConfig from "../common/config";
import getErrorMessage from "../common/errorReporting";

export async function handler(
  event: APIGatewayProxyEventV2,
): Promise<APIGatewayProxyResultV2> {
  try {
    const config = await getConfig();

    // Check authorization
    const credentials = auth(event);
    if (
      credentials === undefined ||
      credentials.name !== config.dummyClientId ||
      credentials.pass !== config.dummySecret
    ) {
      return buildApiResponse({ errorMessage: "Invalid credentials" }, 401);
    }

    if (!event.body?.includes("grant_type=client_credentials")) {
      return buildApiResponse(
        { errorMessage: "Invalid request body: " + event.body },
        400,
      );
    }

    return buildApiResponse(
      {
        access_token: config.dummyAccessTokenValue,
        token_type: "Bearer",
        expires_in: config.tokenLifetimeSeconds,
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
