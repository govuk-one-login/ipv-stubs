import { APIGatewayProxyStructuredResultV2 } from "aws-lambda";

export function buildApiResponse(
  body: object,
  statusCode: number = 200,
): APIGatewayProxyStructuredResultV2 {
  return {
    statusCode,
    body: JSON.stringify(body),
    headers: {
      "content-type": "application/json",
    },
  };
}
