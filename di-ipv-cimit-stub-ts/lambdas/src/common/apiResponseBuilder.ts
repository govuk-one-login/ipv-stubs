import { APIGatewayProxyStructuredResultV2 } from "aws-lambda";

export function buildApiResponse(
  statusCode: number = 200,
  body?: object,
): APIGatewayProxyStructuredResultV2 {
  return {
    statusCode,
    body: body && JSON.stringify(body),
    headers: body ? { "content-type": "application/json" } : {},
  };
}

export function getErrorMessage(error: unknown) {
  if (error instanceof Error) return error.message;
  return String(error);
}
