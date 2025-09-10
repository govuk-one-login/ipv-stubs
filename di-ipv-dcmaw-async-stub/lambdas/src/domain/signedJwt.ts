import { importPKCS8, SignJWT } from "jose";
import { JWTPayload } from "jose/dist/types/types";

export async function vcToSignedJwt(vc: JWTPayload, signingKey: string) {
  const formattedSigningKey = await importPKCS8(
    `-----BEGIN PRIVATE KEY-----\n${signingKey}\n-----END PRIVATE KEY-----`, // pragma: allowlist secret - the key is coming from config
    "ES256",
  );
  return await new SignJWT(vc)
    .setProtectedHeader({ alg: "ES256", typ: "JWT" })
    .sign(formattedSigningKey);
}
