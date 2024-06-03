import { APIGatewayProxyEventV2, APIGatewayProxyResultV2 } from "aws-lambda";
import { buildApiResponse } from "../common/apiResponse";
import { Buffer } from 'buffer';
import getConfig from "../common/config";
import getErrorMessage from "../common/errorReporting";

export async function handler(
  event: APIGatewayProxyEventV2,
): Promise<APIGatewayProxyResultV2> {
  try {
    const config = await getConfig();

    const authorization = event?.headers?.Authorization || event?.headers?.authorization;

    if (!authorization || !authorization.startsWith('Basic')) {
      return buildApiResponse({ errorMessage: "Invalid credentials" }, 401);
    }

    const base64Credentials = authorization.split(' ')[1];
    const decodedCredentials = Buffer.from(base64Credentials, 'base64').toString('ascii');
    const [username, password] = decodedCredentials.split(':');
    if (
      username === undefined ||
      password === undefined ||
      username !== config.dummyClientId ||
      password !== config.dummySecret
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
