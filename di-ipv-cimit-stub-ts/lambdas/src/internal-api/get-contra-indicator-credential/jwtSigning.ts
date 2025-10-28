import { JWTPayload, SignJWT, importPKCS8 } from "jose";
import { getCimitSigningKey } from "../../common/configService";

export const signJWT = async (claimsSet: JWTPayload): Promise<string> => {
  return new SignJWT(claimsSet)
      .setProtectedHeader({ alg: "ES256", typ: "JWT" })
      .sign(await importPKCS8(
        `-----BEGIN PRIVATE KEY-----\n${await getCimitSigningKey()}\n-----END PRIVATE KEY-----`, // pragma: allowlist secret
        "ES256"));
      }
