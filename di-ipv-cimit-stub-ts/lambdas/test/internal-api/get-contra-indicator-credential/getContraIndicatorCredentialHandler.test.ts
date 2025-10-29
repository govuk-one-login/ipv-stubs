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
const CI_D02 = "D02";
const ISSUER_1 = "issuer1";
const ISSUER_2 = "issuer2";
const ISSUER_3 = "issuer3";
const ISSUER_4 = "issuer4";
const MITIGATION_M01 = "M01";
const TXN_1 = "1";
const TXN_2 = "2";
const TXN_3 = "3";
const TXN_4 = "4";
const ISSUANCE_DATE_1 = 1577836800; // 01/01/2020 00:00:00
const ISSUANCE_DATE_2 = 1577836900; // 01/01/2020 00:01:40
const ISSUANCE_DATE_3 = 1577836960; // 01/01/2020 00:02:40
const DOCUMENT_1 = "document_1";
const DOCUMENT_2 = "document_2";
const CIMIT_COMPONENT_ID = "https://cimit.stubs.account.gov.uk";

const PARSED_MITIGATION_01 = {
  code: MITIGATION_M01,
  mitigatingCredential: [
    {
      issuer: "",
      validFrom: "",
      txn: "",
      id: "",
    },
  ],
};

const CIMIT_PUBLIC_JWK =
  '{"kty":"EC","crv":"P-256","x":"E9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIM","y":"KlTMZthHZUkYz5AleTQ8jff0TJiS3q2OB9L5Fw4xA04"}'; // pragma: allowlist secret

const buildGetContraIndicatorCredentialRequest = (
  headers: APIGatewayProxyEventHeaders = {
    "govuk-signin-journey-id": "someJourneyId",
    "ip-address": "someIpAddress",
  },
  queryStringParameters: APIGatewayProxyEventQueryStringParameters = {
    user_id: USER_ID,
  },
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
  jest.resetAllMocks();
  jest.mocked(getCimitComponentId).mockResolvedValue(CIMIT_COMPONENT_ID);
  jest.mocked(getCimitSigningKey).mockResolvedValue(
    "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgOXt0P05ZsQcK7eYusgIPsqZdaBCIJiW4imwUtnaAthWhRANCAAQT1nO46ipxVTilUH2umZPN7OPI49GU6Y8YkcqLxFKUgypUzGbYR2VJGM+QJXk0PI339EyYkt6tjgfS+RcOMQNO", // pragma: allowlist secret
  );
});

// test("Should return signed JWT containing single mitigation when provided valid request", async () => {
//   // Arrange
//   jest.mocked(getCIsForUserID).mockResolvedValue([
//     {
//       userId: USER_ID,
//       contraIndicatorCode: CI_V03,
//       issuer: ISSUER_1,
//       mitigations: [MITIGATION_M01],
//       txn: TXN_1,
//       issuanceDate: ISSUANCE_DATE_1,
//       document: DOCUMENT_1,
//     },
//   ]);
//   const validRequest = buildGetContraIndicatorCredentialRequest();

//   // Act
//   const response = (await getContraIndicatorCredentialHandler(
//     validRequest,
//   )) as APIGatewayProxyStructuredResultV2;

//   // Assert
//   expect(getCimitComponentId).toHaveBeenCalled();
//   expect(getCimitSigningKey).toHaveBeenCalled();

//   expect(response.statusCode).toBe(200);
//   expect(response.body).not.toBeUndefined();

//   const parsedBody = (
//     response.body ? JSON.parse(response.body) : {}
//   ) as GetContraIndicatorCredentialResponse;

//   expect(parsedBody.vc).toBeTruthy();

//   const parsedJWT = decodeJwt(parsedBody.vc);

//   // assert claims on JWT returned from body
//   expect(parsedJWT.sub).toEqual(USER_ID);
//   expect(parsedJWT.iss).toEqual(CIMIT_COMPONENT_ID);

//   // assert properties on VC claim
//   const vcClaim = parsedJWT.vc as VcClaim;
//   expect(vcClaim.type).toEqual([
//     "VerifiableCredential",
//     "SecurityCheckCredential",
//   ]);

//   const contraIndicators = vcClaim.evidence[0].contraIndicator;
//   expect(contraIndicators.length).toEqual(1);

//   const firstContraIndicator = contraIndicators[0];
//   expect(firstContraIndicator.txn).toEqual([TXN_1]);

//   const expectedCI: ContraIndicator = {
//     code: CI_V03,
//     document: DOCUMENT_1,
//     issuanceDate: new Date(ISSUANCE_DATE_1 * 1000).toISOString(),
//     issuers: [ISSUER_1],
//     mitigation: [PARSED_MITIGATION_01],
//     incompleteMitigation: [],
//     txn: [TXN_1],
//   };
//   expect(firstContraIndicator).toEqual(expectedCI);

//   const signingKey = JSON.parse(CIMIT_PUBLIC_JWK);
//   const verifyJwtSignature = async () => {
//     try {
//       await jwtVerify(parsedBody.vc, signingKey, {
//         currentDate: new Date(2021, 1, 1, 1, 0, 0, 0),
//       });
//       return "JWT verified";
//     } catch (e) {
//       return `Failed JWT verification: ${e}`;
//     }
//   };

//   expect(await verifyJwtSignature()).toEqual("JWT verified");
// });

test.each([
  // {
  //   testDescription: "return single unmitigated CI",
  //   mockedDatabaseEntry: [
  //     {
  //       userId: USER_ID,
  //       contraIndicatorCode: CI_V03,
  //       issuer: ISSUER_1,
  //       mitigations: [],
  //       txn: TXN_1,
  //       issuanceDate: ISSUANCE_DATE_1,
  //       document: DOCUMENT_1,
  //     },
  //   ],
  //   expectedCIs: [
  //     {
  //       code: CI_V03,
  //       document: DOCUMENT_1,
  //       issuanceDate: new Date(ISSUANCE_DATE_1 * 1000).toISOString(),
  //       issuers: [ISSUER_1],
  //       mitigation: [],
  //       incompleteMitigation: [],
  //       txn: [TXN_1],
  //     },
  //   ],
  // },
  // {
  //   testDescription: "return two CIs for different documents",
  //   mockedDatabaseEntry: [
  //     {
  //       userId: USER_ID,
  //       contraIndicatorCode: CI_V03,
  //       issuer: ISSUER_1,
  //       mitigations: [],
  //       txn: TXN_1,
  //       issuanceDate: ISSUANCE_DATE_1,
  //       document: DOCUMENT_1,
  //     },
  //     {
  //       userId: USER_ID,
  //       contraIndicatorCode: CI_V03,
  //       issuer: ISSUER_2,
  //       mitigations: [],
  //       txn: TXN_2,
  //       issuanceDate: ISSUANCE_DATE_2,
  //       document: DOCUMENT_2,
  //     },
  //   ],
  //   expectedCIs: [
  //     {
  //       code: CI_V03,
  //       document: DOCUMENT_1,
  //       issuanceDate: new Date(ISSUANCE_DATE_1 * 1000).toISOString(),
  //       issuers: [ISSUER_1],
  //       mitigation: [],
  //       incompleteMitigation: [],
  //       txn: [TXN_1],
  //     },
  //     {
  //       code: CI_V03,
  //       document: DOCUMENT_2,
  //       issuanceDate: new Date(ISSUANCE_DATE_2 * 1000).toISOString(),
  //       issuers: [ISSUER_2],
  //       mitigation: [],
  //       incompleteMitigation: [],
  //       txn: [TXN_2],
  //     },
  //   ],
  // },
  // {
  //   testDescription:
  //     "return two CIs for different documents, with one mitigated",
  //   mockedDatabaseEntry: [
  //     {
  //       userId: USER_ID,
  //       contraIndicatorCode: CI_V03,
  //       issuer: ISSUER_1,
  //       mitigations: [],
  //       txn: TXN_1,
  //       issuanceDate: ISSUANCE_DATE_1,
  //       document: DOCUMENT_1,
  //     },
  //     {
  //       userId: USER_ID,
  //       contraIndicatorCode: CI_V03,
  //       issuer: ISSUER_2,
  //       mitigations: [MITIGATION_M01],
  //       txn: TXN_2,
  //       issuanceDate: ISSUANCE_DATE_2,
  //       document: DOCUMENT_2,
  //     },
  //   ],
  //   expectedCIs: [
  //     {
  //       code: CI_V03,
  //       document: DOCUMENT_1,
  //       issuanceDate: new Date(ISSUANCE_DATE_1 * 1000).toISOString(),
  //       issuers: [ISSUER_1],
  //       mitigation: [],
  //       incompleteMitigation: [],
  //       txn: [TXN_1],
  //     },
  //     {
  //       code: CI_V03,
  //       document: DOCUMENT_2,
  //       issuanceDate: new Date(ISSUANCE_DATE_2 * 1000).toISOString(),
  //       issuers: [ISSUER_2],
  //       mitigation: [PARSED_MITIGATION_01],
  //       incompleteMitigation: [],
  //       txn: [TXN_2],
  //     },
  //   ],
  // },
  // {
  //   testDescription:
  //     "return two CIs for different documents, with both mitigated",
  //   mockedDatabaseEntry: [
  //     {
  //       userId: USER_ID,
  //       contraIndicatorCode: CI_V03,
  //       issuer: ISSUER_1,
  //       mitigations: [MITIGATION_M01],
  //       txn: TXN_1,
  //       issuanceDate: ISSUANCE_DATE_1,
  //       document: DOCUMENT_1,
  //     },
  //     {
  //       userId: USER_ID,
  //       contraIndicatorCode: CI_V03,
  //       issuer: ISSUER_2,
  //       mitigations: [MITIGATION_M01],
  //       txn: TXN_2,
  //       issuanceDate: ISSUANCE_DATE_2,
  //       document: DOCUMENT_2,
  //     },
  //   ],
  //   expectedCIs: [
  //     {
  //       code: CI_V03,
  //       document: DOCUMENT_1,
  //       issuanceDate: new Date(ISSUANCE_DATE_1 * 1000).toISOString(),
  //       issuers: [ISSUER_1],
  //       mitigation: [PARSED_MITIGATION_01],
  //       incompleteMitigation: [],
  //       txn: [TXN_1],
  //     },
  //     {
  //       code: CI_V03,
  //       document: DOCUMENT_2,
  //       issuanceDate: new Date(ISSUANCE_DATE_2 * 1000).toISOString(),
  //       issuers: [ISSUER_2],
  //       mitigation: [PARSED_MITIGATION_01],
  //       incompleteMitigation: [],
  //       txn: [TXN_2],
  //     },
  //   ],
  // },
  // {
  //   testDescription:
  //     "return one CI when same document submitted twice, with the same CI",
  //   mockedDatabaseEntry: [
  //     {
  //       userId: USER_ID,
  //       contraIndicatorCode: CI_V03,
  //       issuer: ISSUER_1,
  //       mitigations: [],
  //       txn: TXN_1,
  //       issuanceDate: ISSUANCE_DATE_1,
  //       document: DOCUMENT_1,
  //     },
  //     {
  //       userId: USER_ID,
  //       contraIndicatorCode: CI_V03,
  //       issuer: ISSUER_2,
  //       mitigations: [],
  //       txn: TXN_2,
  //       issuanceDate: ISSUANCE_DATE_2,
  //       document: DOCUMENT_1,
  //     },
  //   ],
  //   expectedCIs: [
  //     {
  //       code: CI_V03,
  //       document: DOCUMENT_1,
  //       issuanceDate: new Date(ISSUANCE_DATE_2 * 1000).toISOString(),
  //       issuers: [ISSUER_1, ISSUER_2],
  //       mitigation: [],
  //       incompleteMitigation: [],
  //       txn: [TXN_2],
  //     },
  //   ],
  // },
  // {
  //   testDescription:
  //     "return the unmitigated CI, when same document submitted twice with the same CIs but the one is mitigated",
  //   mockedDatabaseEntry: [
  //     {
  //       userId: USER_ID,
  //       contraIndicatorCode: CI_V03,
  //       issuer: ISSUER_1,
  //       mitigations: [MITIGATION_M01],
  //       txn: TXN_1,
  //       issuanceDate: ISSUANCE_DATE_1,
  //       document: DOCUMENT_1,
  //     },
  //     {
  //       userId: USER_ID,
  //       contraIndicatorCode: CI_V03,
  //       issuer: ISSUER_2,
  //       mitigations: [],
  //       txn: TXN_2,
  //       issuanceDate: ISSUANCE_DATE_2,
  //       document: DOCUMENT_1,
  //     },
  //   ],
  //   expectedCIs: [
  //     {
  //       code: CI_V03,
  //       document: DOCUMENT_1,
  //       issuanceDate: new Date(ISSUANCE_DATE_2 * 1000).toISOString(),
  //       issuers: [ISSUER_1, ISSUER_2],
  //       mitigation: [],
  //       incompleteMitigation: [],
  //       txn: [TXN_2],
  //     },
  //   ],
  // },
  // {
  //   testDescription:
  //     "return one mitigated CI, when same document submitted twice with the same CIs and both are mitigated",
  //   mockedDatabaseEntry: [
  //     {
  //       userId: USER_ID,
  //       contraIndicatorCode: CI_V03,
  //       issuer: ISSUER_1,
  //       mitigations: [MITIGATION_M01],
  //       txn: TXN_1,
  //       issuanceDate: ISSUANCE_DATE_1,
  //       document: DOCUMENT_1,
  //     },
  //     {
  //       userId: USER_ID,
  //       contraIndicatorCode: CI_V03,
  //       issuer: ISSUER_2,
  //       mitigations: [MITIGATION_M01],
  //       txn: TXN_2,
  //       issuanceDate: ISSUANCE_DATE_2,
  //       document: DOCUMENT_1,
  //     },
  //   ],
  //   expectedCIs: [
  //     {
  //       code: CI_V03,
  //       document: DOCUMENT_1,
  //       issuanceDate: new Date(ISSUANCE_DATE_2 * 1000).toISOString(),
  //       issuers: [ISSUER_1, ISSUER_2],
  //       mitigation: [PARSED_MITIGATION_01],
  //       incompleteMitigation: [],
  //       txn: [TXN_2],
  //     },
  //   ],
  // },
  {
    testDescription:
      "consolidate duplicate non-doc CIs and keep distinct document CIs separate",
    mockedDatabaseEntry: [
      {
        userId: USER_ID,
        contraIndicatorCode: CI_D02,
        issuer: ISSUER_1,
        mitigations: [],
        txn: TXN_1,
        issuanceDate: ISSUANCE_DATE_1,
        document: DOCUMENT_1,
      },
      {
        userId: USER_ID,
        contraIndicatorCode: CI_D02,
        issuer: ISSUER_2,
        mitigations: [],
        txn: TXN_2,
        issuanceDate: ISSUANCE_DATE_3,
        document: DOCUMENT_2,
      },
      {
        userId: USER_ID,
        contraIndicatorCode: CI_V03,
        issuer: ISSUER_3,
        mitigations: [],
        txn: TXN_3,
        issuanceDate: ISSUANCE_DATE_2,
        document: null,
      },
      {
        userId: USER_ID,
        contraIndicatorCode: CI_V03,
        issuer: ISSUER_4,
        mitigations: [],
        txn: TXN_3,
        issuanceDate: ISSUANCE_DATE_3,
        document: null,
      },
    ],
    expectedCIs: [
      {
        code: CI_D02,
        document: DOCUMENT_1,
        issuanceDate: new Date(ISSUANCE_DATE_1 * 1000).toISOString(),
        issuers: [ISSUER_1],
        mitigation: [],
        incompleteMitigation: [],
        txn: [TXN_1],
      },
      {
        code: CI_V03,
        document: null,
        issuanceDate: new Date(ISSUANCE_DATE_3 * 1000).toISOString(),
        issuers: [ISSUER_3, ISSUER_4],
        mitigation: [],
        incompleteMitigation: [],
        txn: [TXN_3],
      },
      {
        code: CI_D02,
        document: DOCUMENT_2,
        issuanceDate: new Date(ISSUANCE_DATE_3 * 1000).toISOString(),
        issuers: [ISSUER_2],
        mitigation: [],
        incompleteMitigation: [],
        txn: [TXN_2],
      },
    ],
  },
])("Should $testDescription", async ({ mockedDatabaseEntry, expectedCIs }) => {
  // Arrange
  jest.mocked(getCIsForUserID).mockResolvedValue(mockedDatabaseEntry);

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

  expect(contraIndicators).toEqual(expectedCIs);
});

//
// test("?? Should return two CIs when same document submitted twice, with different CIs", async () => {});
//
//
// test("Should consolidate duplicate non-doc CIs and keep distinct document CIs separate", async () => {});
//
// test("Should throw 500 for invalid signing key", async () => {});

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

// test("Should return 500 for unexpected error", async () => {});
