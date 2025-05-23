import { getParameter } from "@aws-lambda-powertools/parameters/ssm";

export const getSsmParameter = async (name: string): Promise<string> => {
  const parameter = await getParameter(name, { maxAge: 60 });
  if (parameter === undefined) {
    throw new Error(`Could not retrieve ssm parameter: ${name}`);
  }
  return parameter;
};
