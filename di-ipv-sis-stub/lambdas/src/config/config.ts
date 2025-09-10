export const config = {
  environment: process.env.ENVIRONMENT,
  region: process.env.REGION,
  isLocalDev: !!process.env.AWS_SAM_LOCAL,
  localDynamoDbEndpoint: process.env.LOCAL_DYNAMODB_ENDPOINT,
  evcsStoredIdentityObjectTableName: process.env.EVCS_STORED_IDENTITY_TABLE,
  evcsParamBasePath: process.env.EVCS_PARAM_BASE_PATH,
  sisSigningKeyId: process.env.SIS_SIGNING_KEY_ID,
  didStoredIdentityId: process.env.DID_STORED_IDENTITY_ID,
  sisSigningKey: process.env.SIS_SIGNING_KEY,
};
