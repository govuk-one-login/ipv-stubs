import { importPKCS8, SignJWT } from "jose";
import { createHash } from "node:crypto";
import { config } from "../config/config";
import { StoredIdentityJwt } from "../domain/userIdentity";

export const createSignedJwt = async (jwt: SignJWT) => {
  if (!config.sisSigningKey) {
    throw new Error("No signing key found");
  }

  const signingKey = await importPKCS8(
    `-----BEGIN PRIVATE KEY-----\n${config.sisSigningKey}\n-----END PRIVATE KEY-----`, // pragma: allowlist secret
    "ES256",
  );

  return await jwt
    .setProtectedHeader({ alg: "ES256", typ: "JWT", kid: createKid() })
    .sign(signingKey);
};

const createKid = () => {
  const hash = createHash("sha256");
  return hash
    .update(`${config.didStoredIdentityId}#${config.sisSigningKeyId}`)
    .digest("hex");
};

export const updateVotOnSiVot = (
  siJwt: StoredIdentityJwt,
  matchedProfile: string | undefined,
) => {
  const jwt = new SignJWT({
    claims: siJwt.claims,
    vot: matchedProfile,
    credentials: siJwt.credentials,
  });

  if (siJwt.aud) {
    jwt.setAudience(siJwt.aud);
  }

  if (siJwt.aud) {
    jwt.setAudience(siJwt.aud);
  }

  if (siJwt.nbf) {
    jwt.setNotBefore(siJwt.nbf);
  }

  if (siJwt.iss) {
    jwt.setIssuer(siJwt.iss);
  }

  if (siJwt.iat) {
    jwt.setIssuedAt(siJwt.iat);
  }

  return jwt;
};
