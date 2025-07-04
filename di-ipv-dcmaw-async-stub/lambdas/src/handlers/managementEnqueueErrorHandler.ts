import { APIGatewayProxyEventV2, APIGatewayProxyResultV2 } from "aws-lambda";
import { buildApiResponse } from "../common/apiResponse";
import getErrorMessage from "../common/errorReporting";
import { ManagementEnqueueErrorRequest } from "../domain/managementEnqueueRequest";
import getConfig from "../common/config";
import { getUserStateItem } from "../services/userStateService";

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

    const { state, journeyId } = await getUserStateItem(requestBody.user_id);

    const queueMessage = {
      sub: requestBody.user_id,
      state,
      govuk_signin_journey_id: journeyId,
      error: requestBody.error_code,
      error_description:
        requestBody.error_description ?? "Error sent via DCMAW Async CRI stub",
    };

    const queueName = requestBody.queue_name ?? config.queueName;
    const delaySeconds = requestBody.delay_seconds ?? 0;

    const postResult = await fetch(config.queueStubUrl, {
      method: "POST",
      headers: { "x-api-key": config.queueStubApiKey },
      body: JSON.stringify({
        queueName: queueName,
        queueEvent: queueMessage,
        delaySeconds: delaySeconds,
      }),
    });

    if (!postResult.ok) {
      throw new Error(`Failed to enqueue VC: ${await postResult.text()}`);
    }

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
