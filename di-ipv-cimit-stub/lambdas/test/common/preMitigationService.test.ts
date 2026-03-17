import { describe, it, expect, vi, beforeEach } from "vitest";
import { mockClient } from "aws-sdk-client-mock";
import {
  DynamoDB,
  PutItemCommand,
  QueryCommand,
} from "@aws-sdk/client-dynamodb";
import "aws-sdk-client-mock-vitest";

vi.mock("../../src/common/configService", () => ({
  isRunningLocally: false,
  getCimitStubTtl: vi.fn().mockResolvedValue(1800),
  getPreMitigationsTableName: vi
    .fn()
    .mockReturnValue("mock-pre-mitigations-table"),
}));
import * as preMitigationService from "../../src/common/preMitigationService";
import { CimitStubItem } from "../../src/common/contraIndicatorTypes";

const dbMock = mockClient(DynamoDB);

describe("preMitigationService", () => {
  beforeEach(() => {
    dbMock.reset();
    vi.clearAllMocks();
  });

  describe("persistPreMitigation", () => {
    it("should create pre-mitigation item", async () => {
      const request = { mitigations: ["M01", "M02"] };

      await preMitigationService.persistPreMitigation("userId", "CI", request);

      expect(dbMock).toHaveReceivedCommandWith(PutItemCommand, {
        TableName: "mock-pre-mitigations-table",
        Item: {
          userId: { S: "userId" },
          mitigatedCi: { S: "CI" },
          mitigationCodes: { L: [{ S: "M01" }, { S: "M02" }] },
          ttl: { N: expect.any(String) },
        },
      });
    });
  });

  describe("applyPreMitigationsToItems", () => {
    const createCimitItem = (
      code: string,
      mitigations: string[] = [],
    ): CimitStubItem => ({
      userId: "userId",
      contraIndicatorCode: code,
      issuanceDate: "2023-08-17T10:20:53.000Z",
      mitigations,
      sortKey: "",
      ttl: 0,
      issuer: "",
      document: "",
      txn: "",
    });

    it("should apply pre-mitigations to matching CIs", async () => {
      const cimitItems = [createCimitItem("CI01")];

      dbMock.on(QueryCommand).resolves({
        Items: [
          {
            userId: { S: "userId" },
            mitigatedCi: { S: "CI01" },
            mitigationCodes: { L: [{ S: "M01" }] },
            ttl: { N: "123456" },
          },
        ],
      });

      await preMitigationService.applyPreMitigationsToItems(
        "userId",
        cimitItems,
      );

      expect(cimitItems[0].mitigations).toEqual(["M01"]);
    });

    it("should merge pre-mitigations with existing mitigations", async () => {
      const cimitItems = [createCimitItem("CI01", ["M02"])];

      dbMock.on(QueryCommand).resolves({
        Items: [
          {
            userId: { S: "userId" },
            mitigatedCi: { S: "CI01" },
            mitigationCodes: { L: [{ S: "M01" }] },
            ttl: { N: "123456" },
          },
        ],
      });

      await preMitigationService.applyPreMitigationsToItems(
        "userId",
        cimitItems,
      );

      expect(cimitItems[0].mitigations).toEqual(["M01", "M02"]);
    });

    it("should not add duplicate mitigations", async () => {
      const cimitItems = [createCimitItem("CI01", ["M01"])];

      dbMock.on(QueryCommand).resolves({
        Items: [
          {
            userId: { S: "userId" },
            mitigatedCi: { S: "CI01" },
            mitigationCodes: { L: [{ S: "M01" }] },
            ttl: { N: "123456" },
          },
        ],
      });

      await preMitigationService.applyPreMitigationsToItems(
        "userId",
        cimitItems,
      );

      expect(cimitItems[0].mitigations).toEqual(["M01"]);
    });

    it("should not apply pre-mitigations to non-matching CIs", async () => {
      const cimitItems = [createCimitItem("CI02")];

      dbMock.on(QueryCommand).resolves({
        Items: [
          {
            userId: { S: "userId" },
            mitigatedCi: { S: "CI01" },
            mitigationCodes: { L: [{ S: "M01" }] },
            ttl: { N: "123456" },
          },
        ],
      });

      await preMitigationService.applyPreMitigationsToItems(
        "userId",
        cimitItems,
      );

      expect(cimitItems[0].mitigations).toEqual([]);
    });

    it("should handle empty pre-mitigations", async () => {
      const cimitItems = [createCimitItem("CI01")];

      dbMock.on(QueryCommand).resolves({ Items: [] });

      await preMitigationService.applyPreMitigationsToItems(
        "userId",
        cimitItems,
      );

      expect(cimitItems[0].mitigations).toEqual([]);
    });
  });
});
