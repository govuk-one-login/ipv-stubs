import { getParameter } from "@aws-lambda-powertools/parameters/ssm";

export const getSsmParameter = async (name: string): Promise<string> => {
  console.info(">> Getting value for ssm parameter: " + name);
  // Retrieve a single parameter
  const parameter = await getParameter(name);
  console.info(">> ssm parameter: " + parameter);
  if (parameter === undefined) {
    throw new Error(`Could not retrieve parameter: ${name}`);
  }
  return parameter;
};
