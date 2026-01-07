jest.mock("../../../src/common/configService", () => ({
  getCimitStubTableName: jest.fn().mockReturnValue("mock-table"),
  getCimitStubTtl: jest.fn().mockResolvedValue(1800),
}));

jest.mock("../../../src/clients/dynamoDBClient", () => ({
  dynamoDBClient: {
    query: jest.fn(),
    putItem: jest.fn(),
    deleteItem: jest.fn(),
  },
}));

import { dynamoDBClient } from "../../../src/clients/dynamoDBClient";
import * as cimitStubItemService from "../../../src/common/cimitStubItemService";
import { CimitStubItem } from "../../../src/common/contraIndicatorTypes";
import { marshall } from "@aws-sdk/util-dynamodb";

const USER_ID = "user-id-1";

beforeEach(() => {
  jest.clearAllMocks();
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

    (dynamoDBClient.query as jest.Mock).mockResolvedValue({
      Items: mockItems,
    });

    const result = await cimitStubItemService.getCIsForUserId(USER_ID);

    expect(dynamoDBClient.query).toHaveBeenCalledWith({
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

    (dynamoDBClient.query as jest.Mock).mockResolvedValue({
      Items: mockItems,
    });

    const result = await cimitStubItemService.getCiForUserId(USER_ID, ciCode);

    expect(dynamoDBClient.query).toHaveBeenCalledWith({
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

    await cimitStubItemService.persistCimitStubItem(item);

    expect(dynamoDBClient.putItem).toHaveBeenCalledTimes(1);
    const callArgs = (dynamoDBClient.putItem as jest.Mock).mock.calls[0][0];
    expect(callArgs.TableName).toBe("mock-table");
    expect(callArgs.Item).toBeDefined();
    expect(callArgs.Item.ttl.N).not.toBe("0");
    expect(callArgs.Item.sortKey.S).toBe(ciCode + "#" + issuanceDate);
  });
});

describe("updateCimitStubItem", () => {
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

    await cimitStubItemService.updateCimitStubItem(item);

    expect(dynamoDBClient.putItem).toHaveBeenCalledTimes(1);
    const callArgs = (dynamoDBClient.putItem as jest.Mock).mock.calls[0][0];
    expect(callArgs.TableName).toBe("mock-table");
    expect(callArgs.Item).toBeDefined();
    expect(callArgs.Item.ttl.N).not.toBe("0");
    expect(callArgs.Item.sortKey.S).toBe("D01#2023-08-17T10:20:53.000Z");
  });
});

describe("deleteCimitStubItem", () => {
  it("should delete CimitStubItem by userId and sortKey", async () => {
    const sortKey = "D02#2023-08-17T10:20:53.000Z";

    await cimitStubItemService.deleteCimitStubItem(USER_ID, sortKey);

    expect(dynamoDBClient.deleteItem).toHaveBeenCalledWith({
      TableName: "mock-table",
      Key: marshall({
        userId: USER_ID,
        sortKey,
      }),
    });
  });
});
