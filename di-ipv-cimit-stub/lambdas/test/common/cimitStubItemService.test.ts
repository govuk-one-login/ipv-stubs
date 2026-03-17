import { describe, it, expect, vi, beforeEach } from "vitest";
import { mockClient } from "aws-sdk-client-mock";
import {
  DynamoDB,
  QueryCommand,
  PutItemCommand,
  DeleteItemCommand,
} from "@aws-sdk/client-dynamodb";
import "aws-sdk-client-mock-vitest";

vi.mock("../../src/common/configService", () => ({
  isRunningLocally: false,
  getCimitStubTableName: vi.fn().mockReturnValue("mock-table"),
  getCimitStubTtl: vi.fn().mockResolvedValue(1800),
}));

import * as cimitStubItemService from "../../src/common/cimitStubItemService";
import { CimitStubItem } from "../../src/common/contraIndicatorTypes";
import { marshall } from "@aws-sdk/util-dynamodb";

const USER_ID = "user-id-1";

const dbMock = mockClient(DynamoDB);

describe("cimitStubItemService", () => {
  beforeEach(() => {
    dbMock.reset();
    vi.clearAllMocks();
  });

  describe("getCIsForUserId", () => {
    it("should return CIs from DynamoDB for specific userId", async () => {
      const ciCode = "V03";
      const mockItems = [
        {
          userId: { S: USER_ID },
          contraIndicatorCode: { S: ciCode },
          sortKey: { S: `${ciCode}#2023-08-17T10:20:53.000Z` },
        },
      ];

      dbMock.on(QueryCommand).resolves({
        Items: mockItems,
      });

      const result = await cimitStubItemService.getCIsForUserId(USER_ID);

      expect(dbMock).toHaveReceivedCommandWith(QueryCommand, {
        TableName: "mock-table",
        KeyConditionExpression: "userId = :userId",
        ExpressionAttributeValues: marshall({ ":userId": USER_ID }),
      });
      expect(result).toHaveLength(1);
      expect(result[0].contraIndicatorCode).toBe(ciCode);
    });
  });

  describe("getCiForUserId", () => {
    it("should return CIs matching sortKey prefix", async () => {
      const ciCode = "D02";
      const mockItems = [
        {
          userId: { S: USER_ID },
          contraIndicatorCode: { S: ciCode },
          sortKey: { S: `${ciCode}#2023-08-17T10:20:53.000Z` },
        },
      ];

      dbMock.on(QueryCommand).resolves({
        Items: mockItems,
      });

      const result = await cimitStubItemService.getCiForUserId(USER_ID, ciCode);

      expect(dbMock).toHaveReceivedCommandWith(QueryCommand, {
        TableName: "mock-table",
        KeyConditionExpression:
          "userId = :userId AND begins_with(sortKey, :prefix)",
        ExpressionAttributeValues: marshall({
          ":userId": USER_ID,
          ":prefix": ciCode,
        }),
      });
      expect(result).toHaveLength(1);
    });
  });

  describe("persistCimitStubItem", () => {
    it("should create CimitStubItem with TTL", async () => {
      const ciCode = "V03";
      const mitigations = ["V01", "V03"];
      const issuanceDate = "2023-08-17T10:20:53.000Z";
      const issuer = "https://address-cri.stubs.account.gov.uk";
      const docId = "some/document/id";

      const item: CimitStubItem = {
        userId: USER_ID,
        contraIndicatorCode: ciCode,
        issuer,
        issuanceDate,
        mitigations,
        document: docId,
        sortKey: "",
        ttl: 0,
        txn: "",
      };

      dbMock.on(PutItemCommand).resolves({});

      await cimitStubItemService.persistCimitStubItem(item);

      expect(dbMock).toHaveReceivedCommand(PutItemCommand);

      const callArgs = dbMock.commandCalls(PutItemCommand)[0].args[0].input;
      expect(callArgs.TableName).toBe("mock-table");
      expect(callArgs.Item).toBeDefined();

      // @ts-expect-error - Accessing marshalled DynamoDB attribute structure
      expect(callArgs.Item.ttl.N).not.toBe("0");
      // @ts-expect-error - Accessing marshalled DynamoDB attribute structure
      expect(callArgs.Item.sortKey.S).toBe(ciCode + "#" + issuanceDate);
    });

    it("should update CimitStubItem with new TTL", async () => {
      const item: CimitStubItem = {
        userId: USER_ID,
        contraIndicatorCode: "D01",
        mitigations: ["V01", "V03"],
        issuanceDate: "2023-08-17T10:20:53.000Z",
        issuer: "https://test.example.com",
        document: "",
        sortKey: "",
        ttl: 0,
        txn: "",
      };

      dbMock.on(PutItemCommand).resolves({});

      await cimitStubItemService.persistCimitStubItem(item);

      expect(dbMock).toHaveReceivedCommand(PutItemCommand);

      const callArgs = dbMock.commandCalls(PutItemCommand)[0].args[0].input;

      // @ts-expect-error - Accessing marshalled DynamoDB attribute structure
      expect(callArgs.Item.ttl.N).not.toBe("0");
      // @ts-expect-error - Accessing marshalled DynamoDB attribute structure
      expect(callArgs.Item.sortKey.S).toBe("D01#2023-08-17T10:20:53.000Z");
    });
  });

  describe("deleteCimitStubItem", () => {
    it("should delete CimitStubItem by userId and sortKey", async () => {
      const sortKey = "D02#2023-08-17T10:20:53.000Z";

      dbMock.on(DeleteItemCommand).resolves({});

      await cimitStubItemService.deleteCimitStubItem(USER_ID, sortKey);

      expect(dbMock).toHaveReceivedCommandWith(DeleteItemCommand, {
        TableName: "mock-table",
        Key: marshall({
          userId: USER_ID,
          sortKey,
        }),
      });
    });
  });
});
