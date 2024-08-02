import { APIGatewayProxyResultV2 } from "aws-lambda";

export const jsonResponse = (
  statusCode: number,
  body: object,
): APIGatewayProxyResultV2 => ({
  statusCode,
  headers: {
      "Content-Type": "application/json"
  },
  isBase64Encoded: false,
  body: JSON.stringify(body),
});
