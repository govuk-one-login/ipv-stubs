import { getParameter } from "@aws-lambda-powertools/parameters/ssm";

export const getSsmParameter = async (name: string): Promise<string> => {
  const parameter = await getParameter(name);
  if (parameter === undefined) {
    throw new Error(`Could not retrieve ssm parameter: ${name}`);
  }
  return parameter;
};

const CIMIT_SIGNING_KEY_PARAM = "signingKey";
export const getCimitSigningKey = async () =>
  getSsmParameter(process.env.CIMIT_PARAM_BASE_PATH + CIMIT_SIGNING_KEY_PARAM);
const CIMIT_COMPONENT_ID_PARAM = "componentId";
export const getCimitComponentId = async () =>
  getSsmParameter(process.env.CIMIT_PARAM_BASE_PATH + CIMIT_COMPONENT_ID_PARAM);
