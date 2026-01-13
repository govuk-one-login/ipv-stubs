import { APIGatewayProxyEvent, APIGatewayProxyResultV2 } from "aws-lambda";
import {
  buildApiResponse,
  getErrorMessage,
} from "../../common/apiResponseBuilder";
import { FailedToParseRequestError } from "../../common/exceptions";
import { decodeJwt, JWTPayload } from "jose";
import { completePendingMitigation } from "../../common/pendingMitigationService";

const FAILURE_RESPONSE = "fail";
const SUCCESS_RESPONSE = "success";

export const postMitigationsHandler = async (
  request: APIGatewayProxyEvent,
): Promise<APIGatewayProxyResultV2> => {
  console.info("Function invoked:", "PostMitigations");
  try {
    const postMitigationsRequest = buildParsedRequest(request);
    postMitigationsRequest.signed_jwts?.forEach((vc) => {
      const payload = decodeJwt(vc) as JWTPayload;
      const subject = payload.sub || "";
      const jwtid = payload.jti || "";
      completePendingMitigation(jwtid, subject);
    });
    return buildApiResponse(200, { result: SUCCESS_RESPONSE });
  } catch (error) {
    if (error instanceof FailedToParseRequestError) {
      console.error(getErrorMessage(error));
      return buildApiResponse(400, {
        result: FAILURE_RESPONSE,
        errorMessage: getErrorMessage(error),
      });
    } else {
      console.error(getErrorMessage(error));
      return buildApiResponse(500, {
        result: FAILURE_RESPONSE,
        errorMessage: getErrorMessage(error),
      });
    }
  }
};

export interface PostMitigationsResponse {
  result: string;
  reason?: string;
  errorMessage?: string;
}

export interface PostMitigationsRequest {
  govuk_signin_journey_id?: string;
  ip_address?: string;
  signed_jwts?: string[];
}

const buildParsedRequest = (
  request: APIGatewayProxyEvent,
): PostMitigationsRequest => {
  const requestBody = request.body;
  if (!requestBody) {
    console.error("Missing request body");
    throw new FailedToParseRequestError("Missing request body");
  }
  const signedJwts = JSON.parse(requestBody).signed_jwts;
  if (!signedJwts || signedJwts.length === 0) {
    console.error("signed_jwts is empty");
    throw new FailedToParseRequestError("signed_jwts is empty");
  }
  const headers = request.headers;
  if (!headers) {
    console.error("No headers present in request");
    throw new FailedToParseRequestError("No headers present in request");
  }
  return {
    govuk_signin_journey_id: headers["govuk_signin_journey_id"],
    ip_address: headers["ip-address"],
    signed_jwts: signedJwts,
  };
};
