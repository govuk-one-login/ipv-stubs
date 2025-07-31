import { SSMProvider } from "@aws-lambda-powertools/parameters/ssm";

const CONFIG_PARAMETER_NAME = "config";
const ssmProvider = new SSMProvider();


interface SsmConfig {
  dummyClientId: string;
  dummySecret: string;
  dummyAccessTokenValue: string;
  tokenLifetimeSeconds: number;
  vcIssuer: string;
  vcAudience: string;
  vcSigningKey: string;
  queueStubUrl: string;
  queueStubApiKey: string;
  queueName: string;
}

interface Config extends SsmConfig {
  dcmawAsyncParamBasePath: string;
}

export async function getSsmConfig(basePath: string): Promise<SsmConfig> {
  const parameterPath = basePath + CONFIG_PARAMETER_NAME;
  const parameters = await ssmProvider.getMultiple(parameterPath);

  if (!parameters) {
    throw new Error(`No parameters found under path: ${parameterPath}`);
  }

  const get = (key: string): string => {
    const fullKey = `${parameterPath}/${key}`;
    const value = parameters[fullKey];
    if (value === undefined) throw new Error(`Missing parameter: ${fullKey}`);
    return value;
  };

  return {
    dummyClientId: get("dummyClientId"),
    dummySecret: get("dummySecret"),
    dummyAccessTokenValue: get("dummyAccessTokenValue"),
    tokenLifetimeSeconds: Number(get("tokenLifetimeSeconds")),
    vcIssuer: get("vcIssuer"),
    vcAudience: get("vcAudience"),
    vcSigningKey: get("vcSigningKey"),
    queueStubUrl: get("queueStubUrl"),
    queueStubApiKey: get("queueStubApiKey"),
    queueName: get("queueName"),
  };
}

export function getEnvironmentVariable(variableName: string): string {
  const variableValue = process.env[variableName];
  if (variableValue === undefined) {
    throw new Error(`Environment variable ${variableName} not set`);
  }
  return variableValue;
}

export default async function getConfig(): Promise<Config> {
  const paramBasePath = getEnvironmentVariable("DCMAW_ASYNC_PARAM_BASE_PATH");

  const ssmConfig = await getSsmConfig(paramBasePath);

  return Promise.resolve({
    dcmawAsyncParamBasePath: paramBasePath,
    dummyAccessTokenValue: ssmConfig.dummyAccessTokenValue,
    dummyClientId: ssmConfig.dummyClientId,
    dummySecret: ssmConfig.dummySecret,
    tokenLifetimeSeconds: ssmConfig.tokenLifetimeSeconds,
    vcIssuer: ssmConfig.vcIssuer,
    vcAudience: ssmConfig.vcAudience,
    vcSigningKey: ssmConfig.vcSigningKey,
    queueStubUrl: ssmConfig.queueStubUrl,
    queueStubApiKey: ssmConfig.queueStubApiKey,
    queueName: ssmConfig.queueName,
  });
}
