import { handler } from "../../src/handlers/credentialHandler";
import {
  APIGatewayProxyEventHeaders,
  APIGatewayProxyEventV2,
  APIGatewayProxyStructuredResultV2,
} from "aws-lambda";
import CredentialResponse from "../../src/domain/credentialResponse";
import CredentialRequest from "../../src/domain/credentialRequest";

const TEST_CLIENTID = "TEST_CLIENTID";
const TEST_SECRET = "TEST_SECRET"; //pragma: allowlist secret
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
jest.mock("../../src/services/userStateService", () => ({
  persistState: jest.fn(),
}));

describe("DCMAW Async credential handler", function () {
  it("returns a successful pending VC response", async () => {
    // arrange
    const requestBody = getValidRequestBody();
    const event = getValidEvent(requestBody);

    // act
    const result = (await handler(event)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);
    const response = JSON.parse(result.body!) as CredentialResponse;
    expect(response.sub).toEqual(requestBody.sub);
    expect(
      response["https://vocab.account.gov.uk/v1/credentialStatus"],
    ).toEqual("pending");
  });

  it("returns a successful pending VC response when redirect_uri is missing", async () => {
    // arrange
    const requestBody = getValidRequestBody();
    delete requestBody.redirect_uri;
    const event = getValidEvent(requestBody);

    // act
    const result = (await handler(event)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);
    const response = JSON.parse(result.body!) as CredentialResponse;
    expect(response.sub).toEqual(requestBody.sub);
    expect(
      response["https://vocab.account.gov.uk/v1/credentialStatus"],
    ).toEqual("pending");
  });

  it("returns an error for a request with a missing authorization header", async () => {
    // arrange
    const event = getValidEvent(getValidRequestBody());
    delete event.headers["authorization"];

    // act
    const result = (await handler(event)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(401);
    const response = JSON.parse(result.body!) as CredentialResponse;
    expect(response.sub).toBeUndefined();
  });

  it("returns an error for a request with a bad authorization header", async () => {
    // arrange
    const event = getValidEvent(getValidRequestBody());
    event.headers["authorization"] = "bad header";

    // act
    const result = (await handler(event)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(401);
    const response = JSON.parse(result.body!) as CredentialResponse;
    expect(response.sub).toBeUndefined();
  });

  it("returns an error for a request with a bad bearer token", async () => {
    // arrange
    const event = getValidEvent(getValidRequestBody());
    event.headers["authorization"] = "Bearer bad_token";

    // act
    const result = (await handler(event)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(401);
    const response = JSON.parse(result.body!) as CredentialResponse;
    expect(response.sub).toBeUndefined();
  });

  it("returns an error for a request without a body", async () => {
    // arrange
    const event = getValidEvent(getValidRequestBody());
    event.body = undefined;

    // act
    const result = (await handler(event)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
    const response = JSON.parse(result.body!) as CredentialResponse;
    expect(response.sub).toBeUndefined();
  });

  it("returns an error for a request with a missing parameter", async () => {
    // arrange
    const requestBody = getValidRequestBody();
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { sub, ...everythingExceptSub } = requestBody;
    const event = getValidEvent(everythingExceptSub);

    // act
    const result = (await handler(event)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
    const response = JSON.parse(result.body!) as CredentialResponse;
    expect(response.sub).toBeUndefined();
  });

  it("returns an error message containing multiple fields for a request with missing parameters", async () => {
    // arrange
    const requestBody = getValidRequestBody();
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { sub, state, ...everythingExceptSubAndState } = requestBody;
    const event = getValidEvent(everythingExceptSubAndState);

    // act
    const result = (await handler(event)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
    const response = JSON.parse(result.body!);
    expect(response.sub).toBeUndefined();
    expect(response.errorMessage).toContain("sub");
    expect(response.errorMessage).toContain("state");
  });
});

function getValidEvent(requestBody: object): APIGatewayProxyEventV2 {
  return {
    headers: {
      "content-type": "application/json",
      authorization: "Bearer " + TEST_ACCESS_TOKEN,
    } as APIGatewayProxyEventHeaders,
    body: JSON.stringify(requestBody),
  } as APIGatewayProxyEventV2;
}

function getValidRequestBody(): CredentialRequest {
  return {
    sub: "dummy_sub",
    govuk_signin_journey_id: "dummy_journey_id",
    client_id: "dummy_client_id",
    state: "dummy_state",
    redirect_uri: "dummy_redirect_uri",
  };
}
