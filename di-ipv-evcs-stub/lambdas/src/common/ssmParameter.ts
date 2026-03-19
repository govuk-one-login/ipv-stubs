import { GetParameterCommandOutput } from "@aws-sdk/client-ssm";

export const getSsmParameter = async (name: string): Promise<string> => {
  const encodedParamName = encodeURIComponent(name);
  const url = `http://localhost:2773/systemsmanager/parameters/get?name=${encodedParamName}`;

  const response = await fetch(url, {
    headers: {
      "X-Aws-Parameters-Secrets-Token": process.env.AWS_SESSION_TOKEN || "",
    },
  });

  const data = (await response.json()) as GetParameterCommandOutput;
  if (!data.Parameter?.Value) {
    throw new Error(`Could not retrieve ssm parameter: ${name}`);
  }

  return data.Parameter.Value;
};
