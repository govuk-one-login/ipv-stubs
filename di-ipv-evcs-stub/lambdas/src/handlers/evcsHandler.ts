import { APIGatewayProxyEvent, APIGatewayProxyResultV2 } from "aws-lambda";
import { buildApiResponse } from "../common/apiResponses";
import PostRequest from "../domain/postRequest";
import ServiceResponse from "../domain/serviceResponse";
import PersistVC from "../domain/persistVC";
import { CreateVcStates } from  "../domain/enums/vcState";

import { processPostUserVCsRequest } from "../services/evcsService";
import { processGetUserVCsRequest } from "../services/evcsService";

export async function handler(
  event: APIGatewayProxyEvent
): Promise<APIGatewayProxyResultV2> {
  console.info(`---Request received----`);
  const userId = event.pathParameters?.userId;
  if (!userId) {
    return buildApiResponse({ errorMessage: "Missing userId." }, 400);
  }

  try {
    let request;
    let res: ServiceResponse = {
      response: Object
    };
    switch(event.httpMethod) {
      case "POST":
        try {
          request = parsePostRequest(event);
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } catch (error: any) {
          console.error(error);
          return buildApiResponse({ errorMessage: error.message }, 400);
        }
        // eslint-disable-next-line no-case-declarations
        res = await processPostUserVCsRequest(decodeURIComponent(userId), request);
        break;
      case "GET":
        // eslint-disable-next-line no-case-declarations
        res = await processGetUserVCsRequest(decodeURIComponent(userId));
        break;
      default:
        console.info(`Not received correct request http method.`);
    }

    return buildApiResponse(res.response, res.statusCode);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } catch (error: any) {
    console.error(error);
    return buildApiResponse({ errorMessage: error.message }, 500);
  }
}

function parsePostRequest(event: APIGatewayProxyEvent): PostRequest {
  console.info(`---Request parsing----`);
  if (!event.body) {
    throw new Error("Missing request body");
  }

  const postRequest = JSON.parse(event.body);
  if (
    !postRequest ||
    !postRequest.persistVCs ||
    postRequest.persistVCs.length <= 0 ||
    ! isValidCreateVcState(postRequest.persistVCs)
  ) {
    throw new Error("Invalid request");
  }

  return postRequest;
}

function isValidCreateVcState(persistVCs: PersistVC[]): boolean {
  for (const persistVC of persistVCs) {
    if (! (persistVC.state in CreateVcStates)) {
      return false;
    }
  }
  return true;
}
