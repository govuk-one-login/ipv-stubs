import { APIGatewayProxyResultV2 } from "aws-lambda";

export function buildApiResponse(
  body: object,
  statusCode: number = 200
): APIGatewayProxyResultV2 {
  return {
    statusCode,
    body: JSON.stringify(body),
    headers: {
      "content-type": "application/json",
    },
  };
}
