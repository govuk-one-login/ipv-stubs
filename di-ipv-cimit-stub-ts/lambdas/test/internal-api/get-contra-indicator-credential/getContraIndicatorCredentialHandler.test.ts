import {
  APIGatewayProxyEvent,
  APIGatewayProxyEventHeaders,
  APIGatewayProxyEventQueryStringParameters,
  APIGatewayProxyStructuredResultV2,
} from "aws-lambda";
import {
  ContraIndicator,
  getContraIndicatorCredentialHandler,
  GetContraIndicatorCredentialResponse,
  VcClaim,
} from "../../../src/internal-api/get-contra-indicator-credential/getContraIndicatorCredentialHandler";
import {
  getCimitComponentId,
  getCimitSigningKey,
} from "../../../src/common/configService";
import { getCIsForUserID } from "../../../src/common/dataService";
import { decodeJwt, jwtVerify } from "jose";

jest.useFakeTimers().setSystemTime(new Date("2020-01-01"));

const USER_ID = "user_id";
const CI_V03 = "V03";
const ISSUER_1 = "issuer1";
const MITIGATION_M01 = "M01";
const TXN_1 = "1";
const ISSUANCE_DATE_1 = 1577836800; // 01/01/2020 00:00:00
const DOCUMENT_1 = "document_1";
const CIMIT_COMPONENT_ID = "https://cimit.stubs.account.gov.uk";

const CIMIT_PUBLIC_JWK =
  '{"kty":"EC","crv":"P-256","x":"E9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIM","y":"KlTMZthHZUkYz5AleTQ8jff0TJiS3q2OB9L5Fw4xA04"}'; // pragma: allowlist secret

const buildGetContraIndicatorCredentialRequest = (
  headers: APIGatewayProxyEventHeaders = {"govuk-signin-journey-id": "someJourneyId", "ip-address": "someIpAddress"},
  queryStringParameters: APIGatewayProxyEventQueryStringParameters = {user_id: USER_ID},
): APIGatewayProxyEvent => {
  return {
    headers,
    queryStringParameters,
  } as APIGatewayProxyEvent;
};

jest.mock("../../../src/common/configService", () => ({
  getCimitSigningKey: jest.fn(),
  getCimitComponentId: jest.fn(),
}));

jest.mock("../../../src/common/dataService", () => ({
  getCIsForUserID: jest.fn(),
}));

beforeEach(async () => {
  jest.mocked(getCimitComponentId).mockResolvedValue(CIMIT_COMPONENT_ID);
  jest
    .mocked(getCimitSigningKey)
    .mockResolvedValue(
      "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgOXt0P05ZsQcK7eYusgIPsqZdaBCIJiW4imwUtnaAthWhRANCAAQT1nO46ipxVTilUH2umZPN7OPI49GU6Y8YkcqLxFKUgypUzGbYR2VJGM+QJXk0PI339EyYkt6tjgfS+RcOMQNO", // pragma: allowlist secret
    );
});

test("Should return signed JWT containing single mitigation when provided valid request", async () => {
  // Arrange
  jest.mocked(getCIsForUserID).mockResolvedValue([
    {
      userId: USER_ID,
      contraIndicatorCode: CI_V03,
      issuer: ISSUER_1,
      mitigations: [MITIGATION_M01],
      txn: TXN_1,
      issuanceDate: ISSUANCE_DATE_1,
      document: DOCUMENT_1,
    },
  ]);
  const validRequest = buildGetContraIndicatorCredentialRequest();

  // Act
  const response = (await getContraIndicatorCredentialHandler(
    validRequest,
  )) as APIGatewayProxyStructuredResultV2;

  // Assert
  expect(getCimitComponentId).toHaveBeenCalled();
  expect(getCimitSigningKey).toHaveBeenCalled();

  expect(response.statusCode).toBe(200);
  expect(response.body).not.toBeUndefined();

  const parsedBody = (
    response.body ? JSON.parse(response.body) : {}
  ) as GetContraIndicatorCredentialResponse;

  expect(parsedBody.vc).toBeTruthy();

  const parsedJWT = decodeJwt(parsedBody.vc);

  // assert claims on JWT returned from body
  expect(parsedJWT.sub).toEqual(USER_ID);
  expect(parsedJWT.iss).toEqual(CIMIT_COMPONENT_ID);

  // assert properties on VC claim
  const vcClaim = parsedJWT.vc as VcClaim;
  expect(vcClaim.type).toEqual([
    "VerifiableCredential",
    "SecurityCheckCredential",
  ]);

  const contraIndicators = vcClaim.evidence[0].contraIndicator;
  expect(contraIndicators.length).toEqual(1);

  const firstContraIndicator = contraIndicators[0];
  expect(firstContraIndicator.txn).toEqual([TXN_1]);

  const expectedCI: ContraIndicator = {
    code: CI_V03,
    document: DOCUMENT_1,
    issuanceDate: new Date(ISSUANCE_DATE_1 * 1000).toISOString(),
    issuers: [ISSUER_1],
    mitigation: [
      {
        code: MITIGATION_M01,
        mitigatingCredential: [
          {
            issuer: "",
            validFrom: "",
            txn: "",
            id: "",
          },
        ],
      },
    ],
    incompleteMitigation: [],
    txn: [TXN_1],
  };
  expect(firstContraIndicator).toEqual(expectedCI);

  const signingKey = JSON.parse(CIMIT_PUBLIC_JWK);
  const verifyJwtSignature = async () => {
    try {
      await jwtVerify(parsedBody.vc, signingKey, {
        currentDate: new Date(2021, 1, 1, 1, 0, 0, 0),
      });
      return "JWT verified";
    } catch (e) {
      return `Failed JWT verification: ${e}`;
    }
  };

  expect(await verifyJwtSignature()).toEqual("JWT verified");
});

test("Should return unmitigated CI", async () => {
  // Arrange
  jest.mocked(getCIsForUserID).mockResolvedValue([
    {
      userId: USER_ID,
      contraIndicatorCode: CI_V03,
      issuer: ISSUER_1,
      mitigations: [],
      txn: TXN_1,
      issuanceDate: ISSUANCE_DATE_1,
      document: DOCUMENT_1,
    },
  ]);

  const validRequest = buildGetContraIndicatorCredentialRequest();

  // Act
  const response = (await getContraIndicatorCredentialHandler(
    validRequest,
  )) as APIGatewayProxyStructuredResultV2;

  // Assert
  expect(response.statusCode).toBe(200);

  const parsedBody = (
    response.body ? JSON.parse(response.body) : {}
  ) as GetContraIndicatorCredentialResponse;

  const parsedJWT = decodeJwt(parsedBody.vc);

  // assert properties on VC claim
  const vcClaim = parsedJWT.vc as VcClaim;

  const contraIndicators = vcClaim.evidence[0].contraIndicator;
  expect(contraIndicators.length).toEqual(1);

  const firstContraIndicator = contraIndicators[0];

  const expectedCI: ContraIndicator = {
    code: CI_V03,
    document: DOCUMENT_1,
    issuanceDate: new Date(ISSUANCE_DATE_1 * 1000).toISOString(),
    issuers: [ISSUER_1],
    mitigation: [],
    incompleteMitigation: [],
    txn: [TXN_1],
  };
  expect(firstContraIndicator).toEqual(expectedCI);
});

test("Should return two CIs for different documents", async () => {});

test("Should return two CIs for different documents, with one mitigated", async () => {});

test("Should return two CIs for different documents, with both mitigated", async () => {});

test("Should return one CI when same document submitted twice, with the same CI", async () => {});

test("?? Should return two CIs when same document submitted twice, with different CIs", async () => {});

test("Should return the unmitigated CI, when same document submitted twice with the same CIs but the one is mitigated", async () => {});

test("Should return one mitigated CI, when same document submitted twice with the same CIs and both are mitigated", async () => {});

test("Should consolidate duplicate non-doc CIs and keep distinct document CIs separate", async () => {});

test("Should throw 500 for invalid signing key", async () => {});

test.each([
  {
    case: "missing ip-address",
    headers: {
      "govuk-signin-journey-id": "someJourneyId",
    } as APIGatewayProxyEventHeaders,
    queryStringParameters: {
      user_id: "someId",
    } as APIGatewayProxyEventQueryStringParameters,
  },
  {
    case: "missing govuk-signin-journey-id",
    headers: {
      "ip-address": "someIpAddress",
    } as APIGatewayProxyEventHeaders,
    queryStringParameters: {
      user_id: "someId",
    } as APIGatewayProxyEventQueryStringParameters,
  },
  {
    case: "missing userId",
    headers: {
      "ip-address": "someIpAddress",
    } as APIGatewayProxyEventHeaders,
    queryStringParameters: {} as APIGatewayProxyEventQueryStringParameters,
  },
])(
  "Should return 400 for invalid request - $case",
  async ({ headers, queryStringParameters }) => {
    // Arrange
    const input = buildGetContraIndicatorCredentialRequest(
      headers,
      queryStringParameters,
    );

    // Act
    const res = (await getContraIndicatorCredentialHandler(
      input,
    )) as APIGatewayProxyStructuredResultV2;

    // Assert
    expect(res.statusCode).toBe(400);
  },
);

test("Should return 500 for unexpected error", async () => {});
