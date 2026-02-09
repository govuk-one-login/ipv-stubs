import { APIGatewayProxyEvent, APIGatewayProxyEventHeaders } from "aws-lambda";
import { putContraIndicatorsHandler } from "../../../src/internal-api/put-contra-indicators/putContraIndicatorsHandler";
import { addUserCIs } from "../../../src/common/contraIndicatorsService";

const SUCCESS_RESPONSE = '{"result":"success"}';

jest.mock("../../../src/common/contraIndicatorsService", () => ({
  addUserCIs: jest.fn(),
}));

const buildPutContraIndicatorsRequest = (
  headers: APIGatewayProxyEventHeaders = {
    "govuk-signin-journey-id": "journeyId",
    "ip-address": "ip-address",
  },
  body?: object,
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
  const request = buildPutContraIndicatorsRequest(headers, {
    signed_jwt: "signed_jwt",
  });
  // Act
  const response = await putContraIndicatorsHandler(request);
  // Assert
  expect(response.body).toBe(SUCCESS_RESPONSE);
  expect(response.statusCode).toBe(200);
});

test("the handler should error if the request body is null", async () => {
  // Arrange
  const headers = {};
  const body = undefined;
  const request = buildPutContraIndicatorsRequest(headers, body);
  // Act
  const response = await putContraIndicatorsHandler(request);
  // Assert
  expect(JSON.parse(response.body || "").errorMessage).toBe(
    "Missing request body",
  );
  expect(response.statusCode).toBe(400);
});

test("the handler should error if the request body is empty", async () => {
  // Arrange
  const headers = {};
  const body = {};
  const request = buildPutContraIndicatorsRequest(headers, body);
  // Act
  const response = await putContraIndicatorsHandler(request);
  // Assert
  expect(JSON.parse(response.body || "").errorMessage).toBe(
    "signed_jwt is empty",
  );
  expect(response.statusCode).toBe(400);
});

test("the handler should return exception when cimit service throws one", async () => {
  // Arrange
  jest.mocked(addUserCIs).mockImplementation(() => {
    throw new Error(
      "CI Codes could not be inserted into the Cimit Stub Table.",
    );
  });
  const headers = {};
  const body = { signed_jwt: "signed_jwt" };
  const request = buildPutContraIndicatorsRequest(headers, body);
  // Act
  const response = await putContraIndicatorsHandler(request);
  // Assert
  expect(JSON.parse(response.body || "").errorMessage).toBe(
    "CI Codes could not be inserted into the Cimit Stub Table.",
  );
  expect(response.statusCode).toBe(500);
});
