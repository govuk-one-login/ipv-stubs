import { handler } from "../../src/handlers/evcsHandler";
import { DynamoDB } from "@aws-sdk/client-dynamodb";
import { DynamoDBDocumentClient, GetCommand } from "@aws-sdk/lib-dynamodb";

import {
    APIGatewayProxyEventV2,
    APIGatewayProxyEventPathParameters,
    APIGatewayProxyStructuredResultV2,
} from "aws-lambda";

import { getParameter } from "@aws-lambda-powertools/parameters/ssm";

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

const TEST_REQUEST = {
  persistVCs: [
      {
          vc: "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ",
          state: "CURRENT"
      }
  ]
};

const TEST_PATH_PARAM = {
  userId: testUserId,
} as APIGatewayProxyEventPathParameters;

const TEST_EVENT = {
  body: JSON.stringify(TEST_REQUEST),
  pathParameters: TEST_PATH_PARAM,
} as APIGatewayProxyEventV2;

const TEST_EVENT_WITHOUT_USER_PARAM = {
  body: JSON.stringify(TEST_REQUEST),
} as APIGatewayProxyEventV2;

describe("EVCS handler", function () {
  it("returns a successful response", async () => {
      // arrange
      jest.mocked(getParameter).mockResolvedValue("1800");
      // act
      const result = (await handler(
        TEST_EVENT
      )) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(result.statusCode).toEqual(202);
      const response = await dynamoDocClient.send(getCommand);
      expect(decodedTestUserId).toEqual(response.Item?.userId);
  });

  it("returns a 400 when userId path paran not passed", async () => {
      // arrange

      // act
      const result = (await handler(
        TEST_EVENT_WITHOUT_USER_PARAM
      )) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(result.statusCode).toEqual(400);
  });

  it("returns a 400 for an empty request", async () => {
    // arrange
    const event = {
      ...TEST_EVENT,
      body: undefined,
    };

    // act
    const result = (await handler(event)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
  });
  
  it("returns a 400 for an invalid request", async () => {
      // arrange
      const event = {
        ...TEST_EVENT,
        body: "invalid json",
      };

      // act
      const result = (await handler(event)) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(result.statusCode).toEqual(400);
  });
});
