import { APIGatewayProxyEvent, APIGatewayProxyResultV2 } from "aws-lambda";
import { buildApiResponse } from "../common/apiResponses";
import { getErrorMessage } from "../common/utils";
import { StatusCodes } from "../domain/enums";
import { processPatchUserVCsRequestV2 } from "../services/evcsService";
import {
  isPatchVcsRequest,
  PatchVcsRequest,
} from "../domain/requests/patchVcsRequest";

export async function updateHandler(
  event: APIGatewayProxyEvent,
): Promise<APIGatewayProxyResultV2> {
  console.info(`---Update V2 request received----`);

  let request;
  try {
    request = parseRequest(event.body);
  } catch (error) {
    console.error(error);
    return buildApiResponse(StatusCodes.BadRequest, {
      message: getErrorMessage(error),
    });
  }

  const res = await processPatchUserVCsRequestV2(request);
  return buildApiResponse(res.statusCode, res.response);
}

function parseRequest(requestBody: string | null): PatchVcsRequest {
  if (!requestBody) {
    throw new Error("Missing request body");
  }

  const patchRequest = JSON.parse(requestBody);
  if (!isPatchVcsRequest(patchRequest)) {
    throw new Error("Request body is not a PatchVcsRequest");
  }

  if (!patchRequest.userId) {
    throw new Error("User id is missing");
  }

  if (patchRequest.vcs.length === 0) {
    throw new Error("VCs are missing");
  }

  const statesBySignature = new Map<string, string>();
  for (const vc of patchRequest.vcs) {
    const existingState = statesBySignature.get(vc.signature);
    if (existingState !== undefined && existingState !== vc.state) {
      throw new Error(
        "Duplicate signature with conflicting states " + vc.signature,
      );
    }
    statesBySignature.set(vc.signature, vc.state);
  }

  return patchRequest;
}
