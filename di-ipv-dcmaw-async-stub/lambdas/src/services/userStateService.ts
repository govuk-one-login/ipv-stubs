import {
  DynamoDB,
  GetItemInput,
  UpdateItemInput,
  DeleteItemInput
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
    UpdateExpression: "set #state = :state",
    ExpressionAttributeNames: {
      "#state": "state",
    },
    ExpressionAttributeValues: marshall({
      ":state": state,
    }),
  };
  await dynamoClient.updateItem(updateItemInput);
}

/** Gets state value and deletes record. */
export async function popState(userId: string): Promise<string | null> {
  const getItemInput: GetItemInput = {
    TableName: userStateTableName,
    Key: marshall({ userId }),
  };
  const { Item } = await dynamoClient.getItem(getItemInput);
  const userStateItem = Item ? (unmarshall(Item) as UserStateItem) : null;
  if (userStateItem === null) {
    throw new Error(`No state record found for user id ${userId}`);
  }
  await dynamoClient.deleteItem(getItemInput);
  return userStateItem.state;
}
