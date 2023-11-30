import {
  DynamoDB,
  GetItemInput,
  PutItemInput,
  UpdateItemInput,
} from "@aws-sdk/client-dynamodb";
import { marshall, unmarshall } from "@aws-sdk/util-dynamodb";

import { getSsmParameter } from "../../common/ssmParameter";
import TicfEvidenceItem from "../../domain/ticfEvidenceItem";
import UserEvidenceItem from "../model/userEvidenceItem";
import { config } from "../../common/config";

const dynamoClient = config.isLocalDev
  ? new DynamoDB({
      endpoint: config.localDynamoDbEndpoint,
      region: config.region,
    })
  : new DynamoDB({ region: config.region });

export async function persistUserEvidence(
  userId: string,
  ticfEvidenceItemReq: TicfEvidenceItem
): Promise<void> {
  const ticfStubTtl = getTtl(
    await getSsmParameter(config.ticfParamBasePath + "ticfStubTtl")
  );

  const existingUserEvidence = await getUserEvidence(userId);
  if (!existingUserEvidence) {
    const userEvidence: UserEvidenceItem = {
      userId: userId,
      evidence: ticfEvidenceItemReq,
      ttl: ticfStubTtl,
    };
    await saveUserEvidence(userEvidence);
  } else {
    await updateUserEvidence(userId, ticfEvidenceItemReq, ticfStubTtl);
  }
}

export async function getUserEvidence(
  userId: string
): Promise<UserEvidenceItem | null> {
  console.info(`Get user record.`);
  const getItemInput: GetItemInput = {
    TableName: config.ticfStubUserEvidenceTableName,
    Key: marshall({
      userId: userId,
    }),
  };
  const { Item } = await dynamoClient.getItem(getItemInput);
  const userEvidenceItem = Item ? (unmarshall(Item) as UserEvidenceItem) : null;
  if (userEvidenceItem === null) {
    console.info(`Record not found for user.`);
  }
  return userEvidenceItem;
}

async function saveUserEvidence(userEvidence: UserEvidenceItem) {
  console.info(`Save user record.`);
  const putItemInput: PutItemInput = {
    TableName: config.ticfStubUserEvidenceTableName,
    Item: marshall(userEvidence),
  };
  const { Attributes } = await dynamoClient.putItem(putItemInput);
  const userEvidenceItem = Attributes
    ? (unmarshall(Attributes) as UserEvidenceItem)
    : null;
  if (userEvidenceItem === null) {
    console.info(`User record not saved.`);
  }
  return userEvidenceItem;
}

async function updateUserEvidence(
  userId: string,
  updatedEvidence: TicfEvidenceItem,
  ticfStubTtl: number
) {
  console.info(`Update user record.`);
  const updateItemInput: UpdateItemInput = {
    TableName: config.ticfStubUserEvidenceTableName,
    Key: marshall({
      userId: userId,
    }),
    UpdateExpression: "set evidence = :updatedEvidence, #attrTtl = :updatedTtl",
    ExpressionAttributeNames: { "#attrTtl": "ttl" },
    ExpressionAttributeValues: marshall({
      ":updatedEvidence": updatedEvidence,
      ":updatedTtl": ticfStubTtl,
    }),
    ReturnValues: "ALL_NEW",
  };
  const { Attributes } = await dynamoClient.updateItem(updateItemInput);
  const userEvidenceItem = Attributes
    ? (unmarshall(Attributes) as UserEvidenceItem)
    : null;
  if (userEvidenceItem === null) {
    console.info(`User record not updated.`);
  }
  return userEvidenceItem;
}

function getTtl(ticfStubTtl: string): number {
  const newLocal = new Date();
  newLocal.setSeconds(parseInt(ticfStubTtl));
  return Math.floor(newLocal.getTime() / 1000);
}

export default persistUserEvidence;
