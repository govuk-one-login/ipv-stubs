import {
  APIGatewayProxyEventPathParameters,
  APIGatewayProxyEventV2,
  APIGatewayProxyStructuredResultV2,
} from "aws-lambda";
import { handler } from "../src/management/handlers/managementHandler";
import persistUserEvidence from "../src/management/services/userEvidenceService";

jest.mock("../src/management/services/userEvidenceService");

const TEST_USER_ID: string = "urn:uuid:test-user-id";

const TEST_REQUEST = {
  evidence: {
    type: "RiskAssessment",
    ci: ["V03", "D03"],
    txn: "uuid",
    intervention: {
      interventionCode: "01",
      interventionReason: "007",
    },
  },
};

const TEST_PATH_PARAM = {
  userId: TEST_USER_ID,
} as APIGatewayProxyEventPathParameters;

const TEST_EVENT = {
  body: JSON.stringify(TEST_REQUEST),
  pathParameters: TEST_PATH_PARAM,
} as APIGatewayProxyEventV2;

describe("TICF management handler", () => {
  it("returns a 200 for a valid request", async () => {
    // arrange
    jest.mocked(persistUserEvidence).mockResolvedValueOnce();

    // act
    const result = (await handler(
      TEST_EVENT,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);
    expect(persistUserEvidence).toHaveBeenCalledWith(
      TEST_USER_ID,
      TEST_REQUEST,
    );
  });

  it("returns a 200 for a request with a response delay", async () => {
    // arrange
    jest.mocked(persistUserEvidence).mockResolvedValueOnce();

    // act
    const result = (await handler({
      ...TEST_EVENT,
      body: JSON.stringify({
        ...TEST_REQUEST,
        responseDelay: 10,
      }),
    })) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);
    expect(persistUserEvidence).toHaveBeenCalledWith(TEST_USER_ID, {
      ...TEST_REQUEST,
      responseDelay: 10,
    });
  });

  it("returns a 200 for a request with error statusCode and no evidence", async () => {
    // arrange
    jest.mocked(persistUserEvidence).mockResolvedValueOnce();

    // act
    const result = (await handler({
      ...TEST_EVENT,
      body: JSON.stringify({
        statusCode: 500,
      }),
    })) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);
    expect(persistUserEvidence).toHaveBeenCalledWith(TEST_USER_ID, {
      statusCode: 500,
    });
  });

  it("returns a 200 for a request with 200 statusCode and no evidence", async () => {
    // arrange
    jest.mocked(persistUserEvidence).mockResolvedValueOnce();

    // act
    const result = (await handler({
      ...TEST_EVENT,
      body: JSON.stringify({
        statusCode: 200,
      }),
    })) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);
    expect(persistUserEvidence).toHaveBeenCalledWith(TEST_USER_ID, {
      statusCode: 200,
    });
  });

  it("returns 400 for a request with no user id", async () => {
    // act
    const result = (await handler({
      ...TEST_EVENT,
      pathParameters: undefined,
    })) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
    expect(persistUserEvidence).not.toHaveBeenCalled();
  });

  it("returns 400 for a request with no body", async () => {
    // act
    const result = (await handler({
      ...TEST_EVENT,
      body: undefined,
    })) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
    expect(persistUserEvidence).not.toHaveBeenCalled();
  });

  it("returns 400 for a request with invalid body", async () => {
    // act
    const result = (await handler({
      ...TEST_EVENT,
      body: undefined,
    })) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
    expect(persistUserEvidence).not.toHaveBeenCalled();
  });

  it("returns 400 for a request with missing evidence type", async () => {
    // act
    const result = (await handler({
      ...TEST_EVENT,
      body: JSON.stringify({
        ...TEST_REQUEST,
        evidence: { ...TEST_REQUEST.evidence, type: undefined },
      }),
    })) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
    expect(persistUserEvidence).not.toHaveBeenCalled();
  });

  it("returns 400 for a request with an invalid evidence", async () => {
    // act
    const result = (await handler({
      ...TEST_EVENT,
      body: JSON.stringify({
        evidence: { ...TEST_REQUEST.evidence, type: undefined },
      }),
    })) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
    expect(persistUserEvidence).not.toHaveBeenCalled();
  });

  it("returns 400 for a request with too large response delay", async () => {
    // act
    const result = (await handler({
      ...TEST_EVENT,
      body: JSON.stringify({
        ...TEST_REQUEST,
        responseDelay: 45,
      }),
    })) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
    expect(persistUserEvidence).not.toHaveBeenCalled();
  });

  it("returns 500 if storing fails", async () => {
    // arrange
    jest.mocked(persistUserEvidence).mockRejectedValueOnce(new Error());

    // act
    const result = (await handler(
      TEST_EVENT,
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(500);
  });
});
