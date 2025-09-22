module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  transformIgnorePatterns: ['node_modules/(?!jose)'],
  resolver: 'ts-jest-resolver',
  transform: {
    '^.+\\.(ts|js)$': [
      'ts-jest',
      {
        isolatedModules: true,
      },
    ],
  },
};

process.env['EC_PRIVATE_KEY_SSM_NAME'] = 'ecPrivateKeyName'; // pragma: allowlist secret
process.env['RSA_PRIVATE_KEY_SSM_NAME'] = 'rsaPrivateKeyName'; // pragma: allowlist secret
process.env['EC_PUBLIC_KEY_SSM_NAME'] = 'ecPublicKeyName';
process.env['RSA_PUBLIC_KEY_SSM_NAME'] = 'rsaPublicKeyName';
process.env['DEFAULT_SSM_VALUE'] = "mock-value";
process.env['DEFAULT_ISSUER'] = 'mock-issuer';
process.env['DEFAULT_AUDIENCE'] = 'mock-audience';
process.env['PERCENTAGE_RETURN_4XX'] = '0';
process.env['PERCENTAGE_RETURN_5XX'] = '0';
process.env['PERCENTAGE_TIMEOUT'] = '0';
process.env['PERCENTAGE_DELAY'] = '0';
process.env['MAXIMUM_DELAY_MILLISECONDS'] = '0';
