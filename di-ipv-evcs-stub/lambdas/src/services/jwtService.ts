import { JWTPayload, importSPKI, jwtVerify } from "jose";
import { config } from "../common/config";
import { getSsmParameter } from "../common/ssmParameter";
import { getErrorMessage } from "../common/utils";

export const verifyTokenAndReturnPayload = async (
  jwt: string,
): Promise<JWTPayload> => {
  const EVCS_VERIFY_KEY = await getSsmParameter(
    config.evcsParamBasePath + "verifyKey",
  );

  let payload;
  try {
    const key = await importSPKI(
      `-----BEGIN PUBLIC KEY-----\n${EVCS_VERIFY_KEY}\n-----END PUBLIC KEY-----`,
      "ES256",
    );
    payload = (await jwtVerify(jwt, key)).payload as JWTPayload;
  } catch (error) {
    throw Error(getErrorMessage(error));
  }
  return payload;
};
