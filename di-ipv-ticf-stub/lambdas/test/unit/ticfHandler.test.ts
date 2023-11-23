import { APIGatewayProxyEvent } from "aws-lambda";

import { handler } from "../../src/handlers/ticfHandler";

import * as ssm from "@aws-lambda-powertools/parameters/ssm";

jest.mock('@aws-lambda-powertools/parameters/ssm', () => {
  return { getParameter: jest.fn(() => "false") };
});

jest.mock('stub-oauth-client', () => {
  return { buildSignedJwt: jest.fn(() => "ZZZZZZ") };
});

describe("Unit test for TICF handler", function () {
  it("verifies successful response", async () => {
    const event: APIGatewayProxyEvent = {
      body: JSON.stringify({
          vtr: [
              "Cl.Cm.P2"
          ],
          vot: "P2",
          vtm: "https://oidc.account.gov.uk/trustmark",
          sub: "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
          govuk_signin_journey_id: "44444444-4444-4444-4444-444444444444",
          "https\://vocab.account.gov.uk/v1/credentialJWT": [
              "<JWT-encoded VC 1>",
              "<JWT-encoded VC 2>"
          ]
      })
    } as any;
    //
    const result = await handler(event);
    //
    expect(result.statusCode).toEqual(200);
    // expect(result.body).toEqual(`JWT VC`);
  });
});
