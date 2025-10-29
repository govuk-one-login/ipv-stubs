import { dynamoClient } from "../clients/dynamoDBClient";
import { QueryInput } from "@aws-sdk/client-dynamodb";
import { unmarshall } from "@aws-sdk/util-dynamodb";

// TODO get rid of this parameter if not used by story's end
export interface UserContraIndicatorsItem {
  userId: string;
  //sortKey: string;
  contraIndicatorCode: string;
  issuer: string;
  issuanceDate: number;
  // ttl: number;
  mitigations: string[];
  document: string;
  txn: string;
}

export const getCIsForUserID = async (
  user_id: string,
): Promise<UserContraIndicatorsItem[]> => {
  const expressionAttributeValues = { ":userIdValue": { S: user_id } };

  const userCIQuery: QueryInput = {
    TableName: process.env.CIMIT_STUB_TABLE_NAME,
    KeyConditionExpression: "#userId = :userIdValue",
    ExpressionAttributeNames: {
      "#userId": "userId",
    },
    ExpressionAttributeValues: expressionAttributeValues,
  };

  const userCIItems = (await dynamoClient.query(userCIQuery)).Items ?? [];

  return userCIItems
    .map((item) => unmarshall(item))
    .map((item) => ({
      userId: item.userId,
      sortKey: item.sortKey,
      contraIndicatorCode: item.contraIndicatorCode,
      issuer: item.issuer,
      ttl: item.ttl,
      mitigations: item.mitigations,
      document: item.document,
      txn: item.txn,
      issuanceDate: item.issuanceDate,
    }));
};
