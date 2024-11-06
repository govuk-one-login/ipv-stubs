import { getParameter } from "@aws-lambda-powertools/parameters/ssm";
import getErrorMessage from "./errorReporting";

const CONFIG_PARAMETER_NAME = "config";

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

async function getSsmConfig(basePath: string): Promise<SsmConfig> {
  const parameterPath = basePath + CONFIG_PARAMETER_NAME;

  let configString;
  try {
    configString = await getParameter(parameterPath);
  } catch (error) {
    throw new Error(
      `Error thrown getting parameter ${parameterPath}: ${getErrorMessage(error)}`,
    );
  }

  if (configString === undefined) {
    throw new Error(`Could not retrieve ssm parameter: ${parameterPath}`);
  }

  return Promise.resolve(JSON.parse(configString) as SsmConfig);
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
