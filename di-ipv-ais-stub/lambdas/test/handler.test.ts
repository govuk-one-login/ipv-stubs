import {
  APIGatewayProxyEventPathParameters,
  APIGatewayProxyEventV2,
} from "aws-lambda";
import { PutItemCommandInput } from "@aws-sdk/client-dynamodb";
import { UserManagementRequest, Response } from "../src/utils/types";
import cases from "../src/cases";

const TEST_USER_ID = "test-user-id";
const TEST_USER_PATH_PARAM = {
  userId: TEST_USER_ID,
} as APIGatewayProxyEventPathParameters;
const TEST_AIS_REQUEST_EVENT = {
  pathParameters: TEST_USER_PATH_PARAM,
} as APIGatewayProxyEventV2;

describe("Management handler primes AIS endpoint responses", () => {
  let mockDynamoClient: any; // eslint-disable-line @typescript-eslint/no-explicit-any
  let managementHandler: any; // eslint-disable-line @typescript-eslint/no-explicit-any
  let handler: any; // eslint-disable-line @typescript-eslint/no-explicit-any

  beforeEach(async () => {
    jest.resetModules();

    mockDynamoClient = {
      putItem: jest.fn(),
      getItem: jest.fn(),
    };

    jest.doMock("../src/utils/dynamoDbClient", () => ({
      __esModule: true,
      default: mockDynamoClient,
    }));

    jest.doMock("../src/utils/config", () => ({
      __esModule: true,
      config: {
        aisStubUserEvidenceTableName: "stub-table-name",
        aisParamBasePath: "/test/path/",
      },
      getSsmParameter: jest.fn().mockResolvedValue("60000"),
    }));

    managementHandler = (await import("../src/handlers/managementHandler"))
      .handler;
    handler = (await import("../src/handlers/handler")).handler;

    // Mock database actions
    let storedItem: Response | null = null;
    mockDynamoClient.putItem.mockImplementation(
      async (item: PutItemCommandInput) => {
        storedItem = item.Item as unknown as Response;
      },
    );
    mockDynamoClient.getItem.mockImplementation(async () => {
      return { Item: storedItem };
    });
  });

  it.each([
    {
      intervention: "AIS_NO_INTERVENTION",
      expectedStatusCode: 200,
      expectedResponseBody: {
        intervention: {
          updatedAt: 1696969322935,
          appliedAt: 1696869005821,
          sentAt: 1696869003456,
          description: "AIS_NO_INTERVENTION",
          reprovedIdentityAt: 1696969322935,
          resetPasswordAt: 1696875903456,
          accountDeletedAt: 1696969359935,
        },
        state: {
          blocked: false,
          suspended: false,
          reproveIdentity: false,
          resetPassword: false,
        },
        auditLevel: "standard",
        history: [],
      },
    },
    {
      intervention: "AIS_NO_INTERVENTION",
      expectedStatusCode: 400,
      expectedResponseBody: {},
    },
    {
      intervention: "AIS_NO_INTERVENTION",
      expectedStatusCode: 500,
      expectedResponseBody: {},
    },
    {
      intervention: "AIS_ACCOUNT_SUSPENDED",
      expectedStatusCode: 200,
      expectedResponseBody: {
        intervention: {
          updatedAt: 1696969322935,
          appliedAt: 1696869005821,
          sentAt: 1696869003456,
          description: "AIS_ACCOUNT_SUSPENDED",
          reprovedIdentityAt: 1696969322935,
          resetPasswordAt: 1696875903456,
          accountDeletedAt: 1696969359935,
        },
        state: {
          blocked: false,
          suspended: true,
          reproveIdentity: false,
          resetPassword: false,
        },
        auditLevel: "standard",
        history: [],
      },
    },
    {
      intervention: "AIS_ACCOUNT_UNSUSPENDED",
      expectedStatusCode: 200,
      expectedResponseBody: {
        intervention: {
          updatedAt: 1696969322935,
          appliedAt: 1696869005821,
          sentAt: 1696869003456,
          description: "AIS_ACCOUNT_UNSUSPENDED",
          reprovedIdentityAt: 1696969322935,
          resetPasswordAt: 1696875903456,
          accountDeletedAt: 1696969359935,
        },
        state: {
          blocked: false,
          suspended: false,
          reproveIdentity: false,
          resetPassword: false,
        },
        auditLevel: "standard",
        history: [],
      },
    },
    {
      intervention: "AIS_ACCOUNT_BLOCKED",
      expectedStatusCode: 200,
      expectedResponseBody: {
        intervention: {
          updatedAt: 1696969322935,
          appliedAt: 1696869005821,
          sentAt: 1696869003456,
          description: "AIS_ACCOUNT_BLOCKED",
          reprovedIdentityAt: 1696969322935,
          resetPasswordAt: 1696875903456,
          accountDeletedAt: 1696969359935,
        },
        state: {
          blocked: true,
          suspended: false,
          reproveIdentity: false,
          resetPassword: false,
        },
        auditLevel: "standard",
        history: [],
      },
    },
    {
      intervention: "AIS_ACCOUNT_UNBLOCKED",
      expectedStatusCode: 200,
      expectedResponseBody: {
        intervention: {
          updatedAt: 1696969322935,
          appliedAt: 1696869005821,
          sentAt: 1696869003456,
          description: "AIS_ACCOUNT_UNBLOCKED",
          reprovedIdentityAt: 1696969322935,
          resetPasswordAt: 1696875903456,
          accountDeletedAt: 1696969359935,
        },
        state: {
          blocked: false,
          suspended: false,
          reproveIdentity: false,
          resetPassword: false,
        },
        auditLevel: "standard",
        history: [],
      },
    },
    {
      intervention: "AIS_FORCED_USER_PASSWORD_RESET",
      expectedStatusCode: 200,
      expectedResponseBody: {
        intervention: {
          updatedAt: 1696969322935,
          appliedAt: 1696869005821,
          sentAt: 1696869003456,
          description: "AIS_FORCED_USER_PASSWORD_RESET",
          reprovedIdentityAt: 1696969322935,
          resetPasswordAt: 1696875903456,
          accountDeletedAt: 1696969359935,
        },
        state: {
          blocked: false,
          suspended: true,
          reproveIdentity: false,
          resetPassword: true,
        },
        auditLevel: "standard",
        history: [],
      },
    },
    {
      intervention: "AIS_FORCED_USER_IDENTITY_VERIFY",
      expectedStatusCode: 200,
      expectedResponseBody: {
        intervention: {
          updatedAt: 1696969322935,
          appliedAt: 1696869005821,
          sentAt: 1696869003456,
          description: "AIS_FORCED_USER_IDENTITY_VERIFY",
          reprovedIdentityAt: 1696969322935,
          resetPasswordAt: 1696875903456,
          accountDeletedAt: 1696969359935,
        },
        state: {
          blocked: false,
          suspended: true,
          reproveIdentity: true,
          resetPassword: false,
        },
        auditLevel: "standard",
        history: [],
      },
    },
    {
      intervention: "AIS_FORCED_USER_PASSWORD_RESET_AND_IDENTITY_VERIFY",
      expectedStatusCode: 200,
      expectedResponseBody: {
        intervention: {
          updatedAt: 1696969322935,
          appliedAt: 1696869005821,
          sentAt: 1696869003456,
          description: "AIS_FORCED_USER_PASSWORD_RESET_AND_IDENTITY_VERIFY",
          reprovedIdentityAt: 1696969322935,
          resetPasswordAt: 1696875903456,
          accountDeletedAt: 1696969359935,
        },
        state: {
          blocked: false,
          suspended: true,
          reproveIdentity: true,
          resetPassword: true,
        },
        auditLevel: "standard",
        history: [],
      },
    },
  ])(
    "Return $expectedStatusCode for $intervention",
    async ({ intervention, expectedStatusCode, expectedResponseBody }) => {
      // arrange
      const managementRequest = getManagementRequest({
        statusCode: expectedStatusCode,
        intervention: intervention as keyof typeof cases,
        responseDelay: 0,
      });

      // act
      await managementHandler(managementRequest);
      const result = await handler(TEST_AIS_REQUEST_EVENT);

      // assert
      expect(result.statusCode).toStrictEqual(expectedStatusCode);
      expect(JSON.parse(result.body)).toStrictEqual(expectedResponseBody);
    },
  );

  it("Return no interventions as default", async () => {
    // act
    const result = await handler(TEST_AIS_REQUEST_EVENT);

    // assert
    expect(result.statusCode).toStrictEqual(200);
    expect(JSON.parse(result.body)).toStrictEqual({
      intervention: {
        updatedAt: 1696969322935,
        appliedAt: 1696869005821,
        sentAt: 1696869003456,
        description: "AIS_NO_INTERVENTION",
        reprovedIdentityAt: 1696969322935,
        resetPasswordAt: 1696875903456,
        accountDeletedAt: 1696969359935,
      },
      state: {
        blocked: false,
        suspended: false,
        reproveIdentity: false,
        resetPassword: false,
      },
      auditLevel: "standard",
      history: []
    })
  })
});

function getManagementRequest(
  body: UserManagementRequest,
): APIGatewayProxyEventV2 {
  return {
    pathParameters: TEST_USER_PATH_PARAM,
    body: JSON.stringify(body),
  } as APIGatewayProxyEventV2;
}
