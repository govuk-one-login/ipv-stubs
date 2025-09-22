import type { APIGatewayEvent, APIGatewayProxyResult, Context } from 'aws-lambda';
import type { JwksKeyType } from '../types/token.js';
import { JsonWebKey } from 'node:crypto';
import {
  getDefaultKeyValue,
  getPublicKeyName,
  getPercentageReturn4xx,
  getPercentageReturn5xx,
  getPercentageTimeout,
  getPercentageDelay,
  getMaximumDelayMilliseconds,
  DEFAULT_ISSUER,
  AUTHENTICATION_ISSUER,
} from '../utils/app-config.js';
import { Algorithms, SignatureTypes, Kids, JWKS_TIMEOUT_MILLISECONDS, HttpCodesEnum } from '../utils/constants.js';
import logger from '../utils/logger.js';
import { CustomError } from '../utils/errors.js';

import { importSPKI, exportJWK } from 'jose';
import { getParameter } from '@aws-lambda-powertools/parameters/ssm';

const JWKS_KEY_TYPES: JwksKeyType[] = [
  { kty: SignatureTypes.EC, alg: Algorithms.EC, kid: Kids.EC },
  { kty: SignatureTypes.RSA, alg: Algorithms.RSA, kid: Kids.RSA },
  { kty: SignatureTypes.EC, alg: Algorithms.EC, kid: Kids.AUTH_EC },
  { kty: SignatureTypes.RSA, alg: Algorithms.RSA, kid: Kids.AUTH_RSA },
];

export let cachedJwks: { keys: JsonWebKey[] };
export const publicKeyMap = new Map<string, string>();

/**
 * JWKS handler. Builds or returns cached JWKS.
 * @returns {@link APIGatewayProxyResult} - as a promise, contains a status code and a body.
 */
export const handle = async (event: APIGatewayEvent, context: Context): Promise<APIGatewayProxyResult> => {
  logger.addContext(context);
  const error = await simulateError();
  if (error) return error;

  logger.debug(JSON.stringify(event));

  if (!cachedJwks || Object.keys(cachedJwks).length <= 0) {
    logger.info('building JWKS');
    const jwks: { keys: JsonWebKey[] } = { keys: [] };

    for (const keyType of JWKS_KEY_TYPES) {
      const { alg, kty, kid } = keyType;
      let publicKeyPem;
      if (!publicKeyMap.has(kid)) {
        try {
          publicKeyPem = await getParameter(
            getPublicKeyName(kty, kid === Kids.EC || kid === Kids.RSA ? DEFAULT_ISSUER : AUTHENTICATION_ISSUER),
          );
        } catch (error: unknown) {
          logger.error(`Failed to retrieve ${kty} public key from SSM`, { error });
          throw new CustomError(HttpCodesEnum.BAD_REQUEST, 'Failed to retrieve key from SSM');
        }

        if (!publicKeyPem || publicKeyPem.trim().length === 0 || publicKeyPem === getDefaultKeyValue()) {
          logger.error('Unable to retrieve public key');
          throw new CustomError(HttpCodesEnum.BAD_REQUEST, 'Unable to retrieve public key');
        }

        publicKeyMap.set(kid, publicKeyPem);
      }

      publicKeyPem = publicKeyMap.get(kid);
      const publicKeySpki = await importSPKI(publicKeyPem as string, alg);

      let publicJwk;
      try {
        publicJwk = await exportJWK(publicKeySpki);
      } catch (error) {
        logger.error(`Failed to export the ${kty} jwk`, { error });
        throw new CustomError(HttpCodesEnum.BAD_REQUEST, 'Unable to export jwk');
      }

      if (Object.keys(publicJwk).length > 0) {
        const key = { ...publicJwk, kid, alg, use: 'sig' };
        jwks.keys.push(key);
      } else {
        logger.warn('Public key undefined, unable to add key to JWKS.');
      }

      if (jwks.keys.length > 0) {
        cachedJwks = jwks;
      }
    }
  } else {
    logger.info('returning cached JWKSs');
  }

  logger.debug(JSON.stringify(cachedJwks));
  const filteredKeys =
    extractIssuer(event) === AUTHENTICATION_ISSUER
      ? cachedJwks?.keys.filter((keyInfo) => (keyInfo['kid'] as string).includes('Auth'))
      : cachedJwks?.keys.filter((keyInfo) => !(keyInfo['kid'] as string).includes('Auth'));

  if (filteredKeys?.length > 0) return { statusCode: 200, body: JSON.stringify({ keys: filteredKeys }) };
  return { statusCode: 500, body: JSON.stringify({}) };
};

/**
 * A function for simulating an error.
 *
 * @returns {@link APIGatewayProxyResult} - resolves the statusCode to simulate the error (4xx, 5xx or timeout 408).
 *                                        - undefined if the error is not triggered.
 */
async function simulateError(): Promise<APIGatewayProxyResult | undefined> {
  if (getPercentageReturn4xx() > Math.random()) {
    const statusCode = selectRandomItem([404, 400, 401, 403, 429]);
    logger.info('Intentionally returned a ${statusCode} error from Mock JWKS.');
    return { statusCode, body: JSON.stringify({ message: `Intentionally returned a ${statusCode} from Mock JWKS.` }) };
  }

  if (getPercentageReturn5xx() > Math.random()) {
    const statusCode = selectRandomItem([500, 501, 502]);
    logger.info('Intentionally returned a ${statusCode} error from Mock JWKS.');
    return { statusCode, body: JSON.stringify({ message: `Intentionally returned a ${statusCode} from Mock JWKS.` }) };
  }

  if (getPercentageTimeout() > Math.random()) {
    logger.info('Intentionally returned a timeout error from Mock JWKS.');
    await wait(JWKS_TIMEOUT_MILLISECONDS);
    return {
      statusCode: 408,
      body: JSON.stringify({ message: `Intentionally returned a timeout error from Mock JWKS.` }),
    };
  }

  if (getPercentageDelay() > Math.random()) {
    const maximumDelay = getMaximumDelayMilliseconds();
    const randomDelay = Math.floor(Math.random() * (maximumDelay + 1));
    logger.info('Intentionally added a random delay of ${randomDelay} to the Mock JWKS');
    await wait(randomDelay);
  }

  return;
}

/**
 * A function for selecting a random item.
 *
 * @param array - numbers.
 * @returns - An array of randomised numbers.
 */
function selectRandomItem(array: number[]) {
  return array[Math.floor(Math.random() * array.length)] as number;
}

function extractIssuer(event: APIGatewayEvent): string {
  if (event.path?.includes('authentication/')) return AUTHENTICATION_ISSUER;
  return DEFAULT_ISSUER;
}
/**
 * A function for delaying/ setting a time-out so the call back is called as close to the time specified.
 *
 * @param milliseconds - in numbers.
 * @returns - A promise when the callback is specified.
 */
async function wait(milliseconds: number) {
  return await new Promise((resolve) => setTimeout(resolve, milliseconds));
}
