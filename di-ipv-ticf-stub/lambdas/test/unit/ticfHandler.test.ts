import { APIGatewayProxyEventV2, APIGatewayProxyStructuredResultV2 } from "aws-lambda";
import { getParameter } from "@aws-lambda-powertools/parameters/ssm";
import { handler } from "../../src/handlers/ticfHandler";
import TicfResponse from "../../src/domain/ticfResponse";
import { importSPKI, jwtVerify } from "jose";
import TicfVc from "../../src/domain/ticfVc";
import config from "../../src/common/config";

jest.mock("@aws-lambda-powertools/parameters/ssm", () => ({
  getParameter: jest.fn(),
}));

jest.mock("../../src/common/config", () => ({
  default: {
    ssmBasePath: '/test/path',
  },
}));

const EC_PRIVATE_KEY = 'MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgkGU1Xuq6ntjxOPqqk5Q/Qq+JpZsGJ6b6TRcD969CEsuhRANCAATXraAdaAWfQhMjaOT9TVWzmbiJZZLxwx1sShONadRgP+4WWaqxNlUgoAAYYdDEfVJMTOuumLfeRhuLYCKrJd8R';
const EC_PUBLIC_KEY = 'MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE162gHWgFn0ITI2jk/U1Vs5m4iWWS8cMdbEoTjWnUYD/uFlmqsTZVIKAAGGHQxH1STEzrrpi33kYbi2AiqyXfEQ==';

const TEST_COMPONENT_ID = 'https://example.com';

const TEST_REQUEST = {
  vtr: ["Cl.Cm.P2"],
  vot: "P2",
  vtm: "https://oidc.account.gov.uk/trustmark",
  sub: "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
  govuk_signin_journey_id: "44444444-4444-4444-4444-444444444444",
  "https://vocab.account.gov.uk/v1/credentialJWT": [],
};

const TEST_EVENT = {
  body: JSON.stringify(TEST_REQUEST),
} as APIGatewayProxyEventV2;

async function parseTicfVc(jwt: string): Promise<TicfVc> {
  const key = await importSPKI(
    `-----BEGIN PUBLIC KEY-----\n${EC_PUBLIC_KEY}\n-----END PUBLIC KEY-----`,
    "ES256"
  );
  return (await jwtVerify(jwt, key)).payload as TicfVc;
}

describe("TICF handler", function () {
  it("returns a successful VC response", async () => {
    // arrange
    jest.mocked(getParameter)
      .mockResolvedValueOnce(EC_PRIVATE_KEY)
      .mockResolvedValueOnce(TEST_COMPONENT_ID)
      .mockResolvedValueOnce("false")
      .mockResolvedValueOnce("false");

    // act
    const result = await handler(TEST_EVENT) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);

    const response = JSON.parse(result.body!) as TicfResponse;
    expect(response.vtr).toEqual(TEST_REQUEST.vtr);
    expect(response.vot).toEqual(TEST_REQUEST.vot);
    expect(response.vtm).toEqual(TEST_REQUEST.vtm);
    expect(response.sub).toEqual(TEST_REQUEST.sub);
    expect(response.govuk_signin_journey_id).toEqual(TEST_REQUEST.govuk_signin_journey_id);
    expect(response["https://vocab.account.gov.uk/v1/credentialJWT"]).toHaveLength(1);

    const ticfVc = await parseTicfVc(response["https://vocab.account.gov.uk/v1/credentialJWT"][0]);
    expect(ticfVc.iss).toEqual(TEST_COMPONENT_ID);
    expect(ticfVc.sub).toEqual(TEST_REQUEST.sub);
    expect(ticfVc.aud).toEqual(TEST_COMPONENT_ID);
    expect(ticfVc.vc.type).toEqual(['VerifiableCredential', 'RiskAssessmentCredential']);
    expect(ticfVc.vc.evidence).toHaveLength(1);
    expect(ticfVc.vc.evidence[0].type).toEqual('RiskAssessment');
    expect(ticfVc.vc.evidence[0].txn).toBeTruthy();
  });

  it("returns a VC with CI when includeCIToVC is true", async () => {
    // arrange
    jest.mocked(getParameter)
      .mockResolvedValueOnce(EC_PRIVATE_KEY)
      .mockResolvedValueOnce(TEST_COMPONENT_ID)
      .mockResolvedValueOnce("false")
      .mockResolvedValueOnce("true");

    // act
    const result = await handler(TEST_EVENT) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);

    const response = JSON.parse(result.body!) as TicfResponse;
    const ticfVc = await parseTicfVc(response["https://vocab.account.gov.uk/v1/credentialJWT"][0]);
    expect(ticfVc.vc.evidence).toHaveLength(1);
    expect(ticfVc.vc.evidence[0].type).toEqual('RiskAssessment');
    expect(ticfVc.vc.evidence[0].txn).toBeDefined();
    expect(ticfVc.vc.evidence[0].ci).toEqual(['V03']);
  });

  it("returns an empty timeout VC when timeoutVC is true", async () => {
    // arrange
    jest.mocked(getParameter)
      .mockResolvedValueOnce(EC_PRIVATE_KEY)
      .mockResolvedValueOnce(TEST_COMPONENT_ID)
      .mockResolvedValueOnce("true")
      .mockResolvedValueOnce("false");

    // act
    const result = await handler(TEST_EVENT) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);

    const response = JSON.parse(result.body!) as TicfResponse;
    const ticfVc = await parseTicfVc(response["https://vocab.account.gov.uk/v1/credentialJWT"][0]);
    expect(ticfVc.vc.evidence).toHaveLength(1);
    expect(ticfVc.vc.evidence[0].type).toEqual('RiskAssessment');
    expect(ticfVc.vc.evidence[0].txn).toBeUndefined();
  });

  it("returns a 400 for an empty request", async () => {
    // arrange
    const event = {
      ...TEST_EVENT,
      body: undefined,
    };

    // act
    const result = await handler(event) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
  });

  it("returns a 400 for an invalid request", async () => {
    // arrange
    const event = {
      ...TEST_EVENT,
      body: 'invalid json',
    };

    // act
    const result = await handler(event) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
  });

  it("returns a 400 for a missing vot", async () => {
    // arrange
    const event = {
      ...TEST_EVENT,
      body: JSON.stringify({
        ...TEST_REQUEST,
        vot: undefined,
      }),
    };

    // act
    const result = await handler(event) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
  });

  it("returns a 500 for missing SSM parameter", async () => {
    // arrange
    jest.mocked(getParameter).mockResolvedValueOnce(undefined);

    // act
    const result = await handler(TEST_EVENT) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(500);
  });
});
