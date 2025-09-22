import { JwtAdapter } from '../../utils/jwt-adapter.js';
import { handle } from '../token-generator.js';
import { Algorithms, Kids } from '../../utils/constants.js';
import type { JwtHeader } from '../../types/token.js';
import type { JWTPayload } from 'jose';
import { Context } from "aws-lambda";

jest.mock('../../utils/jwt-adapter');
jest.mock('../../utils/logger');
jest.mock('../../utils/app-config');
const mockContext = {} as unknown as Context
const mockJwtAdapter = JwtAdapter as jest.Mock;
const mockJwtAdapterSignMethod = JwtAdapter.prototype.sign as jest.Mock;

describe('token-generator', () => {
  let event: any;
  let expectedHeader: JwtHeader = { alg: Algorithms.EC };
  let expectedPayload: JWTPayload = {};

  beforeAll(() => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date(Date.UTC(2023, 2, 13)));
  });

  beforeEach(() => {
    mockJwtAdapter.mockClear();
    mockJwtAdapterSignMethod.mockReturnValue('a.b.c');
    jest.setSystemTime(new Date(Date.UTC(2023, 2, 13)));
    event = '{}';

    expectedHeader = {
      typ: 'JWT',
      alg: Algorithms.EC,
      kid: Kids.EC,
    };

    expectedPayload = {
      iss: 'https://mock-issuer/orchestration',
      aud: 'mock-audience',
      scope: 'proving',
      exp: 1_678_665_900,
      iat: 1_678_665_600,
    };
  });

  afterAll(() => {
    jest.useRealTimers();
  });

  it('creates a valid header and payload for EC token by default', async () => {
    const expectedSignatureType = 'EC';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates a valid header and payload for RSA token', async () => {
    const queryStringParameters = { signatureType: 'RSA' };
    event = { ...event, queryStringParameters };

    expectedHeader = {
      ...expectedHeader,
      alg: Algorithms.RSA,
      kid: Kids.RSA,
    };

    const expectedSignatureType = 'RSA';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an valid header and payload with expiry set to 20 mins for EC token', async () => {
    const body = JSON.stringify({ ttl: 20 });
    event = { ...event, body };

    const expectedExp = expectedPayload.iat! + 20 * 60;

    expectedPayload = { ...expectedPayload, exp: expectedExp };

    const expectedSignatureType = 'EC';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an valid EC header and payload with expiry set to default 5 mins when a non-numeric ttl is passed in payload', async () => {
    const body = JSON.stringify({ ttl: 'not-a-number' });
    event = { ...event, body };

    const expectedSignatureType = 'EC';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an valid header and payload with iat set to 20 mins in the past for EC token', async () => {
    const body = JSON.stringify({ iat: 20 });
    event = { ...event, body };

    const expectedIat = expectedPayload.exp! - 25 * 60;

    expectedPayload = { ...expectedPayload, iat: expectedIat };

    const expectedSignatureType = 'EC';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an valid EC header and payload with iat set to default 0 mins when a non-numeric int is passed in payload', async () => {
    const body = JSON.stringify({ iat: 'not-a-number' });
    event = { ...event, body };

    const expectedSignatureType = 'EC';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an valid header and payload with expiry set to 20 mins for RSA token', async () => {
    const queryStringParameters = { signatureType: 'RSA' };
    event = { ...event, queryStringParameters };

    const body = JSON.stringify({ ttl: 20 });
    event = { ...event, body };

    expectedHeader = {
      ...expectedHeader,
      alg: Algorithms.RSA,
      kid: Kids.RSA,
    };

    const expectedExp = expectedPayload.iat! + 20 * 60;
    expectedPayload = { ...expectedPayload, exp: expectedExp };

    const expectedSignatureType = 'RSA';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an valid RSA header and payload with expiry set to default 5 mins when a non-numeric ttl is passed in payload', async () => {
    const queryStringParameters = { signatureType: 'RSA' };
    event = { ...event, queryStringParameters };

    const body = JSON.stringify({ ttl: 'not-a-number' });
    event = { ...event, body };

    expectedHeader = {
      ...expectedHeader,
      alg: Algorithms.RSA,
      kid: Kids.RSA,
    };

    const expectedSignatureType = 'RSA';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an valid header and payload with iat set to 20 mins in the past for RSA token', async () => {
    const queryStringParameters = { signatureType: 'RSA' };
    event = { ...event, queryStringParameters };

    const body = JSON.stringify({ iat: 20 });
    event = { ...event, body };

    expectedHeader = {
      ...expectedHeader,
      alg: Algorithms.RSA,
      kid: Kids.RSA,
    };

    const expectedIat = expectedPayload.exp! - 25 * 60;
    expectedPayload = { ...expectedPayload, iat: expectedIat };

    const expectedSignatureType = 'RSA';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an valid RSA header and payload with iat set to default 0 mins when a non-numeric int is passed in payload', async () => {
    const queryStringParameters = { signatureType: 'RSA' };
    event = { ...event, queryStringParameters };

    const body = JSON.stringify({ iat: 'not-a-number' });
    event = { ...event, body };

    expectedHeader = {
      ...expectedHeader,
      alg: Algorithms.RSA,
      kid: Kids.RSA,
    };

    const expectedSignatureType = 'RSA';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid header for EC token with invalid algorithm', async () => {
    const queryStringParameters = { tokenType: 'invalidAlg' };
    event = { ...event, queryStringParameters };

    expectedHeader = {
      ...expectedHeader,
      alg: Algorithms.INVALID,
    };

    const expectedSignatureType = 'EC';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid header for EC token with none algorithm', async () => {
    const queryStringParameters = { tokenType: 'noneAlg' };
    event = { ...event, queryStringParameters };

    expectedHeader = {
      ...expectedHeader,
      alg: Algorithms.NONE,
    };

    const expectedSignatureType = 'EC';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid header for EC token with missing kid', async () => {
    const queryStringParameters = { tokenType: 'missingKid' };
    event = { ...event, queryStringParameters };

    delete expectedHeader.kid;

    const expectedSignatureType = 'EC';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid header for EC token with wrong kid', async () => {
    const queryStringParameters = { tokenType: 'wrongKid' };
    event = { ...event, queryStringParameters };

    expectedHeader = { ...expectedHeader, kid: Kids.WRONG };

    const expectedSignatureType = 'EC';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid payload for EC token with expired exp', async () => {
    const queryStringParameters = { tokenType: 'expired' };
    event = { ...event, queryStringParameters };

    const exp = expectedPayload.iat! - 300;

    expectedPayload = { ...expectedPayload, exp };

    const expectedSignatureType = 'EC';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid payload for EC token with expired exp when tokenType expired and ttl in payload both present', async () => {
    const queryStringParameters = { tokenType: 'expired' };
    const body = JSON.stringify({ ttl: 20 });
    event = { ...event, queryStringParameters, body };

    const exp = expectedPayload.iat! - 300;

    expectedPayload = { ...expectedPayload, exp };

    const expectedSignatureType = 'EC';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid payload for EC token with iat in the future', async () => {
    const queryStringParameters = { tokenType: 'iatInFuture' };
    event = { ...event, queryStringParameters };

    const iat = expectedPayload.iat! + 300;

    expectedPayload = { ...expectedPayload, iat };

    const expectedSignatureType = 'EC';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid payload for EC token with no aud', async () => {
    const body = JSON.stringify({ aud: null });
    event = { ...event, body };

    delete expectedPayload.aud;

    const expectedSignatureType = 'EC';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid payload for EC token with invalid aud', async () => {
    const body = JSON.stringify({ aud: 'wrongAudience' });
    event = { ...event, body };

    expectedPayload = { ...expectedPayload, aud: 'wrongAudience' };

    const expectedSignatureType = 'EC';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid payload for EC token with no iss', async () => {
    const body = JSON.stringify({ iss: null });
    event = { ...event, body };

    delete expectedPayload.iss;

    const expectedSignatureType = 'EC';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid payload for EC token with invalid iss', async () => {
    const body = JSON.stringify({ iss: 'wrongIssuer' });
    event = { ...event, body };

    expectedPayload = { ...expectedPayload, iss: 'wrongIssuer' };

    const expectedSignatureType = 'EC';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid header for RSA token with invalid algorithm', async () => {
    const queryStringParameters = { signatureType: 'RSA', tokenType: 'invalidAlg' };
    event = { ...event, queryStringParameters };

    expectedHeader = {
      ...expectedHeader,
      alg: Algorithms.INVALID,
      kid: Kids.RSA,
    };

    const expectedSignatureType = 'RSA';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid header for RSA token with none algorithm', async () => {
    const queryStringParameters = { signatureType: 'RSA', tokenType: 'noneAlg' };
    event = { ...event, queryStringParameters };

    expectedHeader = {
      ...expectedHeader,
      alg: Algorithms.NONE,
      kid: Kids.RSA,
    };

    const expectedSignatureType = 'RSA';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid header for RSA token with missing kid', async () => {
    const queryStringParameters = { signatureType: 'RSA', tokenType: 'missingKid' };
    event = { ...event, queryStringParameters };

    expectedHeader = {
      ...expectedHeader,
      alg: Algorithms.RSA,
    };

    delete expectedHeader.kid;

    const expectedSignatureType = 'RSA';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid header for RSA token with wrong kid', async () => {
    const queryStringParameters = { signatureType: 'RSA', tokenType: 'wrongKid' };
    event = { ...event, queryStringParameters };

    expectedHeader = { ...expectedHeader, alg: Algorithms.RSA, kid: Kids.WRONG };

    const expectedSignatureType = 'RSA';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid payload for RSA token with expired exp', async () => {
    const queryStringParameters = { signatureType: 'RSA', tokenType: 'expired' };
    event = { ...event, queryStringParameters };

    expectedHeader = { ...expectedHeader, alg: Algorithms.RSA, kid: Kids.RSA };
    const exp = expectedPayload.iat! - 300;

    expectedPayload = { ...expectedPayload, exp };

    const expectedSignatureType = 'RSA';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid payload for RSA token with expired exp when tokenType expired and ttl in payload both present', async () => {
    const queryStringParameters = { signatureType: 'RSA', tokenType: 'expired' };
    const body = JSON.stringify({ ttl: 20 });
    event = { ...event, queryStringParameters, body };

    expectedHeader = { ...expectedHeader, alg: Algorithms.RSA, kid: Kids.RSA };
    const exp = expectedPayload.iat! - 300;

    expectedPayload = { ...expectedPayload, exp };

    const expectedSignatureType = 'RSA';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid payload for RSA token with iat in the future', async () => {
    const queryStringParameters = { signatureType: 'RSA', tokenType: 'iatInFuture' };
    event = { ...event, queryStringParameters };

    expectedHeader = { ...expectedHeader, alg: Algorithms.RSA, kid: Kids.RSA };
    const iat = expectedPayload.iat! + 300;

    expectedPayload = { ...expectedPayload, iat };

    const expectedSignatureType = 'RSA';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid payload for RSA token with no aud', async () => {
    const queryStringParameters = { signatureType: 'RSA' };
    const body = JSON.stringify({ aud: null });
    event = { ...event, body, queryStringParameters };

    expectedHeader = { ...expectedHeader, alg: Algorithms.RSA, kid: Kids.RSA };
    delete expectedPayload.aud;

    const expectedSignatureType = 'RSA';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid payload for RSA token with invalid aud', async () => {
    const queryStringParameters = { signatureType: 'RSA' };
    const body = JSON.stringify({ aud: 'wrongAudience' });
    event = { ...event, body, queryStringParameters };

    expectedHeader = { ...expectedHeader, alg: Algorithms.RSA, kid: Kids.RSA };
    expectedPayload = { ...expectedPayload, aud: 'wrongAudience' };

    const expectedSignatureType = 'RSA';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid payload for RSA token with no iss', async () => {
    const queryStringParameters = { signatureType: 'RSA' };
    const body = JSON.stringify({ iss: null });
    event = { ...event, body, queryStringParameters };

    expectedHeader = { ...expectedHeader, alg: Algorithms.RSA, kid: Kids.RSA };
    delete expectedPayload.iss;

    const expectedSignatureType = 'RSA';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('creates an invalid payload for RSA token with invalid iss', async () => {
    const queryStringParameters = { signatureType: 'RSA' };
    const body = JSON.stringify({ iss: 'wrongIssuer' });
    event = { ...event, body, queryStringParameters };

    expectedHeader = { ...expectedHeader, alg: Algorithms.RSA, kid: Kids.RSA };
    expectedPayload = { ...expectedPayload, iss: 'wrongIssuer' };

    const expectedSignatureType = 'RSA';

    await handle(event, mockContext);

    expect(mockJwtAdapterSignMethod).toHaveBeenCalledWith(
      expectedHeader,
      expectedPayload,
      expectedSignatureType,
    );
  });

  it('returns status 200 and token when JwtAdaptor successfully creates token', async () => {
    const expectedResponse = { statusCode: 200, body: JSON.stringify({ token: 'a.b.c' }) };

    const response = await handle(event, mockContext);

    expect(response).toEqual(expectedResponse);
  });

  it("throws error if body is passed a string that isn't valid json", async () => {
    const body = 'notValidJson';
    event = { ...event, body };
    await expect(handle(event, mockContext)).rejects.toThrow('Event body is not valid JSON so cannot be parsed');
  });

  it('throws error when JwtAdaptor does not create a token', async () => {
    mockJwtAdapterSignMethod.mockReturnValue(undefined);
    await expect(handle(event, mockContext)).rejects.toThrow('Token not generated');
  });

  it('throws error when JwtAdaptor sign method fails', async () => {
    mockJwtAdapterSignMethod.mockRejectedValue('error');
    await expect(handle(event, mockContext)).rejects.toThrow('Failed to sign token');
  });
});
