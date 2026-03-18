import { describe, it, expect, vi } from "vitest";
import { handler } from "../../src/handlers/tokenHandler";
import {
  APIGatewayProxyEventHeaders,
  APIGatewayProxyEventV2,
  APIGatewayProxyStructuredResultV2,
} from "aws-lambda";
import { toBase64 } from "../helpers/base64";
import TokenResponse from "../../src/domain/tokenResponse";

const mockedVals = vi.hoisted(() => ({
  clientId: "TEST_CLIENTID",
  secret: "TEST_SECRET", //pragma: allowlist secret
  accessToken: "TEST_ACCESS_TOKEN",
  tokenLifetime: 200,
}));

vi.mock("../../src/common/config", () => ({
  default: vi.fn().mockResolvedValue({
    dcmawAsyncParamBasePath: "TEST_BASE_PATH",
    dummyAccessTokenValue: mockedVals.accessToken,
    dummyClientId: mockedVals.clientId,
    dummySecret: mockedVals.secret,
    tokenLifetimeSeconds: mockedVals.tokenLifetime,
  }),
}));

describe("DCMAW Async token handler", () => {
  it("returns a token for a valid request", async () => {
    // act
    const result = (await handler(
      getValidEvent(),
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);
    const response = JSON.parse(result.body!) as TokenResponse;
    expect(response.access_token).toEqual(mockedVals.accessToken);
    expect(response.token_type).toEqual("Bearer");
    expect(response.expires_in).toEqual(mockedVals.tokenLifetime);
  });

  it("returns an error for a request with a bad client ID", async () => {
    // arrange
    const event = getValidEvent();
    event.headers["authorization"] =
      "Basic " + toBase64(`badClientId:${mockedVals.secret}`);

    // act
    const result = (await handler(event)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(401);
    const response = JSON.parse(result.body!) as TokenResponse;
    expect(response.access_token).toBeUndefined();
  });

  it("returns an error for a request with a bad secret", async () => {
    // arrange
    const event = getValidEvent();
    event.headers["authorization"] =
      "Basic " + toBase64(`${mockedVals.clientId}:bad_secret`);

    // act
    const result = (await handler(event)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(401);
    const response = JSON.parse(result.body!) as TokenResponse;
    expect(response.access_token).toBeUndefined();
  });

  it("returns an error for a request without an authorisation header", async () => {
    // arrange
    const event = getValidEvent();
    event.headers["authorization"] = undefined;

    // act
    const result = (await handler(event)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(401);
    const response = JSON.parse(result.body!) as TokenResponse;
    expect(response.access_token).toBeUndefined();
  });

  it("returns an error for a request without the correct body", async () => {
    // arrange
    const event = getValidEvent();
    event.body = "bad_body";

    // act
    const result = (await handler(event)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
    const response = JSON.parse(result.body!) as TokenResponse;
    expect(response.access_token).toBeUndefined();
  });
});

function getValidEvent(): APIGatewayProxyEventV2 {
  return {
    headers: {
      "content-type": "application/x-www/form-urlencoded",
      authorization:
        "Basic " + toBase64(`${mockedVals.clientId}:${mockedVals.secret}`),
    } as APIGatewayProxyEventHeaders,
    body: "grant_type=client_credentials",
  } as APIGatewayProxyEventV2;
}
