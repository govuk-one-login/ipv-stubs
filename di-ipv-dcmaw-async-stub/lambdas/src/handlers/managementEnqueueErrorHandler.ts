import { APIGatewayProxyEventV2, APIGatewayProxyResultV2 } from "aws-lambda";
import { buildApiResponse } from "../common/apiResponse";
import getErrorMessage from "../common/errorReporting";
import { ManagementEnqueueErrorRequest } from "../domain/managementEnqueueRequest";
import getConfig from "../common/config";
import { getState } from "../services/userStateService";

export async function handler(
  event: APIGatewayProxyEventV2,
): Promise<APIGatewayProxyResultV2> {
  try {
    const config = await getConfig();

    if (event.body === undefined) {
      return buildApiResponse({ errorMessage: "No request body" }, 400);
    }

    const requestBody = parseRequest(event);
    if (typeof requestBody === "string") {
      return buildApiResponse({ errorMessage: requestBody }, 400);
    }

    const state = await getState(requestBody.user_id);

    const queueMessage = {
      sub: requestBody.user_id,
      state,
      error: requestBody.error_code,
      error_description:
        requestBody.error_description ?? "Error sent via DCMAW Async CRI stub",
    };

    await fetch(config.queueStubUrl, {
      method: "POST",
      headers: { "x-api-key": config.queueStubApiKey },
      body: JSON.stringify({
        queueName: config.queueName,
        queueEvent: queueMessage,
        delaySeconds: requestBody.delay_seconds ?? 0,
      }),
    });

    return buildApiResponse(
      {
        result: "success",
      },
      201,
    );
  } catch (error) {
    return buildApiResponse(
      { errorMessage: "Unexpected error: " + getErrorMessage(error) },
      500,
    );
  }
}

function parseRequest(
  event: APIGatewayProxyEventV2,
): string | ManagementEnqueueErrorRequest {
  if (event.body === undefined) {
    return "No request body";
  }

  const requestBody = JSON.parse(event.body);

  const mandatoryFields = ["user_id", "error_code"];
  const missingFields = mandatoryFields.filter(
    (field) => requestBody[field] === undefined,
  );

  if (missingFields.length > 0) {
    return "Request body is missing fields: " + missingFields.join(", ");
  }

  return requestBody as ManagementEnqueueErrorRequest;
}
