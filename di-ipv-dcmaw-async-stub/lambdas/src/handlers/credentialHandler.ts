import { APIGatewayProxyEventV2, APIGatewayProxyResultV2 } from "aws-lambda";
import { buildApiResponse } from "../common/apiResponse";
import getConfig from "../common/config";
import getErrorMessage from "../common/errorReporting";
import CredentialRequest from "../domain/credentialRequest";

export async function handler(
  event: APIGatewayProxyEventV2,
): Promise<APIGatewayProxyResultV2> {
  try {
    const config = await getConfig();

    // Check the access token
    const access_token = getAccessToken(event);

    if (
      access_token === undefined ||
      access_token !== config.dummyAccessTokenValue
    ) {
      return buildApiResponse({ errorMessage: "Invalid credentials" }, 401);
    }

    if (event.body === undefined) {
      return buildApiResponse({ errorMessage: "No request body" }, 400);
    }

    const requestBody = parseRequest(event);
    if (typeof requestBody === "string") {
      return buildApiResponse({ errorMessage: requestBody }, 400);
    }

    return buildApiResponse(
      {
        sub: requestBody.sub,
        "https://vocab.account.gov.uk/v1/credentialStatus": "pending",
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

function getAccessToken(event: APIGatewayProxyEventV2): string | undefined {
  const authHeaderValue = event?.headers?.authorization?.trim();

  if (authHeaderValue === undefined) {
    return undefined;
  }

  if (!authHeaderValue.startsWith("Bearer")) {
    return undefined;
  }

  return authHeaderValue.substring("Bearer".length).trim();
}

// Returns an error message or the parsed request
function parseRequest(
  event: APIGatewayProxyEventV2,
): string | CredentialRequest {
  if (event.body === undefined) {
    return "No request body";
  }

  const requestBody = JSON.parse(event.body);

  // Sadly there isn't a built-in method in TypeScript of checking whether an object matches an interface
  const mandatoryFields = [
    "sub",
    "govuk_signin_journey_id",
    "client_id",
    "state",
  ];
  const missingFields = mandatoryFields.filter(
    (field) => requestBody[field] === undefined,
  );

  if (missingFields.length > 0) {
    return "Request body is missing fields: " + missingFields.join(", ");
  }

  return requestBody as CredentialRequest;
}
