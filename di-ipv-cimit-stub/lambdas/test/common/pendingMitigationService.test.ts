import { describe, it, expect, vi, beforeEach } from "vitest";
import { mockClient } from "aws-sdk-client-mock";
import {
  DynamoDB,
  GetItemCommand,
  PutItemCommand,
} from "@aws-sdk/client-dynamodb";
import "aws-sdk-client-mock-vitest";

vi.mock("../../src/common/configService", () => ({
  isRunningLocally: false,
  getCimitStubTtl: vi.fn().mockResolvedValue(1800),
  getPendingMitigationsTableName: vi.fn().mockReturnValue("mock-pending-table"),
}));

vi.mock("../../src/common/cimitStubItemService", () => ({
  getCiForUserId: vi.fn(),
  persistCimitStubItem: vi.fn(),
}));

import * as pendingMitigationService from "../../src/common/pendingMitigationService";
import * as cimitStubItemService from "../../src/common/cimitStubItemService";
import { CimitStubItem } from "../../src/common/contraIndicatorTypes";

const dbMock = mockClient(DynamoDB);

describe("pendingMitigationService", () => {
  beforeEach(() => {
    dbMock.reset();
    vi.clearAllMocks();
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

      expect(dbMock).toHaveReceivedCommandWith(PutItemCommand, {
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
      dbMock.on(GetItemCommand).resolves({
        Item: createPendingMitigation("POST"),
      });
      vi.mocked(cimitStubItemService.getCiForUserId).mockResolvedValueOnce([
        cimitStubItem,
      ]);

      await pendingMitigationService.completePendingMitigation(
        "aJwtId",
        "aUserId",
      );

      expect(dbMock).toHaveReceivedCommandWith(GetItemCommand, {
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

      const updatedItem = vi.mocked(cimitStubItemService.persistCimitStubItem)
        .mock.calls[0][0];
      expect(updatedItem.mitigations).toEqual(["M01", "M02"]);
    });

    it("should merge mitigations with POST method", async () => {
      const itemWithMitigations = { ...cimitStubItem, mitigations: ["M03"] };

      dbMock.on(GetItemCommand).resolves({
        Item: createPendingMitigation("POST"),
      });
      vi.mocked(cimitStubItemService.getCiForUserId).mockResolvedValueOnce([
        itemWithMitigations,
      ]);

      await pendingMitigationService.completePendingMitigation(
        "aJwtId",
        "aUserId",
      );

      expect(dbMock).toHaveReceivedCommandWith(GetItemCommand, {
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

      const updatedItem = vi.mocked(cimitStubItemService.persistCimitStubItem)
        .mock.calls[0][0];
      expect(updatedItem.mitigations).toEqual(["M01", "M02", "M03"]);
    });

    it("should replace mitigations with PUT method", async () => {
      dbMock.on(GetItemCommand).resolves({
        Item: createPendingMitigation("PUT"),
      });
      vi.mocked(cimitStubItemService.getCiForUserId).mockResolvedValueOnce([
        cimitStubItem,
      ]);

      await pendingMitigationService.completePendingMitigation(
        "aJwtId",
        "aUserId",
      );

      expect(dbMock).toHaveReceivedCommandWith(GetItemCommand, {
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

      const updatedItem = vi.mocked(cimitStubItemService.persistCimitStubItem)
        .mock.calls[0][0];
      expect(updatedItem.mitigations).toEqual(["M01", "M02"]);
    });

    it("should replace existing mitigations with PUT method", async () => {
      const itemWithMitigations = { ...cimitStubItem, mitigations: ["M03"] };

      dbMock.on(GetItemCommand).resolves({
        Item: createPendingMitigation("PUT"),
      });
      vi.mocked(cimitStubItemService.getCiForUserId).mockResolvedValueOnce([
        itemWithMitigations,
      ]);

      await pendingMitigationService.completePendingMitigation(
        "aJwtId",
        "aUserId",
      );

      expect(dbMock).toHaveReceivedCommandWith(GetItemCommand, {
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

      const updatedItem = vi.mocked(cimitStubItemService.persistCimitStubItem)
        .mock.calls[0][0];
      expect(updatedItem.mitigations).toEqual(["M01", "M02"]);
    });

    it("should do nothing if no pending mitigation found", async () => {
      dbMock.on(GetItemCommand).resolves({});

      await pendingMitigationService.completePendingMitigation(
        "aJwtId",
        "aUserId",
      );

      expect(dbMock).toHaveReceivedCommandWith(GetItemCommand, {
        TableName: "mock-pending-table",
        Key: { vcJti: { S: "aJwtId" } },
      });
      expect(cimitStubItemService.getCiForUserId).not.toHaveBeenCalled();
      expect(cimitStubItemService.persistCimitStubItem).not.toHaveBeenCalled();
    });

    it("should do nothing if no CI found", async () => {
      dbMock.on(GetItemCommand).resolves({
        Item: createPendingMitigation("POST"),
      });
      vi.mocked(cimitStubItemService.getCiForUserId).mockResolvedValueOnce([]);

      await pendingMitigationService.completePendingMitigation(
        "aJwtId",
        "aUserId",
      );

      expect(dbMock).toHaveReceivedCommandWith(GetItemCommand, {
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
      dbMock.on(GetItemCommand).resolves({
        Item: createPendingMitigation("DELETE"),
      });
      vi.mocked(cimitStubItemService.getCiForUserId).mockResolvedValueOnce([
        cimitStubItem,
      ]);

      await expect(
        pendingMitigationService.completePendingMitigation("aJwtId", "aUserId"),
      ).rejects.toThrow("Method not supported: DELETE");

      expect(dbMock).toHaveReceivedCommandWith(GetItemCommand, {
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
