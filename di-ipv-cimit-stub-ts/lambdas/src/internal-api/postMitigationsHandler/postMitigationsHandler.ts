import { APIGatewayProxyEvent, APIGatewayProxyResultV2, Context } from "aws-lambda";
import { buildApiResponse, getErrorMessage } from "../../common/apiResponseBuilder";
import { FailedToParseRequestError } from "./exceptions";
import { decodeJwt, JWTPayload } from "jose";

const FAILURE_RESPONSE = "fail";
const SUCCESS_RESPONSE = "success";

export const postMitigationsHandler = async (
  request: APIGatewayProxyEvent, context: Context
): Promise<APIGatewayProxyResultV2> => {
  console.info("Function invoked:", "PostMitigations");
  try {
    const postMitigationsRequest = buildParsedRequest(request);
    var accumulator = [];
    postMitigationsRequest.signed_jwts?.forEach(vc => {
      var payload = decodeJwt(vc) as JWTPayload;
      const subject = payload.sub;
      const jwtid = payload.jti;
      accumulator.push(payload);
    })

    return buildApiResponse(200, {result: accumulator});
  } catch (error) {
    return buildApiResponse(500, {
          message: getErrorMessage(error),
        });
  }


  // extract necessary information from incoming request

  // for each of the VCs (string) in the request (in the list SignedJwtVCs)
  //    parse the VC

  //    get the corresponding JWTClaimsSet
  //    completePendingMitigation using
  //      JWTID from the JWTClaimsSet
  //      Subject from the JWTClaimsSet
  //      and the CimitStubItemService
  // steps in completePendingMitigation
  //    retrieve a PendingMitigationItem from DB by jwtId
  //    (check an item actuallly came back)
  //    get a list of CimitStubItems from DB by userID and CI
  //    (check the list isn't null/empty)
  //    add/set mitigations to the most recent CimitStubItem in the list (getIssuanceDate)
  //    update the CimitStubItem in the DB
  //        set the ttl in the CimitStubItem
  //        then call the DB with the CimitStubItem
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