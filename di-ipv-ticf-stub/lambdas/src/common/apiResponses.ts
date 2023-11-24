import { APIGatewayProxyResult } from "aws-lambda";
import TicfResponse from "../domain/ticfResponse";

export const apiResponses = {
  _200: (body: TicfResponse): APIGatewayProxyResult => ({
    statusCode: 200,
    body: JSON.stringify(body, undefined, 2),
  }),
  _400: (body?: { [key: string]: string }): APIGatewayProxyResult => ({
    statusCode: 400,
    body: JSON.stringify(body ?? { errorMessage: "Bad request" }, undefined, 2),
  }),
  _500: (body?: { [key: string]: string }): APIGatewayProxyResult => ({
    statusCode: 500,
    body: JSON.stringify(
      body ?? { errorMessage: "Error while retrieving TicF CRI VC" },
      undefined,
      2
    ),
  }),
};

export default apiResponses;
