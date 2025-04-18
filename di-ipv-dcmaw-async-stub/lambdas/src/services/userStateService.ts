import {
  DynamoDB,
  GetItemInput,
  UpdateItemInput,
} from "@aws-sdk/client-dynamodb";
import { marshall, unmarshall } from "@aws-sdk/util-dynamodb";
import { getEnvironmentVariable } from "../common/config";

const dynamoClient = new DynamoDB({ region: "eu-west-2" });
const userStateTableName = getEnvironmentVariable(
  "DCMAW_ASYNC_STUB_USER_STATE_TABLE_NAME",
);

type UserStateItem = {
  userId: string;
  state: string;
};

/** Overrides existing state value if record already exists for user. */
export async function persistState(
  userId: string,
  state: string,
): Promise<void> {
  const updateItemInput: UpdateItemInput = {
    TableName: userStateTableName,
    Key: marshall({ userId }),
    UpdateExpression: "set #state = :state, #ttl = :ttl",
    ExpressionAttributeNames: {
      "#state": "state",
      "#ttl": "ttl",
    },
    ExpressionAttributeValues: marshall({
      ":state": state,
      ":ttl": Math.floor(Date.now() / 1000) + 3600, // epoch timestamp in seconds
    }),
  };
  await dynamoClient.updateItem(updateItemInput);
}

/** Gets state record. */
export async function getState(userId: string): Promise<string | null> {
  const getItemInput: GetItemInput = {
    TableName: userStateTableName,
    Key: marshall({ userId }),
  };
  const { Item } = await dynamoClient.getItem(getItemInput);
  const userStateItem = Item ? (unmarshall(Item) as UserStateItem) : null;
  if (userStateItem === null) {
    throw new Error(`No state record found for user id ${userId}`);
  }
  return userStateItem.state;
}

/** Deletes state record. */
export async function deleteState(userId: string): Promise<void> {
  const getItemInput: GetItemInput = {
    TableName: userStateTableName,
    Key: marshall({ userId }),
  };
  await dynamoClient.deleteItem(getItemInput);
}
