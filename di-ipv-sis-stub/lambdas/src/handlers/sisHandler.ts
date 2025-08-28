import { APIGatewayProxyEvent, APIGatewayProxyResultV2 } from "aws-lambda";
import { InvalidAccessToken, InvalidAuthHeader } from "../domain/exceptions";
import { getUserIdentity } from "../services/sisService";
import { buildApiResponse } from "../utils/apiResponseBuilder";
import { config } from "../config/config";
import { getSsmParameter } from "../utils/ssmParameter";
import { importSPKI, jwtVerify } from "jose";
import {
  buildBadRequestResponse,
  buildForbiddenResponse,
  buildNotFoundResponse,
  buildServerErrorResponse,
  buildUnauthorisedResponse,
} from "../domain/errorResponse";

const BEARER_AUTH = "Bearer";
const AUTHORISATION_HEADER = "Authorization";

export const getUserIdentityHandler = async (
  event: APIGatewayProxyEvent,
): Promise<APIGatewayProxyResultV2> => {
  console.info("------------Processing GET user-identity request------------");
  try {
    const userId = await getUserIdFromBearerToken(
      event.headers[AUTHORISATION_HEADER],
    );

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

const getUserIdFromBearerToken = async (
  authHeader: string | undefined,
): Promise<string | undefined> => {
  const bearerToken = getTokenFromAuthHeader(authHeader);

  try {
    const evcsVerifyKey = await getSsmParameter(
      config.evcsParamBasePath + "verifyKey",
    );

    const key = await importSPKI(
      `-----BEGIN PUBLIC KEY-----\n${evcsVerifyKey}\n-----END PUBLIC KEY-----`,
      "ES256",
    );
    const payload = (await jwtVerify(bearerToken, key)).payload;

    return payload.sub;
  } catch (error) {
    console.error(error);
    throw new InvalidAccessToken("Failed to verify bearer token");
  }
};

const getTokenFromAuthHeader = (authHeader: string | undefined) => {
  if (!authHeader) {
    throw new InvalidAuthHeader("Missing auth header");
  }

  const authHeaderParts = authHeader.split(" ");

  if (authHeaderParts.length != 2) {
    throw new InvalidAuthHeader("Invalid auth header format");
  }

  if (authHeaderParts[0] != BEARER_AUTH) {
    throw new InvalidAuthHeader("Invalid auth header - must be Bearer type");
  }

  const token = authHeaderParts[1];

  if (!token) {
    throw new InvalidAuthHeader("Empty bearer token");
  }

  return token;
};
