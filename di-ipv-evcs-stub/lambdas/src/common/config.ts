export const config = {
  environment: process.env.ENVIRONMENT,
  region: process.env.REGION,
  isLocalDev: !!process.env.AWS_SAM_LOCAL,
  localDynamoDbEndpoint: process.env.LOCAL_DYNAMODB_ENDPOINT,
  evcsParamBasePath: process.env.EVCS_PARAM_BASE_PATH,
  evcsStubUserVCsTableName: process.env.EVCS_STUB_USER_VCS_STORE_TABLE_NAME,
  evcsStoredIdentityObjectTableName:
    process.env.EVCS_STUB_STORED_IDENTITY_OBJECT_TABLE_NAME,
};
