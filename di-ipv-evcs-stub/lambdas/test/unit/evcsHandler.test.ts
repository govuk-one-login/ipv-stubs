import { handler } from "../../src/handlers/evcsHandler";
import { DynamoDB } from "@aws-sdk/client-dynamodb";
import { DynamoDBDocumentClient, GetCommand } from "@aws-sdk/lib-dynamodb";

import {
    APIGatewayProxyEvent,
    APIGatewayProxyEventHeaders,
    APIGatewayProxyEventPathParameters,
    APIGatewayProxyStructuredResultV2,
} from "aws-lambda";

import { getParameter } from "@aws-lambda-powertools/parameters/ssm";
import VcState from "../../src/domain/enums/vcState";

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
    vcSignature: 'qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ'
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

const TEST_POST_REQUEST = {
  persistVCs: [
      {
          vc: "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ",
          state: VcState.CURRENT,
          metadata: {
            "reason": "test-created",
            "timestampMs": "1714478033959",
            "txmaEventId": "txma-event-id"
          }
      },
      {
        vc: "yyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.tf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ",
        state: VcState.VERIFICATION
      }
  ]
};
const TEST_POST_INVALID_STATE_REQUEST = {
  persistVCs: [
      {
          vc: "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ",
          state: VcState.HISTORIC
      }
  ]
};

const TEST_NO_VC_REQUEST = {
  persistVCs: [
  ]
};

const TEST_PATH_PARAM = {
  userId: testUserId,
} as APIGatewayProxyEventPathParameters;

const TEST_POST_EVENT = {
  body: JSON.stringify(TEST_POST_REQUEST),
  pathParameters: TEST_PATH_PARAM,
} as APIGatewayProxyEvent;
const TEST_POST_INVALID_STATE_EVENT = {
  body: JSON.stringify(TEST_POST_INVALID_STATE_REQUEST),
  pathParameters: TEST_PATH_PARAM,
} as APIGatewayProxyEvent;

const TEST_HEADERS = {
  Authorisation: "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1cm46dXVpZDpkMTgyMzA2Ni0yMTM3LTQzODAtYjBiYS00YjYxOTQ3ZTA4ZTYiLCJpc3MiOiJodHRwczovL3RpY2YuYnVpbGQuc3R1YnMuYWNjb3VudC5nb3YudWsiLCJhdWQiOiJodHRwczovL3RpY2YuYnVpbGQuc3R1YnMuYWNjb3VudC5nb3YudWsiLCJuYmYiOjE3MTUxNjU0NjksImlhdCI6MTcxMjU3MzQ2OX0.BU6_2LreE5XUaIuz7FC4xZB9cUXLFQ6GcB_TdB43e34",
} as APIGatewayProxyEventHeaders;

const TEST_GET_EVENT = {
  pathParameters: TEST_PATH_PARAM,
  headers: TEST_HEADERS
} as APIGatewayProxyEvent;

const TEST_NO_VC_EVENT = {
  body: JSON.stringify(TEST_NO_VC_REQUEST),
  pathParameters: TEST_PATH_PARAM,
} as APIGatewayProxyEvent;

const TEST_WITHOUT_USER_PARAM_EVENT = {
  body: JSON.stringify(TEST_POST_REQUEST),
} as APIGatewayProxyEvent;

const TEST_PATCH_REQUEST = {
  updateVCs: [
      {
          signature: "qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ",
          state: VcState.CURRENT,
          metadata: {
            "reason": "updated",
            "timestampMs": "1714478033959",
            "txmaEventId": "txma-event-id"
          }
      },
      {
        signature: "tf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ",
        state: VcState.ABANDONED
      }
  ]
};
const TEST_PATCH_INVALID_STATE_REQUEST = {
  updateVCs: [
      {
          signature: "qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ",
          state: VcState.VERIFICATION
      }
  ]
};
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
      httpMethod: "GET"
    };
    jest.mocked(getParameter).mockResolvedValue("1800");
    // act
    const result = (await handler(
      event
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);
    const parseResult = JSON.parse(result.body as string);
    expect(0).toEqual(parseResult.vcs.length);
  });

  it("successfully process post request to persist user VCs", async () => {
      // arrange
      const event = {
        ...TEST_POST_EVENT,
        httpMethod: "POST"
      };
      jest.mocked(getParameter).mockResolvedValue("1800");
      // act
      const result = (await handler(
        event
      )) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(result.statusCode).toEqual(202);
      const response = await dynamoDocClient.send(getCommand);
      expect(decodedTestUserId).toEqual(response.Item?.userId);
  });

  it("successfully persist and then returns user VCs response", async () => {
    // arrange
    const postEvent = {
      ...TEST_POST_EVENT,
      httpMethod: "POST"
    };
    jest.mocked(getParameter).mockResolvedValue("1800");
    // act
    const postResult = (await handler(
      postEvent
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(postResult.statusCode).toEqual(202);

    // arrange
    const event = {
      ...TEST_GET_EVENT,
      httpMethod: "GET"
    };
    jest.mocked(getParameter).mockResolvedValue("1800");
    // act
    const result = (await handler(
      event
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);
    const parseResult = JSON.parse(result.body as string);
    expect(1).toEqual(parseResult.vcs.length);
  });

  it("successfully persist and then update user VCs and then returns user VCs response", async () => {
    // arrange
    const postEvent = {
      ...TEST_POST_EVENT,
      httpMethod: "POST"
    };
    jest.mocked(getParameter).mockResolvedValue("1800");
    // act
    const postResult = (await handler(
      postEvent
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(postResult.statusCode).toEqual(202);

    // arrange
    let event = {
      ...TEST_GET_EVENT,
      httpMethod: "GET"
    };
    jest.mocked(getParameter).mockResolvedValue("1800");
    // act
    let result = (await handler(
      event
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);
    let parseResult = JSON.parse(result.body as string);
    expect(1).toEqual(parseResult.vcs.length);
    expect(VcState.CURRENT).toEqual(parseResult.vcs[0].state);

    // arrange
    event = {
      ...TEST_PATCH_EVENT,
      httpMethod: "PATCH"
    };
    jest.mocked(getParameter).mockResolvedValue("1800");
    // act
    result = (await handler(
      event
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(204);

    // arrange
    event = {
      ...TEST_GET_EVENT,
      httpMethod: "GET"
    };
    jest.mocked(getParameter).mockResolvedValue("1800");
    // act
    result = (await handler(
      event
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(200);
    parseResult = JSON.parse(result.body as string);
    expect(1).toEqual(parseResult.vcs.length);
    expect(parseResult.vcs[0].metadata.reason).toEqual("updated");
  });

  it("returns a 400 when no or invalid access token passed", async () => {
    // arrange
    let TEST_GET_EVENT_WITH_INVALID_AACCESS_TOKEN = {
      pathParameters: TEST_PATH_PARAM,
      httpMethod: "GET"
    } as APIGatewayProxyEvent;

    jest.mocked(getParameter).mockResolvedValue("1800");
    // act
    let result = (await handler(
      TEST_GET_EVENT_WITH_INVALID_AACCESS_TOKEN
    )) as APIGatewayProxyStructuredResultV2;
    // assert
    expect(result.statusCode).toEqual(400);

    // arrange
    let REQ_HEADERS = {
      Authorisation: "Bearer",
    } as APIGatewayProxyEventHeaders;
    TEST_GET_EVENT_WITH_INVALID_AACCESS_TOKEN = {
      pathParameters: TEST_PATH_PARAM,
      headers: REQ_HEADERS,
      httpMethod: "GET"
    } as APIGatewayProxyEvent;
    // act
    result = (await handler(
      TEST_GET_EVENT_WITH_INVALID_AACCESS_TOKEN
    )) as APIGatewayProxyStructuredResultV2;
    // assert
    expect(result.statusCode).toEqual(400);

    // arrange
    REQ_HEADERS = {
      Authorisation: "Bear eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1cm46dXVpZDpkMTgyMzA2Ni0yMTM3LTQzODAtYjBiYS00YjYxOTQ3ZTA4ZTYiLCJpc3MiOiJodHRwczovL3RpY2YuYnVpbGQuc3R1YnMuYWNjb3VudC5nb3YudWsiLCJhdWQiOiJodHRwczovL3RpY2YuYnVpbGQuc3R1YnMuYWNjb3VudC5nb3YudWsiLCJuYmYiOjE3MTUxNjU0NjksImlhdCI6MTcxMjU3MzQ2OX0.BU6_2LreE5XUaIuz7FC4xZB9cUXLFQ6GcB_TdB43e34",
    } as APIGatewayProxyEventHeaders;
    TEST_GET_EVENT_WITH_INVALID_AACCESS_TOKEN = {
      pathParameters: TEST_PATH_PARAM,
      headers: REQ_HEADERS,
      httpMethod: "GET"
    } as APIGatewayProxyEvent;
    // act
    result = (await handler(
      TEST_GET_EVENT_WITH_INVALID_AACCESS_TOKEN
    )) as APIGatewayProxyStructuredResultV2;
    // assert
    expect(result.statusCode).toEqual(400);

    // arrange
    REQ_HEADERS = {
      Authorisation: "Bearer ",
    } as APIGatewayProxyEventHeaders;
    TEST_GET_EVENT_WITH_INVALID_AACCESS_TOKEN = {
      pathParameters: TEST_PATH_PARAM,
      headers: REQ_HEADERS,
      httpMethod: "GET"
    } as APIGatewayProxyEvent;
    // act
    result = (await handler(
      TEST_GET_EVENT_WITH_INVALID_AACCESS_TOKEN
    )) as APIGatewayProxyStructuredResultV2;
    // assert
    expect(result.statusCode).toEqual(400);
  });

  it("returns a 400 when userId path paran not passed", async () => {
      // arrange

      // act
      const result = (await handler(
        TEST_WITHOUT_USER_PARAM_EVENT
      )) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(result.statusCode).toEqual(400);
  });

  it("returns a 400 for an empty postrequest", async () => {
    // arrange
    const event = {
      ...TEST_POST_EVENT,
      body: null,
      httpMethod: "POST"
    };

    // act
    const result = (await handler(event)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
  });

  it("returns a 400 for an empty patch request", async () => {
    // arrange
    const event = {
      ...TEST_PATCH_EVENT,
      body: null,
      httpMethod: "PATCH"
    };

    // act
    const result = (await handler(event)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
  });

  it("returns a 400 for an no vc request", async () => {
    // arrange
    const event = {
      ...TEST_NO_VC_EVENT,
      httpMethod: "POST"
    };

    // act
    const result = (await handler(event)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
  });

  it("returns a 400 for an post request with invalid state value", async () => {
    // arrange
    const event = {
      ...TEST_POST_INVALID_STATE_EVENT,
      httpMethod: "POST"
    };
    jest.mocked(getParameter).mockResolvedValue("1800");
    // act
    const result = (await handler(
      event
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
  });

  it("returns a 400 for an patch request with invalid state value", async () => {
    // arrange
    const event = {
      ...TEST_PATCH_INVALID_STATE_EVENT,
      httpMethod: "PATCH"
    };
    jest.mocked(getParameter).mockResolvedValue("1800");
    // act
    const result = (await handler(
      event
    )) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(400);
  });

  it("returns a 400 for an invalid request", async () => {
      // arrange
      const event = {
        ...TEST_POST_EVENT,
        body: "invalid json",
        httpMethod: "POST"
      };

      // act
      const result = (await handler(event)) as APIGatewayProxyStructuredResultV2;

      // assert
      expect(result.statusCode).toEqual(400);
  });

  it("returns a 500 for missing SSM parameter, failure at service", async () => {
    // arrange
    jest.mocked(getParameter).mockResolvedValueOnce(undefined);
    const event = {
      ...TEST_POST_EVENT,
      httpMethod: "POST"
    };

    // act
    const result = (await handler(event,)) as APIGatewayProxyStructuredResultV2;

    // assert
    expect(result.statusCode).toEqual(500);
  });
});
