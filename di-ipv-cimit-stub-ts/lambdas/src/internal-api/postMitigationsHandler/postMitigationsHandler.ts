import { APIGatewayProxyEvent, APIGatewayProxyResultV2, Context } from "aws-lambda";
import { buildApiResponse, getErrorMessage } from "../../common/apiResponseBuilder";
import { FailedToParseRequestError } from "./exceptions";

const FAILURE_RESPONSE = "fail";
const SUCCESS_RESPONSE = "success";

export const postMitigationsHandler = async (
  request: APIGatewayProxyEvent, context: Context
): Promise<APIGatewayProxyResultV2> => {
  console.info("Function invoked:", "PostMitigations");
  try {
    const postMitigationsRequest = buildParsedRequest(request);
    console.log(postMitigationsRequest);
    return buildApiResponse(200, {result: SUCCESS_RESPONSE});
  } catch (error) {
    return buildApiResponse(500, {
          message: getErrorMessage(error),
        });
  }


  // extract necessary information from incoming request

  // for each of the VCs in the request (in the list SignedJwtVCs)

  //    get an item from the database that matches the JWTid
  //    get the CIs from the database that match the userId in the item
  //
};

export interface PostMitigationsResponse {
  result: string;
  reason?: string;
  errorMessage?: string;
}

export interface PostMitigationsRequest {
  govuk_signin_journey_id?: string;
  ip_address?: string
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
  if (!signedJwts) {
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
}