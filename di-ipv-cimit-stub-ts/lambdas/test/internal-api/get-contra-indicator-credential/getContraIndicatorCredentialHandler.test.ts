import {
  APIGatewayProxyEvent,
  APIGatewayProxyEventHeaders, APIGatewayProxyEventQueryStringParameters,
  APIGatewayProxyStructuredResultV2
} from "aws-lambda";
import {
  getContraIndicatorCredentialHandler
} from "../../../src/internal-api/get-contra-indicator-credential/getContraIndicatorCredentialHandler";

const buildGetContraIndicatorCredentialRequest = (
  headers: APIGatewayProxyEventHeaders = {"govuk-signin-journey-id": "someJourneyId", "ip-address": "someIpAddress"},
  queryStringParameters: APIGatewayProxyEventQueryStringParameters = {user_id: "someId"},
): APIGatewayProxyEvent => {
  return {
    headers,
    queryStringParameters,
  } as APIGatewayProxyEvent;
};

test("Should return signed JWT when provided valid request", async () => {
  // Arrange
  const validRequest = buildGetContraIndicatorCredentialRequest();

  // Act
  const res = (await getContraIndicatorCredentialHandler(validRequest)) as APIGatewayProxyStructuredResultV2;

  // Assert
  expect(res.statusCode).toBe(200);

  const expected = {vc: "someVc"};
  expect(res.body).toEqual(JSON.stringify(expected));
})

test("Should return unmitigated CI")

test("Should return mitigated CI")

test("Should return two CIs for different documents")

test("Should return two CIs for different documents, with one mitigated")

test("Should return two CIs for different documents, with both mitigated")

test("Should return one CI when same document submitted twice, with the same CI")

test("?? Should return two CIs when same document submitted twice, with different CIs")

test("Should return the unmitigated CI, when same document submitted twice with the same CIs but the one is mitigated")

test("Should return one mitigated CI, when same document submitted twice with the same CIs and both are mitigated")

test("Should consolidate duplicate non-doc CIs and keep distinct document CIs separate")

test("Should throw 500 for invalid signing key")

test.each([
  {
    case: "missing ip-address",
    headers: {
      "govuk-signin-journey-id": "someJourneyId"
    } as APIGatewayProxyEventHeaders,
    queryStringParameters: { user_id: "someId" } as APIGatewayProxyEventQueryStringParameters
  },
  {
    case: "missing govuk-signin-journey-id",
    headers: {
      "ip-address": "someIpAddress"
    } as APIGatewayProxyEventHeaders,
    queryStringParameters: { user_id: "someId" } as APIGatewayProxyEventQueryStringParameters
  },
  {
    case: "missing userId",
    headers: {
      "ip-address": "someIpAddress"
    } as APIGatewayProxyEventHeaders,
    queryStringParameters: {} as APIGatewayProxyEventQueryStringParameters
  }
])("Should return 400 for invalid request - $case", async ({headers, queryStringParameters}) => {
  // Arrange
  const input = buildGetContraIndicatorCredentialRequest(headers, queryStringParameters);

  // Act
  const res = (await getContraIndicatorCredentialHandler(input)) as APIGatewayProxyStructuredResultV2;

  // Assert
  expect(res.statusCode).toBe(400);
})

test("Should return 500 for unexpected error", async () => {});