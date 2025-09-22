import { SignatureTypes } from './constants.js';

export const DEFAULT_ISSUER = (('https://' + process.env['DEFAULT_ISSUER']) as string) + '/orchestration';
export const AUTHENTICATION_ISSUER = 'authentication-issuer';

export const DEFAULT_AUDIENCE = process.env['DEFAULT_AUDIENCE'] as string;

export const getPrivateKeyName = (keyType: SignatureTypes, iss: string): string => {
  let keyEnvironment = keyType == SignatureTypes.EC ? 'EC_PRIVATE_KEY_SSM_NAME' : 'RSA_PRIVATE_KEY_SSM_NAME';
  if (iss === AUTHENTICATION_ISSUER) keyEnvironment = `${keyEnvironment}_AUTHENTICATION`;
  return process.env[keyEnvironment] as string;
};

export const getPublicKeyName = (keyType: SignatureTypes, iss: string): string => {
  let keyEnvironment = keyType == SignatureTypes.EC ? 'EC_PUBLIC_KEY_SSM_NAME' : 'RSA_PUBLIC_KEY_SSM_NAME';
  if (iss === AUTHENTICATION_ISSUER) keyEnvironment = `${keyEnvironment}_AUTHENTICATION`;
  return process.env[keyEnvironment] as string;
};

export const getDefaultKeyValue = () => {
  return process.env['DEFAULT_SSM_VALUE'] as string;
};

export const getPercentageReturn4xx = () => {
  return getPositiveNumber('Percentage', 'PERCENTAGE_RETURN_4XX', 1);
};

export const getPercentageReturn5xx = () => {
  return getPositiveNumber('Percentage', 'PERCENTAGE_RETURN_5XX', 1);
};

export const getPercentageTimeout = () => {
  return getPositiveNumber('Percentage', 'PERCENTAGE_TIMEOUT', 1);
};

export const getPercentageDelay = () => {
  return getPositiveNumber('Percentage', 'PERCENTAGE_DELAY', 1);
};

export const getMaximumDelayMilliseconds = () => {
  return getPositiveNumber('Maximum delay', 'MAXIMUM_DELAY_MILLISECONDS');
};

/**
 * A function for getting the positive numbers.
 *
 * @param numberLabel - the number.
 * @param environmentVariable - the environment variable.
 * @param upperLimit - the limit.
 * @returns A positive number.
 */

function getPositiveNumber(numberLabel: string, environmentVariable: string, upperLimit?: number) {
  const number = +(process.env[environmentVariable] || 0);
  if (Number.isNaN(number)) {
    throw new TypeError(`${numberLabel} value must be a number.`);
  }
  if (number < 0 || (upperLimit && number > upperLimit)) {
    const message = upperLimit
      ? `${numberLabel} value must be between 0.00 and ${upperLimit.toFixed(2)}.`
      : `${numberLabel} cannot be a negative number.`;
    throw new Error(message);
  }
  return number;
}
