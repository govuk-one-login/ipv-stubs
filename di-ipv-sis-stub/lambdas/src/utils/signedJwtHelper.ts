import { importPKCS8, SignJWT } from "jose";
import { createHash } from "node:crypto";
import { config } from "../config/config";
import { StoredIdentityJwt } from "../domain/userIdentity";
import { getSsmParameter } from "./ssmParameter";

export const createSignedJwt = async (jwt: SignJWT) => {
  const sisSigningKey = await getSsmParameter(
    config.sisParamsBasePath + "sisSigningKey",
  );

  const signingKey = await importPKCS8(
    `-----BEGIN PRIVATE KEY-----\n${sisSigningKey}\n-----END PRIVATE KEY-----`, // pragma: allowlist secret
    "ES256",
  );

  return await jwt
    .setProtectedHeader({ alg: "ES256", typ: "JWT", kid: await createKid() })
    .sign(signingKey);
};

const createKid = async () => {
  const sisSigningKeyId = await getSsmParameter(
    config.sisParamsBasePath + "sisSigningKeyId",
  );

  const didStoredIdentityId = await getSsmParameter(
    config.sisParamsBasePath + "didStoredIdentityId",
  );

  const hash = createHash("sha256");
  return hash.update(`${didStoredIdentityId}#${sisSigningKeyId}`).digest("hex");
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
