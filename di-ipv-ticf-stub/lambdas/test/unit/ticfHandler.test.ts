import { APIGatewayProxyEvent } from "aws-lambda";
import { getParameter } from "@aws-lambda-powertools/parameters/ssm";

import { buildSignedJwt } from "di-stub-oauth-client";

import { handler } from "../../src/handlers/ticfHandler";

const mockGetParameter = getParameter as jest.Mock;
jest.mock("@aws-lambda-powertools/parameters/ssm", () => ({
  getParameter: jest.fn(),
}));

const mockBuildSignedJwt = buildSignedJwt as jest.Mock;
jest.mock("di-stub-oauth-client", () => ({
  buildSignedJwt: jest.fn(),
}));

describe("Unit test for TICF handler", function () {
  it("verifies successful, 200 response", async () => {
    mockGetParameter
      .mockReturnValueOnce("testSignedKey")
      .mockReturnValueOnce("testComponentId")
      .mockReturnValueOnce("False")
      .mockReturnValueOnce("False");
    mockBuildSignedJwt.mockReturnValueOnce("signed-jwt");

    expect(getParameter).toHaveBeenCalledTimes(0);
    //
    const result = await handler(getTestRequestEvent());
    //
    expect(getParameter).toHaveBeenCalledWith(
      process.env.TICF_PARAM_BASE_PATH + "signingKey"
    );
    expect(getParameter).toHaveBeenCalledWith(
      process.env.TICF_PARAM_BASE_PATH + "timeoutVC"
    );
    expect(getParameter).toHaveBeenCalledWith(
      process.env.TICF_PARAM_BASE_PATH + "includeCIToVC"
    );
    expect(getParameter).toHaveBeenCalledTimes(4);

    expect(buildSignedJwt).toHaveBeenCalledTimes(1);

    expect(result.statusCode).toEqual(200);
    // expect(result.body).toEqual(`JWT VC`);
  });

  it("verifies successful in case of VC with CI, 200 response", async () => {
    mockGetParameter
      .mockReturnValueOnce("testSignedKey")
      .mockReturnValueOnce("testComponentId")
      .mockReturnValueOnce("False")
      .mockReturnValueOnce("True");
    mockBuildSignedJwt.mockReturnValueOnce("signed-jwt");
    //
    const result = await handler(getTestRequestEvent());
    //
    expect(getParameter).toHaveBeenCalledWith(
      process.env.TICF_PARAM_BASE_PATH + "signingKey"
    );
    expect(getParameter).toHaveBeenCalledWith(
      process.env.TICF_PARAM_BASE_PATH + "timeoutVC"
    );
    expect(getParameter).toHaveBeenCalledWith(
      process.env.TICF_PARAM_BASE_PATH + "includeCIToVC"
    );
    expect(getParameter).toHaveBeenCalledTimes(4);

    expect(buildSignedJwt).toHaveBeenCalledTimes(1);

    expect(result.statusCode).toEqual(200);
    // expect(result.body).toEqual(`JWT VC`);
  });

  it("verifies successful in case of timeout VC, 200 response", async () => {
    mockGetParameter
      .mockReturnValueOnce("testSignedKey")
      .mockReturnValueOnce("testComponentId")
      .mockReturnValueOnce("True")
      .mockReturnValueOnce("False");
    mockBuildSignedJwt.mockReturnValueOnce("signed-jwt");
    //
    const result = await handler(getTestRequestEvent());
    //
    expect(getParameter).toHaveBeenCalledWith(
      process.env.TICF_PARAM_BASE_PATH + "signingKey"
    );
    expect(getParameter).toHaveBeenCalledWith(
      process.env.TICF_PARAM_BASE_PATH + "timeoutVC"
    );
    expect(getParameter).toHaveBeenCalledWith(
      process.env.TICF_PARAM_BASE_PATH + "includeCIToVC"
    );
    expect(getParameter).toHaveBeenCalledTimes(4);

    expect(buildSignedJwt).toHaveBeenCalledTimes(1);

    expect(result.statusCode).toEqual(200);
    // expect(result.body).toEqual(`JWT VC`);
  });

  it("verifies bad request with no body, 400 response", async () => {
    expect(getParameter).toHaveBeenCalledTimes(0);
    const event: APIGatewayProxyEvent = {
      body: null,
    } as APIGatewayProxyEvent;
    //
    const result = await handler(event);
    //
    expect(getParameter).toHaveBeenCalledTimes(0);
    expect(buildSignedJwt).toHaveBeenCalledTimes(0);

    expect(result.statusCode).toEqual(400);
  });

  it("verifies bad request fail while parsing, 400 response", async () => {
    jest.spyOn(JSON, "parse").mockImplementationOnce(() => {
      throw new Error();
    });
    expect(getParameter).toHaveBeenCalledTimes(0);
    const event: APIGatewayProxyEvent = {
      body: JSON.stringify({}),
    } as APIGatewayProxyEvent;
    //
    const result = await handler(event);
    //
    expect(getParameter).toHaveBeenCalledTimes(0);
    expect(buildSignedJwt).toHaveBeenCalledTimes(0);

    expect(result.statusCode).toEqual(400);
  });

  it("verifies bad request with missing vot, 400 response", async () => {
    const event: APIGatewayProxyEvent = {
      body: JSON.stringify({
        vtr: ["Cl.Cm.P2"],
        vtm: "https://oidc.account.gov.uk/trustmark",
        sub: "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
        govuk_signin_journey_id: "44444444-4444-4444-4444-444444444444",
        "https://vocab.account.gov.uk/v1/credentialJWT": [
          "<JWT-encoded VC 1>",
          "<JWT-encoded VC 2>",
        ],
      }),
    } as APIGatewayProxyEvent;
    //
    const result = await handler(event);
    //
    expect(getParameter).toHaveBeenCalledTimes(0);
    expect(buildSignedJwt).toHaveBeenCalledTimes(0);

    expect(result.statusCode).toEqual(400);
  });

  it("verifies undefined response from ssm, 500 response", async () => {
    mockGetParameter.mockReturnValueOnce(undefined);
    //
    const result = await handler(getTestRequestEvent());
    //
    expect(getParameter).toHaveBeenCalledTimes(1);
    expect(buildSignedJwt).toHaveBeenCalledTimes(0);

    expect(result.statusCode).toEqual(500);
  });

  it("verifies undefined response from ssm for no componentId, 500 response", async () => {
    mockGetParameter
    .mockReturnValueOnce("testSignedKey")
    .mockRejectedValueOnce(new Error())
    //
    const result = await handler(getTestRequestEvent());
    //
    expect(getParameter).toHaveBeenCalledTimes(2);
    expect(buildSignedJwt).toHaveBeenCalledTimes(0);

    expect(result.statusCode).toEqual(500);
  });

  it("verifies error while building signed JWT, 500 response", async () => {
    mockGetParameter
      .mockReturnValueOnce("testSignedKey")
      .mockReturnValueOnce("testComponentId")
      .mockReturnValueOnce("False")
      .mockReturnValueOnce("False");
    mockBuildSignedJwt.mockImplementation(() => {
      throw new Error();
    });
    //
    const result = await handler(getTestRequestEvent());
    //
    expect(getParameter).toHaveBeenCalledTimes(4);
    expect(buildSignedJwt).toHaveBeenCalledTimes(1);

    expect(result.statusCode).toEqual(500);
  });
});

function getTestRequestEvent(): APIGatewayProxyEvent {
  return {
    body: JSON.stringify({
      vtr: ["Cl.Cm.P2"],
      vot: "P2",
      vtm: "https://oidc.account.gov.uk/trustmark",
      sub: "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
      govuk_signin_journey_id: "44444444-4444-4444-4444-444444444444",
      "https://vocab.account.gov.uk/v1/credentialJWT": [
        "<JWT-encoded VC 1>",
        "<JWT-encoded VC 2>",
      ],
    }),
  } as APIGatewayProxyEvent;
}
