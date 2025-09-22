import * as jwksLambda from '../jwks.js';
import * as jose from 'jose';
import * as appConfig from '../../utils/app-config.js';
import logger from '../../utils/logger.js';
import { getParameter } from '@aws-lambda-powertools/parameters/ssm';
import { APIGatewayEvent, Context } from "aws-lambda";

jest.mock('jose');
jest.mock('../../utils/logger');
jest.mock('../../utils/app-config');
jest.mock('node:crypto');
jest.mock('@aws-lambda-powertools/parameters/ssm');

const mockContext = {} as unknown as Context;

const getParameterCommand = getParameter as jest.Mock;
const mockEvent = {} as APIGatewayEvent;

describe('jwks', () => {
  afterAll(() => {
    jest.useRealTimers();
  });

  describe('when simulating errors', () => {
    beforeEach(() => {
      jest.spyOn(global.Math, 'random').mockReturnValue(0.2);
      jest.useFakeTimers();
    });

    it('returns a 4XX error when environment variable is higher than random number', async () => {
      jest.spyOn(appConfig, 'getPercentageReturn4xx').mockReturnValue(0.3);
      const mockBody = { message: 'Intentionally returned a 400 from Mock JWKS.' };
      const response = await jwksLambda.handle(mockEvent, mockContext);
      expect(response).toEqual({ statusCode: 400, body: JSON.stringify(mockBody) });
      jest.spyOn(appConfig, 'getPercentageReturn4xx').mockRestore();
    });

    it('returns a 5XX error when environment variable is higher than random number', async () => {
      jest.spyOn(appConfig, 'getPercentageReturn5xx').mockReturnValue(0.3);
      const mockBody = { message: 'Intentionally returned a 500 from Mock JWKS.' };
      const response = await jwksLambda.handle(mockEvent, mockContext);
      expect(response).toEqual({ statusCode: 500, body: JSON.stringify(mockBody) });
      jest.spyOn(appConfig, 'getPercentageReturn5xx').mockRestore();
    });

    it('returns a Timeout error when environment variable is higher than random number', async () => {
      jest.spyOn(appConfig, 'getPercentageTimeout').mockReturnValue(0.3);
      const mockBody = { message: 'Intentionally returned a timeout error from Mock JWKS.' };
      const response = jwksLambda.handle(mockEvent, mockContext);
      jest.runAllTimers();
      expect(await response).toEqual({ statusCode: 408, body: JSON.stringify(mockBody) });
    });
  });

  describe('when jwks and keys are not cached', () => {
    beforeEach(() => {
      Object.defineProperty(jwksLambda, 'cachedJwks', { value: undefined, writable: true });
      Object.defineProperty(jwksLambda, 'publicKeyMap', { value: new Map(), writable: true });
      jest.spyOn(global.Math, 'random').mockReturnValue(1);
      jest.spyOn(appConfig, 'getDefaultKeyValue').mockReturnValue('mock-value');
      getParameterCommand.mockReturnValue('publicKey');
    });

    it('returns a valid json body with two JWKs with correct claims', async () => {
      jest.spyOn(jose, 'exportJWK').mockResolvedValueOnce({ kty: 'EC' });
      jest.spyOn(jose, 'exportJWK').mockResolvedValue({ kty: 'RSA' });

      const expectedEcKey = { kty: 'EC', kid: 'ecKid123', alg: 'ES256', use: 'sig' };
      const expectedRsaKey = { kty: 'RSA', kid: 'rsaKid123', alg: 'RS256', use: 'sig' };
      const expectedJson = JSON.stringify({ keys: [expectedEcKey, expectedRsaKey] });

      const response = await jwksLambda.handle(mockEvent, mockContext);

      expect(response.body).toEqual(expectedJson);
    });

    it('returns two AUTHENTICATION JWKs with correct claims when calling Authentication path', async () => {
      jest.spyOn(jose, 'exportJWK').mockResolvedValueOnce({ kty: 'EC' });
      jest.spyOn(jose, 'exportJWK').mockResolvedValueOnce({ kty: 'RSA' });
      jest.spyOn(jose, 'exportJWK').mockResolvedValueOnce({ kty: 'EC' });
      jest.spyOn(jose, 'exportJWK').mockResolvedValueOnce({ kty: 'RSA' });

      const expectedEcKey = { kty: 'EC', kid: 'AuthEcKid123', alg: 'ES256', use: 'sig' };
      const expectedRsaKey = { kty: 'RSA', kid: 'AuthRsaKid123', alg: 'RS256', use: 'sig' };
      const expectedJson = JSON.stringify({ keys: [expectedEcKey, expectedRsaKey] });

      const response = await jwksLambda.handle({
        path: 'authentication/' } as APIGatewayEvent , mockContext);

      expect(response.body).toEqual(expectedJson);
    });

    it('does not include key in jwks if exported jwk is not defined', async () => {
      //@ts-ignore
      jest.spyOn(jose, 'exportJWK').mockResolvedValue({});

      const response = await jwksLambda.handle(mockEvent, mockContext);

      expect(response.body).toBe( "{}" );
    });

    it('throws error if it fails to retrieve public key from SSM', async () => {
      getParameterCommand.mockRejectedValue('error');
      await expect(jwksLambda.handle(mockEvent, mockContext)).rejects.toThrow('Failed to retrieve key from SSM');
    });

    it('throws error if public key from SSM is undefined', async () => {
      getParameterCommand.mockReturnValue(undefined);
      await expect(jwksLambda.handle(mockEvent, mockContext)).rejects.toThrow('Unable to retrieve public key');
    });

    it('throws error if public key from SSM is an empty string', async () => {
      getParameterCommand.mockReturnValue('');
      await expect(jwksLambda.handle(mockEvent, mockContext)).rejects.toThrow('Unable to retrieve public key');
    });

    it('throws error if public key from SSM is equal to the default value', async () => {
      getParameterCommand.mockReturnValue('mock-value');
      await expect(jwksLambda.handle(mockEvent, mockContext)).rejects.toThrow('Unable to retrieve public key');
    });

    it('throws error if generating public key fails', async () => {
      jest.spyOn(jose, 'exportJWK').mockRejectedValue('error');
      await expect(jwksLambda.handle(mockEvent, mockContext)).rejects.toThrow('Unable to export jwk');
    });
  });

  describe('when jwks and keys are cached', () => {
    beforeAll(async () => {
      jest.spyOn(jose, 'exportJWK').mockResolvedValueOnce({ kty: 'EC' });
      jest.spyOn(jose, 'exportJWK').mockResolvedValue({ kty: 'RSA' });
      await jwksLambda.handle(mockEvent, mockContext);
    });
    it('does not create new jwks and returns cached jwks in response', async () => {
      const expectedEcKey = { kty: 'EC', kid: 'ecKid123', alg: 'ES256', use: 'sig' };
      const expectedRsaKey = { kty: 'RSA', kid: 'rsaKid123', alg: 'RS256', use: 'sig' };
      const expectedJson = JSON.stringify({ keys: [expectedEcKey, expectedRsaKey] });

      const response = await jwksLambda.handle(mockEvent, mockContext);

      expect(logger.info).toHaveBeenCalledWith('returning cached JWKSs');
      expect(response.body).toEqual(expectedJson);
    });
  });
});
