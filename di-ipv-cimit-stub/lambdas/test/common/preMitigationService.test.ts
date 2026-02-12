jest.mock("../../src/common/configService", () => ({
  getCimitStubTtl: jest.fn().mockResolvedValue(1800),
  getPreMitigationsTableName: jest.fn().mockReturnValue("mock-pre-mitigations-table"),
}));

jest.mock("../../src/clients/dynamoDBClient", () => ({
  dynamoDBClient: {
    putItem: jest.fn(),
    query: jest.fn(),
  },
}));

import { dynamoDBClient } from "../../src/clients/dynamoDBClient";
import * as preMitigationService from "../../src/common/preMitigationService";
import { CimitStubItem } from "../../src/common/contraIndicatorTypes";

describe("preMitigationService", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("persistPreMitigation", () => {
    it("should create pre-mitigation item", async () => {
      const request = { mitigations: ["M01", "M02"] };

      await preMitigationService.persistPreMitigation("userId", "CI", request);

      expect(dynamoDBClient.putItem).toHaveBeenCalledTimes(1);
      const callArgs = (dynamoDBClient.putItem as jest.Mock).mock.calls[0][0];
      expect(callArgs).toEqual({
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
    it("should apply pre-mitigations to matching CIs", async () => {
      const cimitItems: CimitStubItem[] = [
        {
          userId: "userId",
          contraIndicatorCode: "CI01",
          issuanceDate: "2023-08-17T10:20:53.000Z",
          mitigations: [],
          sortKey: "",
          ttl: 0,
          issuer: "",
          document: "",
          txn: "",
        },
      ];

      (dynamoDBClient.query as jest.Mock).mockResolvedValueOnce({
        Items: [
          {
            userId: { S: "userId" },
            mitigatedCi: { S: "CI01" },
            mitigationCodes: { L: [{ S: "M01" }] },
            ttl: { N: "123456" },
          },
        ],
      });

      await preMitigationService.applyPreMitigationsToItems("userId", cimitItems);

      expect(cimitItems[0].mitigations).toEqual(["M01"]);
    });

    it("should merge pre-mitigations with existing mitigations", async () => {
      const cimitItems: CimitStubItem[] = [
        {
          userId: "userId",
          contraIndicatorCode: "CI01",
          issuanceDate: "2023-08-17T10:20:53.000Z",
          mitigations: ["M02"],
          sortKey: "",
          ttl: 0,
          issuer: "",
          document: "",
          txn: "",
        },
      ];

      (dynamoDBClient.query as jest.Mock).mockResolvedValueOnce({
        Items: [
          {
            userId: { S: "userId" },
            mitigatedCi: { S: "CI01" },
            mitigationCodes: { L: [{ S: "M01" }] },
            ttl: { N: "123456" },
          },
        ],
      });

      await preMitigationService.applyPreMitigationsToItems("userId", cimitItems);

      expect(cimitItems[0].mitigations).toEqual(["M01", "M02"]);
    });

    it("should not add duplicate mitigations", async () => {
      const cimitItems: CimitStubItem[] = [
        {
          userId: "userId",
          contraIndicatorCode: "CI01",
          issuanceDate: "2023-08-17T10:20:53.000Z",
          mitigations: ["M01"],
          sortKey: "",
          ttl: 0,
          issuer: "",
          document: "",
          txn: "",
        },
      ];

      (dynamoDBClient.query as jest.Mock).mockResolvedValueOnce({
        Items: [
          {
            userId: { S: "userId" },
            mitigatedCi: { S: "CI01" },
            mitigationCodes: { L: [{ S: "M01" }] },
            ttl: { N: "123456" },
          },
        ],
      });

      await preMitigationService.applyPreMitigationsToItems("userId", cimitItems);

      expect(cimitItems[0].mitigations).toEqual(["M01"]);
    });

    it("should not apply pre-mitigations to non-matching CIs", async () => {
      const cimitItems: CimitStubItem[] = [
        {
          userId: "userId",
          contraIndicatorCode: "CI02",
          issuanceDate: "2023-08-17T10:20:53.000Z",
          mitigations: [],
          sortKey: "",
          ttl: 0,
          issuer: "",
          document: "",
          txn: "",
        },
      ];

      (dynamoDBClient.query as jest.Mock).mockResolvedValueOnce({
        Items: [
          {
            userId: { S: "userId" },
            mitigatedCi: { S: "CI01" },
            mitigationCodes: { L: [{ S: "M01" }] },
            ttl: { N: "123456" },
          },
        ],
      });

      await preMitigationService.applyPreMitigationsToItems("userId", cimitItems);

      expect(cimitItems[0].mitigations).toEqual([]);
    });

    it("should handle empty pre-mitigations", async () => {
      const cimitItems: CimitStubItem[] = [
        {
          userId: "userId",
          contraIndicatorCode: "CI01",
          issuanceDate: "2023-08-17T10:20:53.000Z",
          mitigations: [],
          sortKey: "",
          ttl: 0,
          issuer: "",
          document: "",
          txn: "",
        },
      ];

      (dynamoDBClient.query as jest.Mock).mockResolvedValueOnce({ Items: [] });

      await preMitigationService.applyPreMitigationsToItems("userId", cimitItems);

      expect(cimitItems[0].mitigations).toEqual([]);
    });
  });
});
