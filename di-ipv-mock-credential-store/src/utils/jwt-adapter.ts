import {Buffer} from 'node:buffer';
import {base64url, importPKCS8, JWTPayload, SignJWT} from 'jose';
import type {JwtHeader} from '../types/token.js';
import {ALG, Algorithms, SignatureTypes} from './constants.js';
import {getDefaultKeyValue, getPrivateKeyName} from './app-config.js';
import logger from './logger.js';
import {getParameter} from '@aws-lambda-powertools/parameters/ssm';

/**
 * An implementation of the JWS standard using Jose to sign Jwts
 *
 * @param jwtHeader - the JWT Header.
 * @param jwtPayload - the JWT Payload.
 * @param signatureType - the types of signature: EC or RSA.
 * @throws when they key is failed to be retrieved from SSM or unable to retrieve private key.
 */

export class JwtAdapter {
  ALG = ALG;
  signingKeyMap = new Map<string, string>();

  constructor() {}

  async sign(jwtHeader: JwtHeader, jwtPayload: JWTPayload, signatureType: SignatureTypes): Promise<string> {
    if (jwtHeader.alg === Algorithms.NONE || jwtHeader.alg === Algorithms.INVALID) {
      logger.info('creating token without a signature.');
      const header = base64url.encode(Buffer.from(JSON.stringify(jwtHeader)));
      const payload = base64url.encode(Buffer.from(JSON.stringify(jwtPayload)));
      return `${header}.${payload}.`;
    }

    let privateKeyPem;
    if (!this.signingKeyMap.has(signatureType)) {
      try {
        privateKeyPem = await getParameter(getPrivateKeyName(signatureType, jwtPayload.iss!));
      } catch (error) {
        logger.error(`Failed to retrieve ${signatureType} private key from SSM`, { error });
        throw new Error('Failed to retrieve key from SSM');
      }

      if (!privateKeyPem || privateKeyPem.trim().length === 0 || privateKeyPem === getDefaultKeyValue()) {
        logger.error('Unable to retrieve private key');
        throw new Error('Unable to retrieve private key');
      }

      this.signingKeyMap.set(signatureType, privateKeyPem);
    }

    privateKeyPem = this.signingKeyMap.get(signatureType);
    const algorithm = signatureType === SignatureTypes.EC ? Algorithms.EC : Algorithms.RSA;
    const privateKey = await importPKCS8(privateKeyPem as string, algorithm);

    let jwt;

    try {
      const unSignedJwt = await new SignJWT(jwtPayload)
        .setProtectedHeader(jwtHeader)
        .setExpirationTime(jwtPayload.exp ?? 0);

      if (jwtPayload.aud) unSignedJwt.setAudience(jwtPayload.aud);
      if (jwtPayload.iss) unSignedJwt.setIssuer(jwtPayload.iss);

      jwt = await unSignedJwt.sign(privateKey);
    } catch (error) {
      logger.error('Failed to sign Jwt', { error });
      throw new Error('Failed to sign Jwt');
    }

    logger.info('Successfully signed token');
    return jwt;
  }
}
