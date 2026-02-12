jest.mock("../../src/common/configService", () => ({
  getCimitStubTtl: jest.fn().mockResolvedValue(1800),
  getPendingMitigationsTableName: jest
    .fn()
    .mockReturnValue("mock-pending-table"),
}));

jest.mock("../../src/clients/dynamoDBClient", () => ({
  dynamoDBClient: {
    putItem: jest.fn(),
    getItem: jest.fn(),
  },
}));

jest.mock("../../src/common/cimitStubItemService", () => ({
  getCiForUserId: jest.fn(),
  persistCimitStubItem: jest.fn(),
}));

import { dynamoDBClient } from "../../src/clients/dynamoDBClient";
import * as pendingMitigationService from "../../src/common/pendingMitigationService";
import * as cimitStubItemService from "../../src/common/cimitStubItemService";
import { CimitStubItem } from "../../src/common/contraIndicatorTypes";

describe("pendingMitigationService", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("persistPendingMitigation", () => {
    it("should create new pending mitigation item", async () => {
      const request = {
        mitigations: ["M01", "M02"],
        vcJti: "someRandomId",
      };

      await pendingMitigationService.persistPendingMitigation(
        request,
        "CI",
        "POST",
      );

      expect(dynamoDBClient.putItem).toHaveBeenCalledTimes(1);
      const callArgs = (dynamoDBClient.putItem as jest.Mock).mock.calls[0][0];
      expect(callArgs).toEqual({
        TableName: "mock-pending-table",
        Item: {
          vcJti: { S: "someRandomId" },
          mitigatedCi: { S: "CI" },
          mitigationCodes: { L: [{ S: "M01" }, { S: "M02" }] },
          requestMethod: { S: "POST" },
          ttl: { N: expect.any(String) },
        },
      });
    });
  });

  describe("completePendingMitigation", () => {
    const cimitStubItem: CimitStubItem = {
      userId: "aUserId",
      contraIndicatorCode: "CI",
      issuanceDate: "2023-08-17T10:20:53.000Z",
      mitigations: [],
      sortKey: "",
      ttl: 0,
      issuer: "",
      document: "",
      txn: "",
    };

    const createPendingMitigation = (requestMethod: string) => ({
      vcJti: { S: "aJwtId" },
      mitigatedCi: { S: "CI" },
      mitigationCodes: { L: [{ S: "M01" }, { S: "M02" }] },
      requestMethod: { S: requestMethod },
      ttl: { N: "123456" },
    });

    it("should update cimit item with POST method", async () => {
      (dynamoDBClient.getItem as jest.Mock).mockResolvedValueOnce({
        Item: createPendingMitigation("POST"),
      });
      (cimitStubItemService.getCiForUserId as jest.Mock).mockResolvedValueOnce([
        cimitStubItem,
      ]);

      await pendingMitigationService.completePendingMitigation(
        "aJwtId",
        "aUserId",
      );

      expect(dynamoDBClient.getItem).toHaveBeenCalledWith({
        TableName: "mock-pending-table",
        Key: { vcJti: { S: "aJwtId" } },
      });
      expect(cimitStubItemService.getCiForUserId).toHaveBeenCalledWith(
        "aUserId",
        "CI",
      );
      expect(cimitStubItemService.persistCimitStubItem).toHaveBeenCalledTimes(
        1,
      );
      const updatedItem = (
        cimitStubItemService.persistCimitStubItem as jest.Mock
      ).mock.calls[0][0];
      expect(updatedItem.mitigations).toEqual(["M01", "M02"]);
    });

    it("should merge mitigations with POST method", async () => {
      const itemWithMitigations = { ...cimitStubItem, mitigations: ["M03"] };

      (dynamoDBClient.getItem as jest.Mock).mockResolvedValueOnce({
        Item: createPendingMitigation("POST"),
      });
      (cimitStubItemService.getCiForUserId as jest.Mock).mockResolvedValueOnce([
        itemWithMitigations,
      ]);

      await pendingMitigationService.completePendingMitigation(
        "aJwtId",
        "aUserId",
      );

      expect(dynamoDBClient.getItem).toHaveBeenCalledWith({
        TableName: "mock-pending-table",
        Key: { vcJti: { S: "aJwtId" } },
      });
      expect(cimitStubItemService.getCiForUserId).toHaveBeenCalledWith(
        "aUserId",
        "CI",
      );
      expect(cimitStubItemService.persistCimitStubItem).toHaveBeenCalledTimes(
        1,
      );
      const updatedItem = (
        cimitStubItemService.persistCimitStubItem as jest.Mock
      ).mock.calls[0][0];
      expect(updatedItem.mitigations).toEqual(["M01", "M02", "M03"]);
    });

    it("should replace mitigations with PUT method", async () => {
      (dynamoDBClient.getItem as jest.Mock).mockResolvedValueOnce({
        Item: createPendingMitigation("PUT"),
      });
      (cimitStubItemService.getCiForUserId as jest.Mock).mockResolvedValueOnce([
        cimitStubItem,
      ]);

      await pendingMitigationService.completePendingMitigation(
        "aJwtId",
        "aUserId",
      );

      expect(dynamoDBClient.getItem).toHaveBeenCalledWith({
        TableName: "mock-pending-table",
        Key: { vcJti: { S: "aJwtId" } },
      });
      expect(cimitStubItemService.getCiForUserId).toHaveBeenCalledWith(
        "aUserId",
        "CI",
      );
      expect(cimitStubItemService.persistCimitStubItem).toHaveBeenCalledTimes(
        1,
      );
      const updatedItem = (
        cimitStubItemService.persistCimitStubItem as jest.Mock
      ).mock.calls[0][0];
      expect(updatedItem.mitigations).toEqual(["M01", "M02"]);
    });

    it("should replace existing mitigations with PUT method", async () => {
      const itemWithMitigations = { ...cimitStubItem, mitigations: ["M03"] };

      (dynamoDBClient.getItem as jest.Mock).mockResolvedValueOnce({
        Item: createPendingMitigation("PUT"),
      });
      (cimitStubItemService.getCiForUserId as jest.Mock).mockResolvedValueOnce([
        itemWithMitigations,
      ]);

      await pendingMitigationService.completePendingMitigation(
        "aJwtId",
        "aUserId",
      );

      expect(dynamoDBClient.getItem).toHaveBeenCalledWith({
        TableName: "mock-pending-table",
        Key: { vcJti: { S: "aJwtId" } },
      });
      expect(cimitStubItemService.getCiForUserId).toHaveBeenCalledWith(
        "aUserId",
        "CI",
      );
      expect(cimitStubItemService.persistCimitStubItem).toHaveBeenCalledTimes(
        1,
      );
      const updatedItem = (
        cimitStubItemService.persistCimitStubItem as jest.Mock
      ).mock.calls[0][0];
      expect(updatedItem.mitigations).toEqual(["M01", "M02"]);
    });

    it("should do nothing if no pending mitigation found", async () => {
      (dynamoDBClient.getItem as jest.Mock).mockResolvedValueOnce({});

      await pendingMitigationService.completePendingMitigation(
        "aJwtId",
        "aUserId",
      );

      expect(dynamoDBClient.getItem).toHaveBeenCalledWith({
        TableName: "mock-pending-table",
        Key: { vcJti: { S: "aJwtId" } },
      });
      expect(cimitStubItemService.getCiForUserId).not.toHaveBeenCalled();
      expect(cimitStubItemService.persistCimitStubItem).not.toHaveBeenCalled();
    });

    it("should do nothing if no CI found", async () => {
      (dynamoDBClient.getItem as jest.Mock).mockResolvedValueOnce({
        Item: createPendingMitigation("POST"),
      });
      (cimitStubItemService.getCiForUserId as jest.Mock).mockResolvedValueOnce(
        [],
      );

      await pendingMitigationService.completePendingMitigation(
        "aJwtId",
        "aUserId",
      );

      expect(dynamoDBClient.getItem).toHaveBeenCalledWith({
        TableName: "mock-pending-table",
        Key: { vcJti: { S: "aJwtId" } },
      });
      expect(cimitStubItemService.getCiForUserId).toHaveBeenCalledWith(
        "aUserId",
        "CI",
      );
      expect(cimitStubItemService.persistCimitStubItem).not.toHaveBeenCalled();
    });

    it("should throw if unsupported method", async () => {
      (dynamoDBClient.getItem as jest.Mock).mockResolvedValueOnce({
        Item: createPendingMitigation("DELETE"),
      });
      (cimitStubItemService.getCiForUserId as jest.Mock).mockResolvedValueOnce([
        cimitStubItem,
      ]);

      await expect(
        pendingMitigationService.completePendingMitigation("aJwtId", "aUserId"),
      ).rejects.toThrow("Method not supported: DELETE");

      expect(dynamoDBClient.getItem).toHaveBeenCalledWith({
        TableName: "mock-pending-table",
        Key: { vcJti: { S: "aJwtId" } },
      });
      expect(cimitStubItemService.getCiForUserId).toHaveBeenCalledWith(
        "aUserId",
        "CI",
      );
    });
  });
});
