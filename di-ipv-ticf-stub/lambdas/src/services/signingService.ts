import { JWTPayload, SignJWT, importPKCS8 } from "jose";

export async function signJwt(
  payload: JWTPayload,
  signingKey: string
): Promise<string> {
  const key = await importPKCS8(
    `-----BEGIN PRIVATE KEY-----\n${signingKey}\n-----END PRIVATE KEY-----`,
    "ES256"
  );

  return new SignJWT(payload)
    .setProtectedHeader({ alg: "ES256", typ: "JWT" })
    .sign(key);
}
