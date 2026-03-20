import { JWTPayload, importSPKI, jwtVerify } from "jose";
import { getErrorMessage } from "../common/utils";

export const verifyTokenAndReturnPayload = async (
  jwt: string,
): Promise<JWTPayload> => {
  const EVCS_VERIFY_KEY = process.env.EVCS_STUB_VERIFY_KEY;

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
