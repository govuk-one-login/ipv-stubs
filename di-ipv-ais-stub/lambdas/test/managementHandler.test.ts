import { State, UserManagementRequest } from "../src/utils/types";
import {
  APIGatewayProxyEventPathParameters,
  APIGatewayProxyEventV2,
  APIGatewayProxyStructuredResultV2,
} from "aws-lambda";
import { handler as managementHandler } from "../src/handlers/managementHandler";
import cases from "../src/cases";

const TEST_USER_ID = "test-user-id";
const TEST_USER_PATH_PARAM = {
  userId: TEST_USER_ID,
} as APIGatewayProxyEventPathParameters;

describe("managementHandler", () => {
  it("Returns 400 if missing intervention", async () => {
    // arrange
    const managementRequest = getManagementRequest({
      statusCode: 200,
      responseDelay: 0,
    });

    // act
    const result = (await managementHandler(
      managementRequest,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toStrictEqual(400);
  });

  it("Returns 400 if provided responseDelay greater than 40", async () => {
    // arrange
    const managementRequest = getManagementRequest({
      statusCode: 200,
      intervention: "AIS_ACCOUNT_BLOCKED",
      responseDelay: 100,
    });

    // act
    const result = (await managementHandler(
      managementRequest,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toStrictEqual(400);
  });

  it.each([
    {
      case: "Missing resetPassword",
      testState: { blocked: true, suspended: false, reproveIdentity: false },
    },
    {
      case: "Missing reproveIdentity",
      testState: { blocked: true, suspended: false, resetPassword: false },
    },
    {
      case: "Missing blocked",
      testState: {
        reproveIdentity: true,
        suspended: false,
        resetPassword: false,
      },
    },
    {
      case: "Missing suspended",
      testState: {
        reproveIdentity: true,
        blocked: false,
        resetPassword: false,
      },
    },
  ])(
    "Returns 400 Bad Request when custom state block is $case",
    async ({ testState }) => {
      // arrange
      const managementRequest = getManagementRequest({
        statusCode: 200,
        intervention: "AIS_ACCOUNT_BLOCKED",
        responseDelay: 0,
        state: testState,
      });

      // act
      const result = (await managementHandler(
        managementRequest,
      )) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(result.statusCode).toStrictEqual(400);
    },
  );
});

function getManagementRequest(
  body: Omit<UserManagementRequest, "state" | "intervention"> & {
    state?: Partial<State>;
    intervention?: keyof typeof cases;
  },
): APIGatewayProxyEventV2 {
  return {
    pathParameters: TEST_USER_PATH_PARAM,
    body: JSON.stringify(body),
  } as APIGatewayProxyEventV2;
}
