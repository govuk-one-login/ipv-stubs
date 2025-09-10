export const config = {
  environment: process.env.ENVIRONMENT,
  region: process.env.REGION,
  isLocalDev: !!process.env.AWS_SAM_LOCAL,
  localDynamoDbEndpoint: process.env.LOCAL_DYNAMODB_ENDPOINT,
  evcsStoredIdentityObjectTableName: process.env.EVCS_STORED_IDENTITY_TABLE,
  evcsParamBasePath: process.env.EVCS_PARAM_BASE_PATH,
  sisParamsBasePath: process.env.SIS_PARAM_BASE_PATH,
};
