import { getSsmParameter } from "./ssmParameter";
import { config } from "../config/config";
import { importSPKI, jwtVerify } from "jose";
import { InvalidAccessToken, InvalidAuthHeader } from "../domain/exceptions";

const BEARER_AUTH = "Bearer";

export const getUserIdFromBearerToken = async (
  authHeader: string,
): Promise<string | undefined> => {
  const bearerToken = getTokenFromAuthHeader(authHeader);

  const evcsVerifyKey = await getSsmParameter(
    config.evcsParamBasePath + "verifyKey",
  );

  try {
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

const getTokenFromAuthHeader = (authHeader: string) => {
  const authHeaderParts = authHeader.split(" ");

  if (authHeaderParts.length != 2) {
    throw new InvalidAuthHeader("Invalid auth header format");
  }

  if (authHeaderParts[0] != BEARER_AUTH) {
    throw new InvalidAuthHeader("Invalid auth header - must be Bearer type");
  }

  return authHeaderParts[1];
};
