import { APIGatewayProxyEvent, APIGatewayProxyResultV2 } from "aws-lambda";
import { buildApiResponse } from "../common/apiResponses";
import {
  isPostVcsRequest,
  PostVcsRequest,
} from "../domain/requests/postVcsRequest";
import { processPostUserVCsRequestV2 } from "../services/evcsService";
import { getErrorMessage } from "../common/utils";
import { StatusCodes } from "../domain/enums";

export async function createHandler(
  event: APIGatewayProxyEvent,
): Promise<APIGatewayProxyResultV2> {
  console.info(`---Create V2 request received----`);

  let request;
  try {
    request = parseVcsRequest(event.body);
  } catch (error) {
    console.error(error);
    return buildApiResponse(StatusCodes.BadRequest, {
      message: getErrorMessage(error),
    });
  }

  const res = await processPostUserVCsRequestV2(request);
  return buildApiResponse(res.statusCode, res.response);
}

function parseVcsRequest(requestBody: string | null): PostVcsRequest {
  if (!requestBody) {
    throw new Error("Missing request body");
  }

  const postRequest = JSON.parse(requestBody);
  if (!isPostVcsRequest(postRequest)) {
    throw new Error("Request body is not a PostVcsRequest");
  }

  if (!postRequest.userId) {
    throw new Error("User id is missing");
  }

  if (postRequest.vcs.length === 0) {
    throw new Error("VCs are missing");
  }

  const statesByVc = new Map<string, string>();
  for (const vc of postRequest.vcs) {
    const existingState = statesByVc.get(vc.vc);
    if (existingState !== undefined && existingState !== vc.state) {
      throw new Error(
        "Duplicate VCs with different states present in request " + vc.vc,
      );
    }
    statesByVc.set(vc.vc, vc.state);
  }

  return postRequest;
}
