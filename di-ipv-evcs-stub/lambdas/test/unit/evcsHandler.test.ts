import { DynamoDB } from "@aws-sdk/client-dynamodb";
import { DynamoDBDocumentClient, GetCommand } from "@aws-sdk/lib-dynamodb";
import {
  APIGatewayProxyEvent,
  APIGatewayProxyEventHeaders,
  APIGatewayProxyEventPathParameters,
  APIGatewayProxyEventQueryStringParameters,
  APIGatewayProxyStructuredResultV2,
} from "aws-lambda";

import {
  createHandler,
  getHandler,
  updateHandler,
} from "../../src/handlers/evcsHandler";
import { getParameter } from "@aws-lambda-powertools/parameters/ssm";
import { VcState } from "../../src/domain/enums/vcState";
import VCProvenance from "../../src/domain/enums/vcProvenance";

jest.mock("@aws-lambda-powertools/parameters/ssm", () => ({
  getParameter: jest.fn(),
}));

const testUserId: string = "urn%3Auuid%3Atest-user-id";
const decodedTestUserId: string = decodeURIComponent(testUserId);

const dbConfig = {
  convertEmptyValues: true,
  endpoint: "http://localhost:8000",
  sslEnabled: false,
  region: "local-env",
};

const dynamoClient = new DynamoDB(dbConfig);
const dynamoDocClient = DynamoDBDocumentClient.from(dynamoClient);
const getCommand = new GetCommand({
  TableName: "evcs-stub-user-vcs-store",
  Key: {
    userId: decodedTestUserId,
    vcSignature:
      "qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ",
  },
});

jest.mock("../../src/common/config", () => ({
  config: {
    evcsParamBasePath: "/test/path/",
    evcsStubUserVCsTableName: "evcs-stub-user-vcs-store",
    localDynamoDbEndpoint: "http://localhost:8000",
    region: "local-dev",
    isLocalDev: true,
  },
}));

const EVCS_VERIFY_KEY =
  "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEE9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIMqVMxm2EdlSRjPkCV5NDyN9/RMmJLerY4H0vkXDjEDTg==";

const TEST_POST_REQUEST = [
  {
    vc: "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ",
    state: VcState.CURRENT,
    metadata: {
      reason: "test-created",
      timestampMs: "1714478033959",
      txmaEventId: "txma-event-id",
      testProperty: "testProperty",
    },
  },
  {
    vc: "zzJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.zf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ",
    state: VcState.CURRENT,
    provenance: VCProvenance.MIGRATED,
  },
  {
    vc: "yyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.tf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ",
    state: VcState.VERIFICATION,
  },
  {
    vc: "ddJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.df0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ",
    state: VcState.PENDING_RETURN,
  },
];
const TEST_POST_INVALID_STATE_REQUEST = [
  {
    vc: "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ",
    state: VcState.HISTORIC,
  },
];

const TEST_NO_VC_REQUEST: never[] = [];

const TEST_PATH_PARAM = {
  userId: testUserId,
} as APIGatewayProxyEventPathParameters;

const TEST_QUERY_SPECIFIC_STATE_PARAM = {
  state: VcState.CURRENT.concat(",").concat(VcState.VERIFICATION),
} as unknown as APIGatewayProxyEventQueryStringParameters;

const TEST_QUERY_ALL_STATE_PARAM = {
  state: "ALL",
} as unknown as APIGatewayProxyEventQueryStringParameters;

const TEST_QUERY_INVALID_STATE_PARAM = {
  state: "ALL, APPROVED",
} as unknown as APIGatewayProxyEventQueryStringParameters;

const TEST_POST_EVENT = {
  body: JSON.stringify(TEST_POST_REQUEST),
  pathParameters: TEST_PATH_PARAM,
} as APIGatewayProxyEvent;
const TEST_POST_INVALID_STATE_EVENT = {
  body: JSON.stringify(TEST_POST_INVALID_STATE_REQUEST),
  pathParameters: TEST_PATH_PARAM,
} as APIGatewayProxyEvent;

const TEST_HEADERS = {
  Authorisation: `Bearer eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2V2Y3MuYnVpbGQuc3R1YnMuYWNjb3VudC5nb3YudWsiLCJzdWIiOiJ1cm46dXVpZDpkMTgyMzA2Ni0yMTM3LTQzODAtYjBiYS00YjYxOTQ3ZTA4ZTYiLCJhdWQiOiJodHRwczovL2V2Y3MuYnVpbGQuc3R1YnMuYWNjb3VudC5nb3YudWsiLCJqdGkiOiJ1cm46dXVpZDpiNmRkMjNkMy1mZjM3LTQzYzYtOTI3My01NTRkNjQzMjFiODMiLCJuYmYiOjE3MTUxNjU0NjksImlhdCI6MTcxMjU3MzQ2OX0.1-nRkV6ny9ThBGDbQ1sDCrJpYSe0tbOXEMJJNEoomVWjKsRL1RK6qdATkk-54p_c68Gzu1mN4FDM-buk1gXIPQ`,
} as APIGatewayProxyEventHeaders;

const TEST_GET_EVENT = {
  pathParameters: TEST_PATH_PARAM,
  headers: TEST_HEADERS,
} as APIGatewayProxyEvent;

const TEST_GET_EVENT_WITH_SPECIFIC_STATE_IN_QUERY = {
  pathParameters: TEST_PATH_PARAM,
  queryStringParameters: TEST_QUERY_SPECIFIC_STATE_PARAM,
  headers: TEST_HEADERS,
} as APIGatewayProxyEvent;

const TEST_GET_EVENT_WITH_ALL_STATE_IN_QUERY = {
  pathParameters: TEST_PATH_PARAM,
  queryStringParameters: TEST_QUERY_ALL_STATE_PARAM,
  headers: TEST_HEADERS,
} as APIGatewayProxyEvent;

const TEST_GET_EVENT_WITH_INVALID_STATE_IN_QUERY = {
  pathParameters: TEST_PATH_PARAM,
  queryStringParameters: TEST_QUERY_INVALID_STATE_PARAM,
  headers: TEST_HEADERS,
} as APIGatewayProxyEvent;

const TEST_NO_VC_EVENT = {
  body: JSON.stringify(TEST_NO_VC_REQUEST),
  pathParameters: TEST_PATH_PARAM,
} as APIGatewayProxyEvent;

const TEST_WITHOUT_USER_PARAM_EVENT = {
  body: JSON.stringify(TEST_POST_REQUEST),
} as APIGatewayProxyEvent;

const TEST_PATCH_REQUEST = [
  {
    signature:
      "qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ",
    state: VcState.CURRENT,
    metadata: {
      reason: "updated",
      timestampMs: "1714478033959",
      txmaEventId: "txma-event-id",
    },
  },
  {
    signature:
      "tf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ",
    state: VcState.ABANDONED,
  },
];
const TEST_PATCH_INVALID_STATE_REQUEST = [
  {
    signature:
      "qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ",
    state: VcState.VERIFICATION,
  },
];
const TEST_PATCH_EVENT = {
  body: JSON.stringify(TEST_PATCH_REQUEST),
  pathParameters: TEST_PATH_PARAM,
} as APIGatewayProxyEvent;
const TEST_PATCH_INVALID_STATE_EVENT = {
  body: JSON.stringify(TEST_PATCH_INVALID_STATE_REQUEST),
  pathParameters: TEST_PATH_PARAM,
} as APIGatewayProxyEvent;

describe("EVCS handler", function () {
  it("successfully retrieve and return user VCs response, No VCs in this case", async () => {
    // arrange
    const event = {
      ...TEST_GET_EVENT,
      httpMethod: "GET",
    };
    jest.mocked(getParameter).mockResolvedValue(EVCS_VERIFY_KEY);
    // act
    const result = (await getHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);
    const parseResult = JSON.parse(result.body as string);
    expect(parseResult.vcs.length).toEqual(0);
  });

  it("successfully process post request to persist user VCs", async () => {
    // arrange
    const event = {
      ...TEST_POST_EVENT,
      httpMethod: "POST",
    };
    jest.mocked(getParameter).mockResolvedValue("1800");
    // act
    const result = (await createHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(202);
    const response = await dynamoDocClient.send(getCommand);
    expect(response.Item?.userId).toEqual(decodedTestUserId);
    expect(response.Item?.provenance).toEqual(VCProvenance.ONLINE);
  });

  it("successfully persist and then returns user VCs response with different value for query param state", async () => {
    // arrange
    const postEvent = {
      ...TEST_POST_EVENT,
      httpMethod: "POST",
    };
    jest.mocked(getParameter).mockResolvedValue("1800");
    // act
    const postResult = (await createHandler(
      postEvent,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(postResult.statusCode).toEqual(202);

    // arrange
    let getEvent = {
      ...TEST_GET_EVENT,
      httpMethod: "GET",
    };
    jest.mocked(getParameter).mockResolvedValue(EVCS_VERIFY_KEY);
    // act
    let result = (await getHandler(
      getEvent,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);
    let parseResult = JSON.parse(result.body as string);
    expect(parseResult.vcs.length).toEqual(2);

    // arrange  - testing specific states as query param
    getEvent = {
      ...TEST_GET_EVENT_WITH_SPECIFIC_STATE_IN_QUERY,
      httpMethod: "GET",
    };
    jest.mocked(getParameter).mockResolvedValue(EVCS_VERIFY_KEY);
    // act
    result = (await getHandler(getEvent)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);
    parseResult = JSON.parse(result.body as string);
    expect(parseResult.vcs.length).toEqual(3);

    // arrange  - testing state as ALL value in query param
    getEvent = {
      ...TEST_GET_EVENT_WITH_ALL_STATE_IN_QUERY,
      httpMethod: "GET",
    };
    jest.mocked(getParameter).mockResolvedValue(EVCS_VERIFY_KEY);
    // act
    result = (await getHandler(getEvent)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);
    parseResult = JSON.parse(result.body as string);
    expect(parseResult.vcs.length).toEqual(4);
  });

  it("successfully persist and then update user VCs and then returns user VCs response", async () => {
    // arrange
    const postEvent = {
      ...TEST_POST_EVENT,
      httpMethod: "POST",
    };
    jest.mocked(getParameter).mockResolvedValue("1800");
    // act
    const postResult = (await createHandler(
      postEvent,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(postResult.statusCode).toEqual(202);

    // arrange
    let event = {
      ...TEST_GET_EVENT,
      httpMethod: "GET",
    };
    jest.mocked(getParameter).mockResolvedValue(EVCS_VERIFY_KEY);
    // act
    let result = (await getHandler(event)) as APIGatewayProxyStructuredResultV2;
    // assert
    expect(result.statusCode).toEqual(200);
    let parseResult = JSON.parse(result.body as string);
    expect(parseResult.vcs.length).toEqual(2);
    expect(parseResult.vcs[0].state).toEqual(VcState.CURRENT);
    expect(parseResult.vcs[0].metadata.reason).toEqual("test-created");
    expect(parseResult.vcs[0].metadata).toHaveProperty("testProperty");
    // arrange
    event = {
      ...TEST_PATCH_EVENT,
      httpMethod: "PATCH",
    };
    jest.mocked(getParameter).mockResolvedValue("1800");
    // act
    result = (await updateHandler(event)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(204);

    // arrange
    event = {
      ...TEST_GET_EVENT,
      httpMethod: "GET",
    };
    jest.mocked(getParameter).mockResolvedValue(EVCS_VERIFY_KEY);
    // act
    result = (await getHandler(event)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);
    parseResult = JSON.parse(result.body as string);
    expect(parseResult.vcs.length).toEqual(2);
    expect(parseResult.vcs[0].metadata.reason).toEqual("updated");
    expect(parseResult.vcs[0].metadata).not.toHaveProperty("testProperty");
  });

  it("returns a 400 when no or invalid access token passed", async () => {
    // arrange
    let TEST_GET_EVENT_WITH_INVALID_ACCESS_TOKEN = {
      pathParameters: TEST_PATH_PARAM,
      httpMethod: "GET",
    } as APIGatewayProxyEvent;

    jest.mocked(getParameter).mockResolvedValue(EVCS_VERIFY_KEY);
    // act
    let result = (await getHandler(
      TEST_GET_EVENT_WITH_INVALID_ACCESS_TOKEN,
    )) as APIGatewayProxyStructuredResultV2;
    // assert
    expect(result.statusCode).toEqual(400);

    // arrange
    let REQ_HEADERS = {
      Authorisation: "Bearer",
    } as APIGatewayProxyEventHeaders;
    TEST_GET_EVENT_WITH_INVALID_ACCESS_TOKEN = {
      pathParameters: TEST_PATH_PARAM,
      headers: REQ_HEADERS,
      httpMethod: "GET",
    } as APIGatewayProxyEvent;
    // act
    result = (await getHandler(
      TEST_GET_EVENT_WITH_INVALID_ACCESS_TOKEN,
    )) as APIGatewayProxyStructuredResultV2;
    // assert
    expect(result.statusCode).toEqual(400);

    // arrange
    REQ_HEADERS = {
      Authorisation:
        "Bear eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1cm46dXVpZDpkMTgyMzA2Ni0yMTM3LTQzODAtYjBiYS00YjYxOTQ3ZTA4ZTYiLCJpc3MiOiJodHRwczovL3RpY2YuYnVpbGQuc3R1YnMuYWNjb3VudC5nb3YudWsiLCJhdWQiOiJodHRwczovL3RpY2YuYnVpbGQuc3R1YnMuYWNjb3VudC5nb3YudWsiLCJuYmYiOjE3MTUxNjU0NjksImlhdCI6MTcxMjU3MzQ2OX0.BU6_2LreE5XUaIuz7FC4xZB9cUXLFQ6GcB_TdB43e34",
    } as APIGatewayProxyEventHeaders;
    TEST_GET_EVENT_WITH_INVALID_ACCESS_TOKEN = {
      pathParameters: TEST_PATH_PARAM,
      headers: REQ_HEADERS,
      httpMethod: "GET",
    } as APIGatewayProxyEvent;
    // act
    result = (await getHandler(
      TEST_GET_EVENT_WITH_INVALID_ACCESS_TOKEN,
    )) as APIGatewayProxyStructuredResultV2;
    // assert
    expect(result.statusCode).toEqual(400);

    // arrange
    REQ_HEADERS = {
      Authorisation: "Bearer ",
    } as APIGatewayProxyEventHeaders;
    TEST_GET_EVENT_WITH_INVALID_ACCESS_TOKEN = {
      pathParameters: TEST_PATH_PARAM,
      headers: REQ_HEADERS,
      httpMethod: "GET",
    } as APIGatewayProxyEvent;
    // act
    result = (await getHandler(
      TEST_GET_EVENT_WITH_INVALID_ACCESS_TOKEN,
    )) as APIGatewayProxyStructuredResultV2;
    // assert
    expect(result.statusCode).toEqual(400);

    // arrange
    REQ_HEADERS = {
      Authorisation:
        "Bearer eyJhbGc.eyJzdWIiOiJ1cm46dXVpZDpkMTgyMzA2Ni0yMTM3LTQzODAtYjBiYS00YjYxOTQ3ZTA4ZTYiLCJpc3MiOiJodHRwczovL3RpY2YuYnVpbGQuc3R1YnMuYWNjb3VudC5nb3YudWsiLCJhdWQiOiJodHRwczovL3RpY2YuYnVpbGQuc3R1YnMuYWNjb3VudC5nb3YudWsiLCJuYmYiOjE3MTUxNjU0NjksImlhdCI6MTcxMjU3MzQ2OX0.BU6_2LreE5XUaIuz7FC4xZB9cUXLFQ6GcB_TdB43e34",
    } as APIGatewayProxyEventHeaders;
    TEST_GET_EVENT_WITH_INVALID_ACCESS_TOKEN = {
      pathParameters: TEST_PATH_PARAM,
      headers: REQ_HEADERS,
      httpMethod: "GET",
    } as APIGatewayProxyEvent;
    // act
    result = (await getHandler(
      TEST_GET_EVENT_WITH_INVALID_ACCESS_TOKEN,
    )) as APIGatewayProxyStructuredResultV2;
    // assert
    expect(result.statusCode).toEqual(400);
  });

  it("returns a 400 when userId path paran not passed for create request", async () => {
    // arrange

    // act
    const result = (await createHandler(
      TEST_WITHOUT_USER_PARAM_EVENT,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
  });

  it("returns a 400 when userId path paran not passed for update request", async () => {
    // arrange

    // act
    const result = (await updateHandler(
      TEST_WITHOUT_USER_PARAM_EVENT,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
  });

  it("returns a 400 when userId path param not passed for get request", async () => {
    // arrange

    // act
    const result = (await getHandler(
      TEST_WITHOUT_USER_PARAM_EVENT,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
  });

  it("returns a 400 when invalid state param passed for get request", async () => {
    // arrange
    const getEvent = {
      ...TEST_GET_EVENT_WITH_INVALID_STATE_IN_QUERY,
      httpMethod: "GET",
    };
    // act
    const result = (await getHandler(
      getEvent,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
  });

  it("returns a 400 for an empty postrequest", async () => {
    // arrange
    const event = {
      ...TEST_POST_EVENT,
      body: null,
      httpMethod: "POST",
    };

    // act
    const result = (await createHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
  });

  it("returns a 400 for an empty patch request", async () => {
    // arrange
    const event = {
      ...TEST_PATCH_EVENT,
      body: null,
      httpMethod: "PATCH",
    };

    // act
    const result = (await updateHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
  });

  it("returns a 400 for an no vc request", async () => {
    // arrange
    const event = {
      ...TEST_NO_VC_EVENT,
      httpMethod: "POST",
    };

    // act
    const result = (await createHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
  });

  it("returns a 400 for an post request with invalid state value", async () => {
    // arrange
    const event = {
      ...TEST_POST_INVALID_STATE_EVENT,
      httpMethod: "POST",
    };
    jest.mocked(getParameter).mockResolvedValue("1800");
    // act
    const result = (await createHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
  });

  it("returns a 400 for an patch request with invalid state value", async () => {
    // arrange
    const event = {
      ...TEST_PATCH_INVALID_STATE_EVENT,
      httpMethod: "PATCH",
    };
    jest.mocked(getParameter).mockResolvedValue("1800");
    // act
    const result = (await updateHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
  });

  it("returns a 400 for an invalid request", async () => {
    // arrange
    const event = {
      ...TEST_POST_EVENT,
      body: "invalid json",
      httpMethod: "POST",
    };

    // act
    const result = (await createHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
  });

  it("returns a 500 for missing SSM parameter, failure at service for create request", async () => {
    // arrange
    jest.mocked(getParameter).mockResolvedValueOnce(undefined);
    const event = {
      ...TEST_POST_EVENT,
      httpMethod: "POST",
    };

    // act
    const result = (await createHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(500);
  });

  it("returns a 500 for missing SSM parameter, failure at service for update request", async () => {
    // arrange
    jest.mocked(getParameter).mockResolvedValueOnce(undefined);
    const event = {
      ...TEST_PATCH_EVENT,
      httpMethod: "PATCH",
    };

    // act
    const result = (await updateHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(500);
  });

  it("returns a 400 for an invalid http method", async () => {
    // arrange
    const event = {
      ...TEST_POST_EVENT,
      body: null,
      httpMethod: "CONNECT",
    };

    // act
    const result = (await createHandler(
      event,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
  });
});
