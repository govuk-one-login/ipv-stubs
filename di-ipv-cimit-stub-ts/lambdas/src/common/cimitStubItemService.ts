import { CimitStubItem } from "./contraIndicatorTypes";
import { dynamoDBClient } from "../clients/dynamoDBClient";
import * as config from "./configService";
import { marshall } from "@aws-sdk/util-dynamodb";

const tableName = config.getCimitStubTableName();

export const persistCimitStubItem = async (
  cimitStubItem: CimitStubItem,
): Promise<void> => {
  await dynamoDBClient.putItem({
    TableName: tableName,
    Item: marshall(cimitStubItem, {
      removeUndefinedValues: true,
    }),
  });
};

export const calculateSortKey = (
  code: string,
  issuanceDate: string,
): string => {
  return code + "#" + issuanceDate;
};

export const calculateTtl = async (): Promise<number> => {
  const now = new Date();
  const nowInSeconds = Math.floor(now.getTime() / 1000);
  const ttl = await config.getCimitStubTtl();
  return nowInSeconds + ttl;
};
