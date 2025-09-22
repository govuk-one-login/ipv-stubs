import type {
  APIGatewayProxyEvent,
  APIGatewayProxyResult,
  APIGatewayProxyEventQueryStringParameters,
  Context,
} from 'aws-lambda';
import { DEFAULT_ISSUER, DEFAULT_AUDIENCE, AUTHENTICATION_ISSUER } from '../utils/app-config.js';
import { JwtAdapter } from '../utils/jwt-adapter.js';
import { CustomError } from '../utils/errors.js';
import {
  ALG,
  Kids,
  DEFAULT_SIGNATURE_TYPE,
  DEFAULT_TOKEN_TYPE,
  DEFAULT_TOKEN_EXPIRY,
  DEFAULT_TOKEN_INITIATED_AT,
  MILLISECONDS_IN_MINUTES,
  CONVERT_TO_SECONDS,
  TokenTypes,
  SignatureTypes,
  Algorithms,
  HttpCodesEnum,
  Scope,
} from '../utils/constants.js';
import logger from '../utils/logger.js';
import type { JWTPayload } from 'jose';
import type { JwtHeader } from '../types/token.js';

/**
 * A function for returning the status code and token.
 *
 * @param event - The API Gateway Proxy event to process.
 * @param context - Lambda context
 * @returns {@link APIGatewayProxyEvent} - A promise with the status code and token.
 * @throws {@link CustomError} - if the mocked token is not generated.
 */
export const handle = async (event: APIGatewayProxyEvent, context: Context): Promise<APIGatewayProxyResult> => {
  logger.addContext(context);
  const { signatureType, tokenType } = getQueryParameters(
    event.queryStringParameters as APIGatewayProxyEventQueryStringParameters,
  );
  logger.debug('signatureType & tokenType', { signatureType, tokenType });

  const jwtHeader = getJwtHeader(tokenType, signatureType);
  const jwtPayload = getJwtPayload(tokenType, event.body);

  logger.debug('token header & payload', { jwtHeader, jwtPayload });

  const token = await generateToken(jwtHeader, jwtPayload, signatureType);

  if (!token) {
    throw new CustomError(HttpCodesEnum.BAD_REQUEST, 'Token not generated');
  }

  return { statusCode: HttpCodesEnum.OK, body: JSON.stringify({ token }) };
};

/**
 * A function for returning the signature and token types.
 *
 * @param queryStringParameters - the query string parameters.
 * @returns A promise when the callback is specified.
 */
function getQueryParameters(queryStringParameters: APIGatewayProxyEventQueryStringParameters): {
  signatureType: SignatureTypes;
  tokenType: TokenTypes;
} {
  const queryStringTokenType = queryStringParameters && queryStringParameters['tokenType'];
  const queryStringSignatureType = queryStringParameters && queryStringParameters['signatureType'];

  const tokenType: TokenTypes =
    Object.values(TokenTypes).find((validType) => validType === queryStringTokenType) || DEFAULT_TOKEN_TYPE;
  const signatureType: SignatureTypes =
    Object.values(SignatureTypes).find((validType) => validType === queryStringSignatureType) || DEFAULT_SIGNATURE_TYPE;
  return { signatureType, tokenType };
}

/**
 * A function for returning the JWT Header.
 *
 * @param tokenType - the types of token: valid, invalid, none-algorithm, missing-kid, expired, iat in future.
 * @param signatureType - the types of signature: EC or RSA.
 */
function getJwtHeader(tokenType: TokenTypes, signatureType: SignatureTypes): JwtHeader {
  const typ = 'JWT';
  let alg: Algorithms = ALG[signatureType];
  let kid: string | undefined =
    tokenType == TokenTypes.AUTH_ISS
      ? signatureType === SignatureTypes.EC
        ? Kids.AUTH_EC
        : Kids.AUTH_RSA
      : signatureType === SignatureTypes.EC
        ? Kids.EC
        : Kids.RSA;
  switch (tokenType) {
    case TokenTypes.INVALID_ALGORITHM: {
      alg = Algorithms.INVALID;
      break;
    }
    case TokenTypes.NONE_ALGORITHM: {
      alg = Algorithms.NONE;
      break;
    }
    case TokenTypes.MISSING_KID: {
      kid = undefined;
      break;
    }
    case TokenTypes.WRONG_KID: {
      kid = Kids.WRONG;
      break;
    }
  }

  return { typ, alg, ...(kid && { kid }) };
}

/**
 * A function for returning the JWT Payload.
 *
 * @param tokenType - the types of token: valid, invalid, none-algorithm, missing-kid, expired, iat in future.
 * @param body - the body string.
 * @throws {@link CustomError} - if the event body is invalid JSON.
 */
function getJwtPayload(tokenType: TokenTypes, body: string | null): JWTPayload {
  let bodyPayload: JWTPayload = {};
  try {
    bodyPayload = typeof body === 'string' ? JSON.parse(body) : {};
  } catch (error) {
    logger.error('Event body cannot be parsed', { error });
    throw new CustomError(HttpCodesEnum.BAD_REQUEST, 'Event body is not valid JSON so cannot be parsed');
  }

  logger.debug('payload values from request body', { bodyPayload });

  const { aud: bodyAud, iss: bodyIss, iat: bodyIat, scope: bodyScope, ttl, ...payload } = bodyPayload;

  const expiresIn = typeof ttl === 'number' ? ttl : DEFAULT_TOKEN_EXPIRY;
  const initiatedAt = typeof bodyIat === 'number' ? bodyIat * -1 : DEFAULT_TOKEN_INITIATED_AT;
  const exp = tokenType === TokenTypes.EXPIRED ? getDateEpoch(-5) : getDateEpoch(expiresIn);
  const iat = tokenType === TokenTypes.IAT_IN_FUTURE ? getDateEpoch(5) : getDateEpoch(initiatedAt);
  const iss =
    tokenType === TokenTypes.AUTH_ISS
      ? AUTHENTICATION_ISSUER
      : bodyIss === null
        ? undefined
        : bodyIss || DEFAULT_ISSUER;
  const aud = bodyAud === null ? undefined : bodyAud || DEFAULT_AUDIENCE;
  const scope =
    bodyScope === null
      ? undefined
      : bodyScope || (iss === AUTHENTICATION_ISSUER ? Scope.REVERIFICATION : Scope.PROVING);

  return {
    ...payload,
    exp,
    iat,
    ...(iss && { iss }),
    ...(aud && { aud }),
    ...(scope && { scope }),
  };
}

/**
 * A function for returning the promise for generating the token.
 *
 * @param header - the JWT Header
 * @param payload - the JWT Payload.
 * @param signatureType - the signature types.
 * @throws {@link CustomError} - if the token could not be signed.
 */
async function generateToken(header: JwtHeader, payload: JWTPayload, signatureType: SignatureTypes): Promise<string> {
  try {
    const jwtAdapter = new JwtAdapter();
    const token = await jwtAdapter.sign(header, payload, signatureType);
    return token;
  } catch (error) {
    logger.error('Failed to sign the token', { error });
    throw new CustomError(HttpCodesEnum.BAD_REQUEST, 'Failed to sign token');
  }
}

/**
 * A function for getting the Date in EPOCH format.
 *
 * @returns the epoch equivalent for the date.
 * @param minutes - minutes
 */
function getDateEpoch(minutes: number) {
  return Math.floor((Date.now() + minutes * MILLISECONDS_IN_MINUTES) / CONVERT_TO_SECONDS);
}
