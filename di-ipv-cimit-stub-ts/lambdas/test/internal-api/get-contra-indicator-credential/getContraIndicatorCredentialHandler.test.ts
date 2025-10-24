import {
  APIGatewayProxyEvent,
  APIGatewayProxyEventHeaders, APIGatewayProxyEventQueryStringParameters,
  APIGatewayProxyStructuredResultV2
} from "aws-lambda";
import {
  getContraIndicatorCredentialHandler
} from "../../../src/internal-api/get-contra-indicator-credential/getContraIndicatorCredentialHandler";
import { getCimitComponentId, getCimitSigningKey } from "../../../src/common/configService";

const buildGetContraIndicatorCredentialRequest = (
  headers: APIGatewayProxyEventHeaders = {"govuk-signin-journey-id": "someJourneyId", "ip-address": "someIpAddress"},
  queryStringParameters: APIGatewayProxyEventQueryStringParameters = {user_id: "someId"},
): APIGatewayProxyEvent => {
  return {
    headers,
    queryStringParameters,
  } as APIGatewayProxyEvent;
};

jest.mock("../src/common/configService", () => ({
  getCimitSigningKey: jest.fn(),
  getCimitComponentId: jest.fn(),
}));


beforeEach(async () => {
  jest.mocked(getCimitComponentId).mockResolvedValue("https://cimit.stubs.account.gov.uk");
  jest.mocked(getCimitSigningKey).mockResolvedValue("MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgOXt0P05ZsQcK7eYusgIPsqZdaBCIJiW4imwUtnaAthWhRANCAAQT1nO46ipxVTilUH2umZPN7OPI49GU6Y8YkcqLxFKUgypUzGbYR2VJGM"); // pragma: allowlist secret

})

test("Should return signed JWT when provided valid request", async () => {
  // Arrange
  const validRequest = buildGetContraIndicatorCredentialRequest();

  // build a datastore service and mock a call to it here
  // check size and parameters of CIs returned

  // Act
  const res = (await getContraIndicatorCredentialHandler(validRequest)) as APIGatewayProxyStructuredResultV2;

  // Assert
  expect(res.statusCode).toBe(200);

  const expected = {vc: "someVc"};
  expect(res.body).toEqual(JSON.stringify(expected));

  // verify the signature of the JWT
})

test("Should return unmitigated CI", async () => {})

test("Should return mitigated CI", async () => {})

test("Should return two CIs for different documents", async () => {})

test("Should return two CIs for different documents, with one mitigated", async () => {})

test("Should return two CIs for different documents, with both mitigated", async () => {})

test("Should return one CI when same document submitted twice, with the same CI", async () => {})

test("?? Should return two CIs when same document submitted twice, with different CIs", async () => {})

test("Should return the unmitigated CI, when same document submitted twice with the same CIs but the one is mitigated", async () => {})

test("Should return one mitigated CI, when same document submitted twice with the same CIs and both are mitigated", async () => {})

test("Should consolidate duplicate non-doc CIs and keep distinct document CIs separate", async () => {})

test("Should throw 500 for invalid signing key", async () => {})

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