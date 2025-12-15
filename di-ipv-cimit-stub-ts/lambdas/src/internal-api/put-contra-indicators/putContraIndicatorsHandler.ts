import { APIGatewayProxyEvent, APIGatewayProxyResultV2, Context } from "aws-lambda";
import { buildApiResponse, getErrorMessage } from "../../common/apiResponseBuilder";
import { FailedToParseRequestError } from "../../common/exceptions";
import { addUserCIs } from "../../common/contraIndicatorsService";


const FAILURE_RESPONSE = "fail";
const SUCCESS_RESPONSE = "success";


export const putContraIndicatorsHandler = async (
  request: APIGatewayProxyEvent, context: Context
): Promise<APIGatewayProxyResultV2> => {
    try {
        const parsedRequest = parseRequest(request);
        addUserCIs(parsedRequest);
        return buildApiResponse(200, {result: SUCCESS_RESPONSE});
    }
    catch (error) {
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

}

const parseRequest = (
  request: APIGatewayProxyEvent,
): PutContraIndicatorRequest => {
  const requestBody = request.body;
  if (!requestBody) {
    console.error("Missing request body");
    throw new FailedToParseRequestError("Missing request body");
  }
  const signedJwt = JSON.parse(requestBody).signed_jwt;
  if (!signedJwt) {
    console.error("signed_jwt is empty");
    throw new FailedToParseRequestError("signed_jwt is empty");
  }
  const headers = request.headers;
  return {
    govuk_signin_journey_id: headers["govuk_signin_journey_id"],
    ip_address: headers["ip-address"],
    signed_jwt: signedJwt,
  };
}

export interface PutContraIndicatorRequest {
  govuk_signin_journey_id?: string;
  ip_address?: string
  signed_jwt: string;
}

