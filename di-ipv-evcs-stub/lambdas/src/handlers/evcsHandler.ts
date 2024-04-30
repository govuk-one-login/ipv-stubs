import { APIGatewayProxyEventV2, APIGatewayProxyResultV2 } from "aws-lambda";
import { buildApiResponse } from "../common/apiResponses";
import PostRequest from "../domain/postRequest";
import { ApiResponse } from "../domain/apiResponse";
import { processPostUserVCsRequest } from "../services/evcsService";
import { v4 as uuid } from "uuid";

export async function handler(
  event: APIGatewayProxyEventV2
): Promise<APIGatewayProxyResultV2> {
  console.info(`---Request received----`);
  const userId = event.pathParameters?.userId;
  if (!userId) {
    return buildApiResponse({ errorMessage: "Missing userId." }, 400);
  }
  let request;
  try {
    request = parseRequest(event);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } catch (error: any) {
    console.error(error);
    return buildApiResponse({ errorMessage: error.message }, 400);
  }

  try {
    const response: ApiResponse = {
        messageId: uuid()
      }

      // switch(event.requestContext.http.method) {
      //   case "POST":
          await processPostUserVCsRequest(decodeURIComponent(userId), request);
      // }

    console.info(`Returning ${JSON.stringify(response)}`);
    return buildApiResponse(response, 202);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } catch (error: any) {
    console.error(error);
    return buildApiResponse({ errorMessage: error.message }, 500);
  }
}

function parseRequest(event: APIGatewayProxyEventV2): PostRequest {
  console.info(`---Request parsing----`);
  if (!event.body) {
    throw new Error("Missing request body");
  }

  const postRequest = JSON.parse(event.body);
  if (
    !postRequest ||
    !postRequest.persistVCs ||
    postRequest.persistVCs.length <= 0
  ) {
    throw new Error("Invalid request");
  }

  return postRequest;
}
