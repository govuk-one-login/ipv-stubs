import { getParameter } from "@aws-lambda-powertools/parameters/ssm";

const getSsmParameter = async (name: string): Promise<string> => {
  const parameter = await getParameter(name);
  if (parameter === undefined) {
    throw new Error(`Could not retrieve ssm parameter: ${name}`);
  }
  return parameter;
};

export const isRunningLocally = process.env.IS_LOCAL?.toLowerCase() === "true";

const CIMIT_SIGNING_KEY_PARAM = "signingKey";
const CIMIT_COMPONENT_ID_PARAM = "componentId";
const CIMIT_STUB_TTL = "cimitStubTtl";

export const getCimitSigningKey = async () =>
  getSsmParameter(process.env.CIMIT_PARAM_BASE_PATH + CIMIT_SIGNING_KEY_PARAM);
export const getCimitComponentId = async () =>
  getSsmParameter(process.env.CIMIT_PARAM_BASE_PATH + CIMIT_COMPONENT_ID_PARAM);
export const getCimitStubTableName = () =>
  process.env.CIMIT_STUB_TABLE_NAME || "table name not found";
export const getCimitStubTtl = async () =>
  parseInt(
    await getSsmParameter(process.env.CIMIT_PARAM_BASE_PATH + CIMIT_STUB_TTL),
  );
