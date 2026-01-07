import { CimitStubItem } from "./contraIndicatorTypes";
import { dynamoDBClient } from "../clients/dynamoDBClient";
import * as config from "./configService";
import { marshall, unmarshall } from "@aws-sdk/util-dynamodb";

export async function getCIsForUserId(
  userId: string,
): Promise<CimitStubItem[]> {
  const result = await dynamoDBClient.query({
    TableName: config.getCimitStubTableName(),
    KeyConditionExpression: "userId = :userId",
    ExpressionAttributeValues: marshall({ ":userId": userId }),
  });

  return (result.Items || []).map((item) => unmarshall(item) as CimitStubItem);
}

export async function getCiForUserId(
  userId: string,
  ci: string,
): Promise<CimitStubItem[]> {
  const result = await dynamoDBClient.query({
    TableName: config.getCimitStubTableName(),
    KeyConditionExpression:
      "userId = :userId AND begins_with(sortKey, :prefix)",
    ExpressionAttributeValues: marshall({
      ":userId": userId,
      ":prefix": ci,
    }),
  });

  return (result.Items || []).map((item) => unmarshall(item) as CimitStubItem);
}

export async function persistCimitStubItem(
  cimitStubItem: CimitStubItem,
): Promise<void> {
  await setDynamoProperties(cimitStubItem);

  await dynamoDBClient.putItem({
    TableName: config.getCimitStubTableName(),
    Item: marshall(cimitStubItem, {
      removeUndefinedValues: true,
    }),
  });
}

export async function updateCimitStubItem(
  cimitStubItem: CimitStubItem,
): Promise<void> {
  await setDynamoProperties(cimitStubItem);

  await dynamoDBClient.putItem({
    TableName: config.getCimitStubTableName(),
    Item: marshall(cimitStubItem, {
      removeUndefinedValues: true,
    }),
  });
}

export async function deleteCimitStubItem(
  userId: string,
  sortKey: string,
): Promise<void> {
  await dynamoDBClient.deleteItem({
    TableName: config.getCimitStubTableName(),
    Key: marshall({
      userId,
      sortKey,
    }),
  });
}

async function setDynamoProperties(
  cimitStubItem: CimitStubItem,
): Promise<void> {
  cimitStubItem.sortKey =
    cimitStubItem.contraIndicatorCode + "#" + cimitStubItem.issuanceDate;

  const nowInSeconds = Math.floor(Date.now() / 1000);
  const ttlSeconds = await config.getCimitStubTtl();
  cimitStubItem.ttl = nowInSeconds + ttlSeconds;
}
