import { QueryInput } from "@aws-sdk/client-dynamodb";
import { config } from "../config/config";
import { marshall, unmarshall } from "@aws-sdk/util-dynamodb";
import { dynamoClient } from "../clients/dynamodbClient";
import { UserIdentity } from "../domain/userIdentity";

export const getUserIdentity = async (
  userId: string,
): Promise<UserIdentity | null> => {
  console.info("Getting user's SIS record");

  console.log(config.evcsStoredIdentityObjectTableName);

  const getItemInput: QueryInput = {
    TableName: config.evcsStoredIdentityObjectTableName,
    KeyConditionExpression: "userId = :userIdValue",
    ExpressionAttributeValues: {
      ":userIdValue": marshall(userId),
    },
  };

  const response = await dynamoClient.query(getItemInput);

  if (!response.Items || response.Items.length === 0) {
    return null;
  }

  const parsedResponse = response.Items.map((siItem) => {
    const { storedIdentity, levelOfConfidence, isValid } = unmarshall(siItem);

    return {
      content: storedIdentity,
      vot: levelOfConfidence,
      isValid,
      // defaulting to false as the ttl is set to the default
      // retention of VCs which is 120 years
      expired: false,
    };
  });

  return parsedResponse[0];
};
