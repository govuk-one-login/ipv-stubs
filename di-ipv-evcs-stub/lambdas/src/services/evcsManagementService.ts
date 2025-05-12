import EvcsStoredIdentityItem from "../model/storedIdentityItem";
import { DynamoDB, QueryInput } from "@aws-sdk/client-dynamodb";
import { config } from "../common/config";
import { marshall, unmarshall } from "@aws-sdk/util-dynamodb";
import { GetStoredIdentity } from "../domain/serviceResponse";
import { StatusCodes } from "../domain/enums";

const dynamoClient = config.isLocalDev
  ? new DynamoDB({
      endpoint: config.localDynamoDbEndpoint,
      region: config.region,
    })
  : new DynamoDB({ region: config.region });

export async function processGetStoredIdentity(
  userId: string,
): Promise<GetStoredIdentity> {
  console.info("Getting user's stored identity record");

  const getItemInput: QueryInput = {
    TableName: config.evcsStoredIdentityObjectTableName,
    KeyConditionExpression: "#userId = :userIdValue",
    ExpressionAttributeValues: {
      ":userIdValue": marshall(userId),
    },
  };

  const response = await dynamoClient.query(getItemInput);

  if (!response.Items || response.Items.length === 0) {
    return {
      statusCode: StatusCodes.NotFound,
    };
  }

  const { jwtSignature, storedIdentity, levelOfConfidence, isValid } =
    unmarshall(response.Items[0]) as EvcsStoredIdentityItem;

  return {
    response: {
      userId,
      jwtSignature,
      storedIdentity,
      levelOfConfidence,
      isValid,
    },
    statusCode: StatusCodes.Success,
  };
}
