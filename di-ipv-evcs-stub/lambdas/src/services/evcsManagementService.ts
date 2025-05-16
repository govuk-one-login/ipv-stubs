import { QueryInput } from "@aws-sdk/client-dynamodb";
import { config } from "../common/config";
import { marshall, unmarshall } from "@aws-sdk/util-dynamodb";
import { GetStoredIdentity } from "../domain/serviceResponse";
import { dynamoClient } from "../clients/dynamodbClient";

export async function processGetStoredIdentity(
  userId: string,
): Promise<GetStoredIdentity> {
  console.info("Getting user's stored identity record");

  const getItemInput: QueryInput = {
    TableName: config.evcsStoredIdentityObjectTableName,
    KeyConditionExpression: "userId = :userIdValue",
    ExpressionAttributeValues: {
      ":userIdValue": marshall(userId),
    },
  };

  const response = await dynamoClient.query(getItemInput);

  if (!response.Items || response.Items.length === 0) {
    return {
      storedIdentities: [],
    };
  }

  const parsedResponse = response.Items.map((siItem) => {
    const { recordType, storedIdentity, levelOfConfidence, isValid } =
      unmarshall(siItem);
    return {
      userId,
      recordType,
      storedIdentity,
      levelOfConfidence,
      isValid,
    };
  });

  return {
    storedIdentities: parsedResponse,
  };
}
