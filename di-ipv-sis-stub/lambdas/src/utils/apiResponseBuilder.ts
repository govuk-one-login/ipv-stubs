import { APIGatewayProxyResultV2 } from "aws-lambda";

export const buildApiResponse = (
  statusCode: number = 200,
  body?: object,
): APIGatewayProxyResultV2 => {
  return {
    statusCode,
    body: body && JSON.stringify(body),
    headers: body ? { "content-type": "application/json" } : {},
  };
};
