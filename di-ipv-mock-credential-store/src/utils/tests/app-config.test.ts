import {
  getPrivateKeyName,
  getPublicKeyName,
  getDefaultKeyValue,
  getPercentageReturn4xx,
  getPercentageReturn5xx,
  getPercentageTimeout,
  getPercentageDelay,
  getMaximumDelayMilliseconds,
} from '../app-config.js';
import { SignatureTypes } from '../constants.js';

describe('app-config', () => {
  describe('getPrivateKeyName', () => {
    it('return EC private key name when passed EC', () => {
      const privateKeyName = getPrivateKeyName(SignatureTypes.EC, "iss");
      expect(privateKeyName).toEqual('ecPrivateKeyName');
    });

    it('return RSA private key name when passed RSA', () => {
      const privateKeyName = getPrivateKeyName(SignatureTypes.RSA, "iss");
      expect(privateKeyName).toEqual('rsaPrivateKeyName');
    });
  });

  describe('getPublicKeyName', () => {
    it('return EC public key name when passed EC', () => {
      const publicKeyName = getPublicKeyName(SignatureTypes.EC, "iss");
      expect(publicKeyName).toEqual('ecPublicKeyName');
    });

    it('return RSA public key name when passed RSA', () => {
      const publicKeyName = getPublicKeyName(SignatureTypes.RSA,"iss");
      expect(publicKeyName).toEqual('rsaPublicKeyName');
    });
  });

  describe('getDefaultKeyValue', () => {
    it('returns the default key value', () => {
      const defaultKeyValue = getDefaultKeyValue();
      expect(defaultKeyValue).toEqual('mock-value');
    });
  });

  describe('getPercentageReturn4xx', () => {
    it('returns percentage value when value is between 0 and 1', () => {
      process.env['PERCENTAGE_RETURN_4XX'] = '0.3';
      const percentage = getPercentageReturn4xx();
      expect(percentage).toEqual(0.3);
    });

    it('throws error when value is less than 0', async () => {
      process.env['PERCENTAGE_RETURN_4XX'] = '-0.1';
      let percentage;
      try {
        percentage = getPercentageReturn4xx();
      } catch (error: unknown) {
        if (error instanceof Error) {
          percentage = error.message;
        }
      }
      expect(percentage).toEqual('Percentage value must be between 0.00 and 1.00.');
    });

    it('throws error when value is greater than 1', async () => {
      process.env['PERCENTAGE_RETURN_4XX'] = '1.1';
      let percentage;
      try {
        percentage = getPercentageReturn4xx();
      } catch (error: unknown) {
        if (error instanceof Error) {
          percentage = error.message;
        }
      }
      expect(percentage).toEqual('Percentage value must be between 0.00 and 1.00.');
    });

    it('throws error when value is not a number', () => {
      process.env['PERCENTAGE_RETURN_4XX'] = 'notAnInteger';
      let percentage;
      try {
        percentage = getPercentageReturn4xx();
      } catch (error: unknown) {
        if (error instanceof Error) {
          percentage = error.message;
        }
      }
      expect(percentage).toEqual('Percentage value must be a number.');
    });
  });

  describe('getPercentageReturn5xx', () => {
    it('returns percentage value when value is between 0 and 1', () => {
      process.env['PERCENTAGE_RETURN_5XX'] = '0.3';
      const percentage = getPercentageReturn5xx();
      expect(percentage).toEqual(0.3);
    });

    it('throws error when value is less than 0', async () => {
      process.env['PERCENTAGE_RETURN_5XX'] = '-0.1';
      let percentage;
      try {
        percentage = getPercentageReturn5xx();
      } catch (error: unknown) {
        if (error instanceof Error) {
          percentage = error.message;
        }
      }
      expect(percentage).toEqual('Percentage value must be between 0.00 and 1.00.');
    });

    it('throws error when value is greater than 1', async () => {
      process.env['PERCENTAGE_RETURN_5XX'] = '1.1';
      let percentage;
      try {
        percentage = getPercentageReturn5xx();
      } catch (error: unknown) {
        if (error instanceof Error) {
          percentage = error.message;
        }
      }
      expect(percentage).toEqual('Percentage value must be between 0.00 and 1.00.');
    });

    it('throws error when value is not a number', () => {
      process.env['PERCENTAGE_RETURN_5XX'] = 'notAnInteger';
      let percentage;
      try {
        percentage = getPercentageReturn5xx();
      } catch (error: unknown) {
        if (error instanceof Error) {
          percentage = error.message;
        }
      }
      expect(percentage).toEqual('Percentage value must be a number.');
    });
  });

  describe('getPercentageTimeout', () => {
    it('returns percentage value when value is between 0 and 1', () => {
      process.env['PERCENTAGE_TIMEOUT'] = '0.3';
      const percentage = getPercentageTimeout();
      expect(percentage).toEqual(0.3);
    });

    it('throws error when value is less than 0', async () => {
      process.env['PERCENTAGE_TIMEOUT'] = '-0.1';
      let percentage;
      try {
        percentage = getPercentageTimeout();
      } catch (error: unknown) {
        if (error instanceof Error) {
          percentage = error.message;
        }
      }
      expect(percentage).toEqual('Percentage value must be between 0.00 and 1.00.');
    });

    it('throws error when value is greater than 1', async () => {
      process.env['PERCENTAGE_TIMEOUT'] = '1.1';
      let percentage;
      try {
        percentage = getPercentageTimeout();
      } catch (error: unknown) {
        if (error instanceof Error) {
          percentage = error.message;
        }
      }
      expect(percentage).toEqual('Percentage value must be between 0.00 and 1.00.');
    });

    it('throws error when value is not a number', () => {
      process.env['PERCENTAGE_TIMEOUT'] = 'notAnInteger';
      let percentage;
      try {
        percentage = getPercentageTimeout();
      } catch (error: unknown) {
        if (error instanceof Error) {
          percentage = error.message;
        }
      }
      expect(percentage).toEqual('Percentage value must be a number.');
    });
  });

  describe('getPercentageDelay', () => {
    it('returns percentage value when value is between 0 and 1', () => {
      process.env['PERCENTAGE_DELAY'] = '0.3';
      const percentage = getPercentageDelay();
      expect(percentage).toEqual(0.3);
    });

    it('throws error when value is less than 0', async () => {
      process.env['PERCENTAGE_DELAY'] = '-0.1';
      let percentage;
      try {
        percentage = getPercentageDelay();
      } catch (error: unknown) {
        if (error instanceof Error) {
          percentage = error.message;
        }
      }
      expect(percentage).toEqual('Percentage value must be between 0.00 and 1.00.');
    });

    it('throws error when value is greater than 1', async () => {
      process.env['PERCENTAGE_DELAY'] = '1.1';
      let percentage;
      try {
        percentage = getPercentageDelay();
      } catch (error: unknown) {
        if (error instanceof Error) {
          percentage = error.message;
        }
      }
      expect(percentage).toEqual('Percentage value must be between 0.00 and 1.00.');
    });

    it('throws error when value is not a number', () => {
      process.env['PERCENTAGE_DELAY'] = 'notAnInteger';
      let percentage;
      try {
        percentage = getPercentageDelay();
      } catch (error: unknown) {
        if (error instanceof Error) {
          percentage = error.message;
        }
      }
      expect(percentage).toEqual('Percentage value must be a number.');
    });
  });

  describe('getMaximumDelayMilliseconds', () => {
    it('returns the delay value when environment variable is a positive number', () => {
      process.env['MAXIMUM_DELAY_MILLISECONDS'] = '1000';
      const delay = getMaximumDelayMilliseconds();
      expect(delay).toEqual(1000);
    });

    it('throws error if the delay value when environment variable is a negative number', () => {
      process.env['MAXIMUM_DELAY_MILLISECONDS'] = '-1000';
      let delay;
      try {
        delay = getMaximumDelayMilliseconds();
      } catch (error: unknown) {
        if (error instanceof Error) {
          delay = error.message;
        }
      }
      expect(delay).toEqual('Maximum delay cannot be a negative number.');
    });

    it('throws error if the delay value when environment variable is not a integer string', () => {
      process.env['MAXIMUM_DELAY_MILLISECONDS'] = 'notAnInteger';
      let delay;
      try {
        delay = getMaximumDelayMilliseconds();
      } catch (error: unknown) {
        if (error instanceof Error) {
          delay = error.message;
        }
      }
      expect(delay).toEqual('Maximum delay value must be a number.');
    });
  });
});
