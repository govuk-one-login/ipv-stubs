import { APIGatewayProxyEvent, APIGatewayProxyResultV2 } from "aws-lambda";
import {
  InvalidAccessToken,
  InvalidAuthHeader,
  InvalidRequestBody,
} from "../domain/exceptions";
import { getUserIdentity } from "../services/sisService";
import { buildApiResponse } from "../utils/apiResponseBuilder";
import {
  buildBadRequestResponse,
  buildForbiddenResponse,
  buildNotFoundResponse,
  buildServerErrorResponse,
  buildUnauthorisedResponse,
} from "../domain/errorResponse";
import { getUserIdFromBearerToken } from "../utils/tokenVerifier";

const AUTHORISATION_HEADER = "Authorization";

export const postUserIdentityHandler = async (
  event: APIGatewayProxyEvent,
): Promise<APIGatewayProxyResultV2> => {
  console.info("------------Processing GET user-identity request------------");
  try {
    const authHeader = event.headers[AUTHORISATION_HEADER];

    if (!authHeader) {
      throw new InvalidAuthHeader("Missing auth header");
    }

    const userId = await getUserIdFromBearerToken(authHeader);

    if (!userId) {
      return buildApiResponse(400, buildBadRequestResponse("Missing user id"));
    }

    validateUserIdentityRequestBody(event);

    const userIdentity = await getUserIdentity(userId);

    if (!userIdentity) {
      return buildApiResponse(404, buildNotFoundResponse());
    }

    return buildApiResponse(200, userIdentity);
  } catch (e) {
    console.error(e);
    if (e instanceof InvalidRequestBody) {
      return buildApiResponse(400, buildBadRequestResponse(e.message));
    }

    if (e instanceof InvalidAuthHeader) {
      return buildApiResponse(401, buildUnauthorisedResponse());
    }

    if (e instanceof InvalidAccessToken) {
      return buildApiResponse(403, buildForbiddenResponse());
    }

    return buildApiResponse(500, buildServerErrorResponse());
  }
};

const validateUserIdentityRequestBody = (event: APIGatewayProxyEvent) => {
  console.info("---Parsing user identity request body---");

  if (!event.body) {
    throw new InvalidRequestBody("Missing request body");
  }

  const requestBody = JSON.parse(event.body);

  if (!requestBody.vtr || requestBody.vtr.length === 0) {
    throw new InvalidRequestBody("Missing vtr in request body");
  }

  if (!requestBody.govukSigninJourneyId) {
    throw new InvalidRequestBody(
      "Missing govukSigninJourneyId in request body",
    );
  }
};
