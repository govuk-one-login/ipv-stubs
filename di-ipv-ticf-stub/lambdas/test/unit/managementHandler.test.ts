import {
  APIGatewayProxyEventV2,
  APIGatewayProxyEventPathParameters,
  APIGatewayProxyStructuredResultV2,
} from "aws-lambda";
import { getParameter } from "@aws-lambda-powertools/parameters/ssm";
import { handler } from "../../src/management/handlers/managementHandler";

jest.mock("@aws-lambda-powertools/parameters/ssm", () => ({
  getParameter: jest.fn(),
}));

jest.mock("../../src/common/config", () => ({
  config: {
    ticfParamBasePath: "/test/path/",
    ticfStubUserEvidenceTableName: "ticf-stub-user-evidence",
  },
}));

const TEST_REQUEST = {
  type: "RiskAssessment",
  ci: ["V03", "D03"],
  txn: "uuid",
};
const TEST_PATH_PARAM = {
  userId: "test-user-id",
} as APIGatewayProxyEventPathParameters;

const TEST_EVENT = {
  body: JSON.stringify(TEST_REQUEST),
  pathParameters: TEST_PATH_PARAM,
} as APIGatewayProxyEventV2;

describe("TICF management handler", function () {
  it.skip("returns a successful response", async () => {
    // arrange
    jest.mocked(getParameter).mockResolvedValue("1800");

    // act
    const result = (await handler(
      TEST_EVENT
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);

    // const response = JSON.parse(result.body!) as TicfResponse;
  });
});
