import {
  APIGatewayEventDefaultAuthorizerContext,
  APIGatewayEventRequestContextWithAuthorizer,
} from "aws-lambda/common/api-gateway";

jest.mock(
  "../../../src/external-api/stub-management/service/userService",
  () => ({
    addUserCis: jest.fn(),
    updateUserCis: jest.fn(),
  }),
);

jest.mock("../../../src/common/cimitStubItemService", () => ({
  getCiForUserId: jest.fn(),
}));

jest.mock("../../../src/common/pendingMitigationService", () => ({
  persistPendingMitigation: jest.fn(),
}));

import { APIGatewayProxyEvent } from "aws-lambda";
import { handler } from "../../../src/external-api/stub-management/stubManagementHandler";
import * as userService from "../../../src/external-api/stub-management/service/userService";
import * as cimitStubItemService from "../../../src/common/cimitStubItemService";
import * as pendingMitigationService from "../../../src/common/pendingMitigationService";

describe("stubManagementHandler", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const createEvent = (
    httpMethod: string,
    path: string,
    pathParameters: { [key: string]: string },
    body?: object,
  ): APIGatewayProxyEvent => ({
    httpMethod,
    path,
    body: body !== undefined ? JSON.stringify(body) : null,
    pathParameters: pathParameters,
    headers: {},
    multiValueHeaders: {},
    isBase64Encoded: false,
    queryStringParameters: null,
    multiValueQueryStringParameters: null,
    stageVariables: null,
    requestContext:
      {} as APIGatewayEventRequestContextWithAuthorizer<APIGatewayEventDefaultAuthorizerContext>,
    resource: "",
  });

  describe("CI requests", () => {
    it("should add user CIs successfully when valid CI request list", async () => {
      const userCisRequests = [
        {
          code: "code1",
          issuanceDate: "2023-07-25T10:00:00Z",
          mitigations: ["V01", "V03"],
          issuer: "",
          document: "",
          txn: "",
        },
        {
          code: "code2",
          issuanceDate: "2023-07-25T10:00:00Z",
          mitigations: [],
          issuer: "",
          document: "",
          txn: "",
        },
      ];

      const event = createEvent(
        "POST",
        "/user/123/cis",
        { userId: "123" },
        userCisRequests,
      );
      const response = await handler(event);

      expect(response.statusCode).toBe(200);
      expect(response.body).toContain("success");
      expect(userService.addUserCis).toHaveBeenCalledWith(
        "123",
        userCisRequests,
      );
    });

    it("should update user Cis from valid CI request", async () => {
      const userCisRequest = [
        {
          code: "code1",
          issuanceDate: "2023-07-25T10:00:00Z",
          mitigations: ["V01", "V03"],
          issuer: "",
          document: "",
          txn: "",
        },
      ];

      const event = createEvent(
        "PUT",
        "/user/123/cis",
        { userId: "123" },
        userCisRequest,
      );
      const response = await handler(event);

      expect(response.statusCode).toBe(200);
      expect(response.body).toContain("success");
      expect(userService.updateUserCis).toHaveBeenCalledWith(
        "123",
        userCisRequest,
      );
    });

    it("should handle default user ID format in CI endpoint pattern", async () => {
      const urlEncodedUserId =
        "urn%3Auuid%3Ac08630f8-330e-43f8-a782-21432a197fc5";
      const userCisRequests = [
        {
          code: "code1",
          issuanceDate: "2023-07-25T10:00:00Z",
          mitigations: ["V01", "V03"],
          issuer: "",
          document: "",
          txn: "",
        },
      ];

      const event = createEvent(
        "POST",
        `/user/${urlEncodedUserId}/cis`,
        { userId: urlEncodedUserId },
        userCisRequests,
      );
      const response = await handler(event);

      expect(response.statusCode).toBe(200);
      expect(response.body).toContain("success");
      expect(userService.addUserCis).toHaveBeenCalledWith(
        "urn:uuid:c08630f8-330e-43f8-a782-21432a197fc5",
        userCisRequests,
      );
    });

    it("should return bad request when invalid request body for CIs", async () => {
      const event = createEvent("POST", "/user/123/cis", { userId: "123" });
      event.body = "invalid json";

      const response = await handler(event);

      expect(response.statusCode).toBe(400);
      expect(response.body).toContain("Invalid request body");
    });

    it("should return bad request for empty code", async () => {
      const userCisRequest = [
        {
          code: "",
          issuanceDate: "2023-07-25T10:00:00Z",
          mitigations: ["V01", "V03"],
          issuer: "",
          document: "",
          txn: "",
        },
      ];

      const event = createEvent(
        "POST",
        "/user/123/cis",
        { userId: "123" },
        userCisRequest,
      );
      const response = await handler(event);

      expect(response.statusCode).toBe(400);
      expect(response.body).toContain("CI codes cannot be empty.");
      expect(userService.addUserCis).not.toHaveBeenCalled();
    });

    it("should return success for valid PUT request with empty content", async () => {
      const event = createEvent("PUT", "/user/123/cis", { userId: "123" }, []);
      const response = await handler(event);

      expect(response.statusCode).toBe(200);
      expect(userService.updateUserCis).toHaveBeenCalledWith("123", []);
    });

    it("should return success for valid PUT request with single content", async () => {
      const userCisRequest = [
        {
          code: "code1",
          issuanceDate: "2023-07-25T10:00:00Z",
          mitigations: ["V01", "V03"],
          issuer: "",
          document: "",
          txn: "",
        },
      ];

      const event = createEvent(
        "PUT",
        "/user/123/cis",
        { userId: "123" },
        userCisRequest,
      );
      const response = await handler(event);

      expect(response.statusCode).toBe(200);
      expect(userService.updateUserCis).toHaveBeenCalledWith(
        "123",
        userCisRequest,
      );
    });
  });

  describe("Mitigation requests", () => {
    ["POST", "PUT"].forEach((method) => {
      it(`should add pending mitigation when valid mitigation request - ${method}`, async () => {
        (
          cimitStubItemService.getCiForUserId as jest.Mock
        ).mockResolvedValueOnce([{}]);
        const userMitigationRequest = { mitigations: ["V01"], vcJti: "jti123" };

        const event = createEvent(
          method,
          "/user/123/mitigations/456",
          { userId: "123", ci: "456" },
          userMitigationRequest,
        );
        const response = await handler(event);

        expect(response.statusCode).toBe(200);
        expect(response.body).toContain("success");
        expect(cimitStubItemService.getCiForUserId).toHaveBeenCalledWith(
          "123",
          "456",
        );
        expect(
          pendingMitigationService.persistPendingMitigation,
        ).toHaveBeenCalledWith(userMitigationRequest, "456", method);
      });
    });

    ["POST", "PUT"].forEach((method) => {
      it(`should return 404 if CI item not found for mitigation request - ${method}`, async () => {
        (
          cimitStubItemService.getCiForUserId as jest.Mock
        ).mockResolvedValueOnce(null);
        const userMitigationRequest = { mitigations: ["V01"], vcJti: "jti123" };

        const event = createEvent(
          method,
          "/user/123/mitigations/456",
          { userId: "123", ci: "456" },
          userMitigationRequest,
        );
        const response = await handler(event);

        expect(response.statusCode).toBe(404);
        expect(response.body).toContain("not found");
        expect(cimitStubItemService.getCiForUserId).toHaveBeenCalledWith(
          "123",
          "456",
        );
        expect(
          pendingMitigationService.persistPendingMitigation,
        ).not.toHaveBeenCalled();
      });
    });

    ["POST", "PUT"].forEach((method) => {
      it("should return bad request when invalid request body for mitigation", async () => {
        const event = createEvent(method, "/user/123/mitigations/456", {
          userId: "123",
          ci: "456",
        });
        event.body = "invalid json";

        const response = await handler(event);

        expect(response.statusCode).toBe(400);
        expect(response.body).toContain("Invalid request body");
      });
    });

    ["POST", "PUT"].forEach((method) => {
      it("should handle default user ID format in mitigations pattern", async () => {
        (
          cimitStubItemService.getCiForUserId as jest.Mock
        ).mockResolvedValueOnce([{}]);
        const urlEncodedUserId =
          "urn%3Auuid%3Ac08630f8-330e-43f8-a782-21432a197fc5";
        const userMitigationRequest = { mitigations: ["V01"], vcJti: "jti123" };

        const event = createEvent(
          method,
          `/user/${urlEncodedUserId}/mitigations/456`,
          { userId: urlEncodedUserId, ci: "456" },
          userMitigationRequest,
        );
        const response = await handler(event);

        expect(response.statusCode).toBe(200);
        expect(response.body).toContain("success");
        expect(cimitStubItemService.getCiForUserId).toHaveBeenCalledWith(
          "urn:uuid:c08630f8-330e-43f8-a782-21432a197fc5",
          "456",
        );
        expect(
          pendingMitigationService.persistPendingMitigation,
        ).toHaveBeenCalledWith(userMitigationRequest, "456", method);
      });
    });

    ["POST", "PUT"].forEach((method) => {
      it("should return data not found for missing user CIs", async () => {
        const userCisRequest = [
          {
            code: "code1",
            issuanceDate: "2023-07-25T10:00:00Z",
            mitigations: ["V01", "V03"],
            issuer: "",
            document: "",
            txn: "",
          },
        ];
        (
          cimitStubItemService.getCiForUserId as jest.Mock
        ).mockResolvedValueOnce([]);

        const event = createEvent(
          method,
          "/user/123/mitigations/456",
          { userId: "123", ci: "456" },
          userCisRequest,
        );
        const response = await handler(event);

        expect(response.statusCode).toBe(404);
        expect(response.body).toContain("User and ContraIndicator not found.");
        expect(cimitStubItemService.getCiForUserId).toHaveBeenCalledWith(
          "123",
          "456",
        );
      });
    });
  });

  it("should return bad request when invalid endpoint", async () => {
    const event = createEvent("POST", "/user/123/invalid", {});
    const response = await handler(event);

    expect(response.statusCode).toBe(400);
    expect(response.body).toContain("Invalid URI.");
  });

  it("should return error for CI request with no content", async () => {
    const event = createEvent("PUT", "/user/123/cis", { userId: "123" });
    event.body = null;

    const response = await handler(event);

    expect(response.statusCode).toBe(400);
    expect(response.body).toContain("Invalid request body");
  });

  it("should return internal server error for unknown exception", async () => {
    const userCisRequest = [
      {
        code: "code1",
        issuanceDate: "2023-07-25T10:00:00Z",
        mitigations: ["V01", "V03"],
        issuer: "",
        document: "",
        txn: "",
      },
    ];
    (userService.addUserCis as jest.Mock).mockRejectedValueOnce(
      new Error("Unknown exception occurred."),
    );

    const event = createEvent(
      "POST",
      "/user/123/cis",
      { userId: "123" },
      userCisRequest,
    );
    const response = await handler(event);

    expect(response.statusCode).toBe(500);
    expect(response.body).toContain("Unknown exception occurred.");
  });

  it("should return bad request for invalid method", async () => {
    const userCisRequest = [
      {
        code: "code1",
        issuanceDate: "2023-07-25T10:00:00Z",
        mitigations: ["V01", "V03"],
        issuer: "",
        document: "",
        txn: "",
      },
    ];
    const event = createEvent(
      "PATCH",
      "/user/123/cis",
      { userId: "123" },
      userCisRequest,
    );

    const response = await handler(event);

    expect(response.statusCode).toBe(400);
    expect(response.body).toContain("Http Method is not supported.");
  });
});
