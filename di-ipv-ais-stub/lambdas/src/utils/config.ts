import { getParameter } from "@aws-lambda-powertools/parameters/ssm";

export const getSsmParameter = async (name: string): Promise<string> => {
  const parameter = await getParameter(name);
  if (parameter === undefined) {
    throw new Error(`Could not retrieve ssm parameter: ${name}`);
  }
  return parameter;
};

export const config = {
  environment: process.env.ENVIRONMENT,
  region: process.env.REGION,
  isLocalDev: !!process.env.AWS_SAM_LOCAL,
  localDynamoDbEndpoint: process.env.LOCAL_DYNAMODB_ENDPOINT,
  aisParamBasePath: process.env.AIS_PARAM_BASE_PATH,
  aisStubUserEvidenceTableName:
    process.env.AIS_STUB_ACCOUNT_INTERVENTIONS_TABLE_NAME,
};
