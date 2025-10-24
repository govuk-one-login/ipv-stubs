import {APIGatewayProxyEvent, APIGatewayProxyResultV2} from "aws-lambda";
import {BadRequestError} from "./exceptions";
import {buildApiResponse, getErrorMessage} from "./apiResponseBuilder";

interface GetContraIndicatorCredentialRequest {
  userId: string;
  govukSigninJourneyId: string;
  ipAddress: string;
}

export interface GetContraIndicatorCredentialResponse {
  vc: string;
}

export const getContraIndicatorCredentialHandler = async (request: APIGatewayProxyEvent): Promise<APIGatewayProxyResultV2> => {
  console.info("Function invoked:", "GetContraIndicatorCredential");
  try {
    const parsedRequest = validateAndParseRequest(request);

    return buildApiResponse(200, {message: "hi"});
  } catch (error) {
    console.error(getErrorMessage(error));

    if (error instanceof BadRequestError) {
      return buildApiResponse(400, {
        message: getErrorMessage(error),
      });
    }

    return buildApiResponse(500, {
      message: getErrorMessage(error),
    });
  }
}

const validateAndParseRequest = (request: APIGatewayProxyEvent): GetContraIndicatorCredentialRequest => {
  const userId = request.queryStringParameters?.["user_id"];

  if (!userId) {
    console.error("Missing userId from request");
    throw new BadRequestError("Missing userId from request");
  }

  const govukSigninJourneyId = request.headers["govuk-signin-journey-id"];
  if (!govukSigninJourneyId) {
    console.error("Missing govukSigninJourneyId from request");
    throw new BadRequestError("Missing govukSigninJourneyId from request");
  }

  const ipAddress = request.headers["ip-address"];
  if (!ipAddress) {
    console.error("Missing ipAddress from request");
    throw new BadRequestError("Missing ipAddress from request");
  }

  return {
    userId,
    govukSigninJourneyId,
    ipAddress
  }
}