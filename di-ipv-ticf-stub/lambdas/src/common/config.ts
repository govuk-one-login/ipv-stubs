export const config = {
  environment: process.env.ENVIRONMENT,
  region: process.env.REGION,
  isLocalDev: !!process.env.AWS_SAM_LOCAL,
  localDynamoDbEndpoint: process.env.LOCAL_DYNAMODB_ENDPOINT,
  ticfParamBasePath: process.env.TICF_PARAM_BASE_PATH,
  ticfStubUserEvidenceTableName: process.env.TICF_STUB_USER_EVIDENCE_TABLE_NAME,
};
