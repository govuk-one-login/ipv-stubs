import {
  APIGatewayProxyEventV2,
  APIGatewayProxyEventPathParameters,
  APIGatewayProxyStructuredResultV2,
} from "aws-lambda";
import { DynamoDB } from "@aws-sdk/client-dynamodb";
import { DynamoDBDocumentClient, GetCommand } from "@aws-sdk/lib-dynamodb";
import { getParameter } from "@aws-lambda-powertools/parameters/ssm";
import { handler } from "../../src/management/handlers/managementHandler";
import { getUserEvidence } from "../../src/management/services/userEvidenceService";
import UserEvidenceItem from "../../src/management/model/userEvidenceItem";

jest.mock("@aws-lambda-powertools/parameters/ssm", () => ({
  getParameter: jest.fn(),
}));

const testUserId: string = "test-user-id";

const dbConfig = {
  convertEmptyValues: true,
  endpoint: "http://localhost:8000",
  sslEnabled: false,
  region: "local-env",
};

const dynamoClient = new DynamoDB(dbConfig);
const dynamoDocClient = DynamoDBDocumentClient.from(dynamoClient);

const getCommand = new GetCommand({
  TableName: "ticf-stub-user-evidence",
  Key: {
    userId: testUserId,
  },
});

jest.mock("../../src/common/config", () => ({
  config: {
    ticfParamBasePath: "/test/path/",
    ticfStubUserEvidenceTableName: "ticf-stub-user-evidence",
    localDynamoDbEndpoint: "http://localhost:8000",
    region: "local-dev",
    isLocalDev: true,
  },
}));

const TEST_REQUEST = {
  type: "RiskAssessment",
  ci: ["V03", "D03"],
  txn: "uuid",
};
const TEST_REQUEST_WITHOUT_CI = {
  type: "RiskAssessment",
  txn: "uuid",
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

const TEST_EVENT_WITHOUT_CI = {
  body: JSON.stringify(TEST_REQUEST_WITHOUT_CI),
  pathParameters: TEST_PATH_PARAM,
} as APIGatewayProxyEventV2;

describe("TICF management handler", function () {
  it("returns a successful response", async () => {
    const initiallyUserEvidenceInDb: UserEvidenceItem | null =
      await getUserEvidence(testUserId);
    expect(initiallyUserEvidenceInDb).toBeNull();
    // arrange
    jest.mocked(getParameter).mockResolvedValue("1800");
    // act
    let result = (await handler(
      TEST_EVENT
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);
    // assert - check evidence in db
    let response = await dynamoDocClient.send(getCommand);
    let evidence = response.Item?.evidence;
    expect(evidence.type).toEqual("RiskAssessment");
    expect(evidence.txn).toEqual("uuid");
    expect(evidence.ci).toEqual(["V03", "D03"]);

    //call again to update evidence for same user
    //act
    result = (await handler(
      TEST_EVENT_WITHOUT_CI
    )) as APIGatewayProxyStructuredResultV2;
    // assert
    expect(result.statusCode).toEqual(200);
    // assert - check evidence in db
    response = await dynamoDocClient.send(getCommand);
    evidence = response.Item?.evidence;
    expect(evidence.type).toEqual("RiskAssessment");
    expect(evidence.txn).toEqual("uuid");
    expect(evidence.ci).toBeUndefined();

    // to cover service method
    const userEvidenceItem: UserEvidenceItem | null = await getUserEvidence(
      testUserId
    );
    expect(userEvidenceItem).toBeDefined();
    expect(userEvidenceItem?.userId).toEqual(testUserId);
    expect(userEvidenceItem?.evidence).toBeDefined;
    evidence = userEvidenceItem?.evidence;
    expect(evidence.type).toEqual("RiskAssessment");
    expect(evidence.txn).toEqual("uuid");
    expect(evidence.ci).toBeUndefined();
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

  it("returns a 400 for a missing evidence type", async () => {
    // arrange
    const event = {
      ...TEST_EVENT,
      body: JSON.stringify({
        ...TEST_REQUEST,
        type: undefined,
      }),
    };

    // act
    const result = (await handler(event)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
  });

  it("returns a 500 for missing SSM parameter", async () => {
    // arrange
    jest.mocked(getParameter).mockResolvedValueOnce(undefined);

    // act
    const result = (await handler(
      TEST_EVENT
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(500);
  });
});
