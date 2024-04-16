import { getParameter } from "@aws-lambda-powertools/parameters/lib/ssm";

const CONFIG_PARAMETER_NAME = "config";

interface SsmConfig {
  dummyClientId: string;
  dummySecret: string;
  dummyAccessTokenValue: string;
  tokenLifetimeSeconds: number;
}

interface Config extends SsmConfig {
  dcmawAsyncParamBasePath: string;
}

async function getSsmConfig(basePath: string): Promise<SsmConfig> {
  const parameterPath = basePath + CONFIG_PARAMETER_NAME;
  const configString = await getParameter(parameterPath);
  if (configString === undefined) {
    throw new Error(`Could not retrieve ssm parameter: ${parameterPath}`);
  }

  return JSON.parse(configString) as SsmConfig;
}

function getEnvironmentVariable(variableName: string): string {
  const variableValue = process.env[variableName];
  if (variableValue === undefined) {
    throw new Error(`Environment variable ${variableName} not set`);
  }
  return variableValue;
}

export default async function getConfig(): Promise<Config> {
  const paramBasePath = getEnvironmentVariable("DCMAW_ASYNC_PARAM_BASE_PATH");

  const ssmConfig = await getSsmConfig(paramBasePath);

  return {
    dcmawAsyncParamBasePath: paramBasePath,
    dummyAccessTokenValue: ssmConfig.dummyAccessTokenValue,
    dummyClientId: ssmConfig.dummyClientId,
    dummySecret: ssmConfig.dummySecret,
    tokenLifetimeSeconds: ssmConfig.tokenLifetimeSeconds,
  };
}
