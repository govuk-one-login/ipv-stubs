import { JwtAdapter } from '../jwt-adapter.js';
import * as jose from 'jose';
import type { JwtHeader } from '../../types/token.js';
import { Algorithms, SignatureTypes } from '../constants.js';
import { getParameter } from '@aws-lambda-powertools/parameters/ssm';

jest.mock('jose');
jest.mock('@aws-lambda-powertools/parameters/ssm');
jest.mock('../../utils/logger');

const mockJoseSignJwt = jose.SignJWT as jest.Mock;
const getParameterCommand = getParameter as jest.Mock;

describe('JwtAdapter', () => {
  let header: JwtHeader;
  let payload: jose.JWTPayload;

  beforeEach(() => {
    jest.clearAllMocks();
    header = { alg: Algorithms.EC };
    payload = {};

    jest.spyOn(jose.base64url, 'encode').mockReturnValueOnce('encodedHeader').mockReturnValueOnce('encodedPayload');

    mockJoseSignJwt.prototype.setProtectedHeader.mockReturnThis();
    mockJoseSignJwt.prototype.setIssuedAt.mockReturnThis();
    mockJoseSignJwt.prototype.setIssuer.mockReturnThis();
    mockJoseSignJwt.prototype.setAudience.mockReturnThis();
    mockJoseSignJwt.prototype.setExpirationTime.mockReturnThis();
    mockJoseSignJwt.prototype.sign.mockReturnValue('jwtHeader.jwtPayload.jwtSignature');

    getParameterCommand.mockReturnValue('privateKey');
  });

  describe('sign', () => {
    describe('when private keys not cached', () => {
      it('throws error if it fails to retrieve public key from SSM', async () => {
        getParameterCommand.mockRejectedValue('error');
        const signatureType = SignatureTypes.EC;

        const jwtAdapter = new JwtAdapter();

        await expect(jwtAdapter.sign(header, payload, signatureType)).rejects.toThrow(
          'Failed to retrieve key from SSM',
        );
      });

      it('throws error if public key from SSM is undefined', async () => {
        getParameterCommand.mockReturnValue(undefined);
        const signatureType = SignatureTypes.EC;

        const jwtAdapter = new JwtAdapter();

        await expect(jwtAdapter.sign(header, payload, signatureType)).rejects.toThrow('Unable to retrieve private key');
      });

      it('throws error if public key from SSM is an empty string', async () => {
        getParameterCommand.mockReturnValue('');
        const signatureType = SignatureTypes.EC;

        const jwtAdapter = new JwtAdapter();

        await expect(jwtAdapter.sign(header, payload, signatureType)).rejects.toThrow('Unable to retrieve private key');
      });

      it('throws error if public key from SSM is equal to the default value', async () => {
        getParameterCommand.mockReturnValue('mock-value');
        const signatureType = SignatureTypes.EC;

        const jwtAdapter = new JwtAdapter();
        await expect(jwtAdapter.sign(header, payload, signatureType)).rejects.toThrow('Unable to retrieve private key');
      });
    });

    describe('when private keys are cached', () => {
      it('does not call ssm to retrieve private keys when ec key is cached', async () => {
        const signatureType = SignatureTypes.EC;

        const jwtAdapter = new JwtAdapter();
        jwtAdapter.signingKeyMap.set('EC', 'ecPrivateKey');

        const token = await jwtAdapter.sign(header, payload, signatureType);

        expect(token).toEqual('jwtHeader.jwtPayload.jwtSignature');
        expect(getParameterCommand).not.toHaveBeenCalled();
      });

      it('does not call ssm to retrieve private keys when rsa key is cached', async () => {
        const signatureType = SignatureTypes.RSA;

        const jwtAdapter = new JwtAdapter();
        jwtAdapter.signingKeyMap.set('RSA', 'rsaPrivateKey');

        const token = await jwtAdapter.sign(header, payload, signatureType);

        expect(token).toEqual('jwtHeader.jwtPayload.jwtSignature');
        expect(getParameterCommand).not.toHaveBeenCalled();
      });
    });

    describe('when signing token', () => {
      it('creates RSA token in a jwt format and correct signature', async () => {
        const signatureType = SignatureTypes.RSA;

        const jwtAdapter = new JwtAdapter();
        const token = await jwtAdapter.sign(header, payload, signatureType);

        expect(token).toEqual('jwtHeader.jwtPayload.jwtSignature');
      });

      it('creates EC token in a jwt format and correct signature', async () => {
        const signatureType = SignatureTypes.EC;

        const jwtAdapter = new JwtAdapter();
        const token = await jwtAdapter.sign(header, payload, signatureType);

        expect(token).toEqual('jwtHeader.jwtPayload.jwtSignature');
      });

      it('creates token without a signature when alg claim equals none', async () => {
        header = { alg: Algorithms.NONE };
        const signatureType = SignatureTypes.EC;

        const jwtAdapter = new JwtAdapter();
        const token = await jwtAdapter.sign(header, payload, signatureType);

        expect(token).toEqual('encodedHeader.encodedPayload.');
      });

      it('throws error when token fails to be signed', async () => {
        mockJoseSignJwt.prototype.sign.mockRejectedValue('error!!');
        const signatureType = SignatureTypes.EC;

        const jwtAdapter = new JwtAdapter();

        await expect(jwtAdapter.sign(header, payload, signatureType)).rejects.toThrow('Failed to sign Jwt');
      });
    });
  });
});
