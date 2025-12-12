import {
  APIGatewayProxyEvent,
  APIGatewayProxyEventHeaders,
  Context,
} from "aws-lambda";
import { putContraIndicatorsHandler } from "../../../src/internal-api/put-contra-indicators/putContraIndicatorsHandler";

const mockCimitStubItemService = {};
const mockContext = jest.fn() as unknown as Context;

const SUCCESS_RESPONSE = "{\"result\":\"success\"}";
const FAILURE_RESPONSE = "{\"result\":\"fail\"}";



const buildPutContraIndicatorsRequest = (
  headers: APIGatewayProxyEventHeaders = {
    "govuk-signin-journey-id": "journeyId",
    "ip-address": "ip-address",
  },
  body?: postMitigationsRequestBody,
): APIGatewayProxyEvent => {
  return {
    headers,
    body: body ? JSON.stringify(body) : undefined,
  } as APIGatewayProxyEvent;
};

beforeEach(() => {
  jest.resetAllMocks();
});

test("the handler should return success if there is a body", async () => {
  // Arrange
  const headers = {};
  const request = buildPutContraIndicatorsRequest(
    headers,
    { signed_jwt: "signed_jwt" }
  );

  // Act
  const response = await putContraIndicatorsHandler(request, mockContext);

  // Assert
  expect(response.body).toBe(SUCCESS_RESPONSE);
  expect(response.statusCode).toBe(200);
});