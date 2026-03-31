import { decodeJwt } from "jose";

export function getErrorMessage(error: unknown) {
  if (error instanceof Error) return error.message;
  return String(error);
}

export function isValidJWT(token: string): boolean {
  const parts = token.split(".");
  if (parts.length !== 3) {
    return false;
  }
  const [header, payload, signature] = parts;
  if (!header || !payload || !signature) {
    return false;
  }
  const isBase64Url = (str: string) => /^[A-Za-z0-9_-]+$/.test(str);
  if (
    !isBase64Url(header) ||
    !isBase64Url(payload) ||
    !isBase64Url(signature)
  ) {
    return false;
  }

  try {
    decodeJwt(token);
  } catch {
    return false;
  }
  return true;
}

export function getSignatureFromJwt(jwt: string): string {
  return jwt.split(".")[2];
}
