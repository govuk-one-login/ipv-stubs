import { handler } from "../../src/handlers/tokenHandler";
import {
  APIGatewayProxyEventHeaders,
  APIGatewayProxyEventV2,
  APIGatewayProxyStructuredResultV2,
} from "aws-lambda";
import { toBase64 } from "../helpers/base64";
import TokenResponse from "../../src/domain/tokenResponse";

const TEST_CLIENTID = "TEST_CLIENTID";
const TEST_SECRET = "TEST_SECRET";
const TEST_ACCESS_TOKEN = "TEST_ACCESS_TOKEN";
const TOKEN_LIFETIME = 200;

jest.mock(
  "../../src/common/config",
  () => () =>
    Promise.resolve({
      dcmawAsyncParamBasePath: "TEST_BASE_PATH",
      dummyAccessTokenValue: TEST_ACCESS_TOKEN,
      dummyClientId: TEST_CLIENTID,
      dummySecret: TEST_SECRET,
      tokenLifetimeSeconds: TOKEN_LIFETIME,
    }),
);

describe("DCMAW Async credential handler", function () {
  it("returns a token for a valid request", async () => {
    // act
    const result = (await handler(
      getValidEvent(),
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);
    const response = JSON.parse(result.body!) as TokenResponse;
    expect(response.access_token).toEqual(TEST_ACCESS_TOKEN);
    expect(response.token_type).toEqual("Bearer");
    expect(response.expires_in).toEqual(TOKEN_LIFETIME);
  });

  it("returns an error for a request with a bad client ID", async () => {
    // arrange
    const event = getValidEvent();
    event.headers["authorization"] =
      "Basic " + toBase64(`badClientId:${TEST_SECRET}`);

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
      "Basic " + toBase64(`${TEST_CLIENTID}:bad_secret`);

    // act
    const result = (await handler(event)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(401);
    const response = JSON.parse(result.body!) as TokenResponse;
    expect(response.access_token).toBeUndefined();
  });

  it("returns an error for a request without and authorisation header", async () => {
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
      authorization: "Basic " + toBase64(`${TEST_CLIENTID}:${TEST_SECRET}`),
    } as APIGatewayProxyEventHeaders,
    body: "grant_type=client_credentials",
  } as APIGatewayProxyEventV2;
}
