import { APIGatewayProxyEvent, APIGatewayProxyResultV2 } from "aws-lambda";
import { InvalidAccessToken, InvalidAuthHeader } from "../domain/exceptions";
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

export const getUserIdentityHandler = async (
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

    const userIdentity = await getUserIdentity(userId);

    if (!userIdentity) {
      return buildApiResponse(404, buildNotFoundResponse());
    }

    return buildApiResponse(200, userIdentity);
  } catch (e) {
    console.error(e);
    if (e instanceof InvalidAuthHeader) {
      return buildApiResponse(401, buildUnauthorisedResponse());
    }

    if (e instanceof InvalidAccessToken) {
      return buildApiResponse(403, buildForbiddenResponse());
    }

    return buildApiResponse(500, buildServerErrorResponse());
  }
};
