import type { Algorithms, SigningAlgorithms, SignatureTypes } from '../utils/constants.js';

export type AlgType = { [key in SignatureTypes]: Algorithms };
export type SigningAlgorithmType = { [key in SignatureTypes]: SigningAlgorithms };

export interface JwksKeyType {
  kty: SignatureTypes;
  alg: Algorithms;
  kid: string;
}

export type JwtHeader = {
  alg: Algorithms;
  typ?: string;
  kid?: string;
};
