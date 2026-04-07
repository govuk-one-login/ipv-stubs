import { decodeJwt } from "jose";
import { decode } from "jose/base64url";

export function getErrorMessage(error: unknown) {
  if (error instanceof Error) return error.message;
  return String(error);
}

export function isValidJWT(token: string): boolean {
  if (!token) {
    return false;
  }
  try {
    // decodeJwt validates only payload shape and 3 parts jwt format
    decodeJwt(token);
  } catch {
    return false;
  }
  const parts = token.split(".");
  const header = parts[0];
  const signature = parts[2];
  return !(!isBase64Url(header) || !isBase64Url(signature));
}

export function isBase64Url(str: string): boolean {
  try {
    decode(str);
    return true;
  } catch {
    return false;
  }
}

export function getSignatureFromJwt(jwt: string): string {
  return jwt.split(".")[2];
}
