import { APIGatewayProxyEvent, APIGatewayProxyResult } from "aws-lambda";
import {
  addUserCis,
  updateUserCis,
  UserCisRequest,
} from "./service/userService";
import * as cimitStubItemService from "../../common/cimitStubItemService";
import * as pendingMitigationService from "../../common/pendingMitigationService";
import * as preMitigationService from "../../common/preMitigationService";
import { UserMitigationRequest } from "../../common/pendingMitigationService";
import { UserPreMitigationRequest } from "../../common/preMitigationService";

const CIS_PATTERN = /^\/user\/[-a-zA-Z0-9_:]+\/cis$/;
const CIS_MITIGATIONS =
  /^\/user\/[-a-zA-Z0-9_:]+\/mitigations\/[-a-zA-Z0-9_]+$/;
const PRE_MITIGATIONS_PATTERN =
  /^\/user\/[-a-zA-Z0-9_:]+\/premitigations\/[-a-zA-Z0-9_]+$/;
const SUPPORTED_HTTP_METHODS = ["POST", "PUT"];

export const handler = async (
  event: APIGatewayProxyEvent,
): Promise<APIGatewayProxyResult> => {
  const httpMethod = event.httpMethod;

  try {
    const path = decodeURIComponent(event.path);
    console.info(`Received '${httpMethod}' event with path '${path}'`);

    if (!SUPPORTED_HTTP_METHODS.includes(httpMethod)) {
      return buildErrorResponse("Http Method is not supported.", 400);
    }

    const pathParameters = event.pathParameters || {};
    const userId = decodeURIComponent(pathParameters.userId || "");

    if (CIS_PATTERN.test(path)) {
      let userCisRequests: UserCisRequest[];
      try {
        userCisRequests = JSON.parse(event.body || "");
      } catch {
        return buildErrorResponse("Invalid request body", 400);
      }
      if (
        userCisRequests.some(
          (request) => !request.code || request.code.trim() === "",
        )
      ) {
        return buildErrorResponse("CI codes cannot be empty.", 400);
      }

      if (httpMethod === "POST") {
        await addUserCis(userId, userCisRequests);
      } else if (httpMethod === "PUT") {
        await updateUserCis(userId, userCisRequests);
      }
    } else if (CIS_MITIGATIONS.test(path)) {
      const ci = pathParameters.ci || "";
      let userMitigationRequest: UserMitigationRequest;
      try {
        userMitigationRequest = JSON.parse(event.body || "");
      } catch {
        return buildErrorResponse("Invalid request body", 400);
      }

      const ciData = await cimitStubItemService.getCiForUserId(userId, ci);
      if (!ciData || ciData.length === 0) {
        return buildErrorResponse("User and ContraIndicator not found.", 404);
      }
      await pendingMitigationService.persistPendingMitigation(
        userMitigationRequest,
        ci,
        httpMethod,
      );
    } else if (PRE_MITIGATIONS_PATTERN.test(path)) {
      const ci = pathParameters.ci || "";

      let userPreMitigationRequest: UserPreMitigationRequest;
      try {
        userPreMitigationRequest = JSON.parse(event.body || "");
      } catch {
        return buildErrorResponse("Invalid request body", 400);
      }

      await preMitigationService.persistPreMitigation(
        userId,
        ci,
        userPreMitigationRequest,
      );
    } else {
      return buildErrorResponse("Invalid URI.", 400);
    }

    return buildSuccessResponse();
  } catch (error: unknown) {
    console.info(`Unexpected error : ${error}`);
    return buildErrorResponse(`Unexpected error : ${error}`, 500);
  }
};

function buildSuccessResponse(): APIGatewayProxyResult {
  return { statusCode: 200, body: "success" };
}

function buildErrorResponse(
  message: string,
  statusCode: number,
): APIGatewayProxyResult {
  return { statusCode, body: message };
}
