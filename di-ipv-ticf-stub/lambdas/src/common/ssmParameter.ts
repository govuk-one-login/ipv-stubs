import { getParameter } from "@aws-lambda-powertools/parameters/ssm";

export const getSsmParameter = async (
  name: string
): Promise<string | undefined> => {
  console.info(">> Getting value for ssm parameter: " + name);
  let parameter;
  try {
    // Retrieve a single parameter
    parameter = await getParameter(name);
  } catch (error) {
    console.error(`>> Error retriving parameter. Error: ${error}`);
  }
  return parameter;
};
