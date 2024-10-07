import { DynamoDB, GetItemInput, PutItemInput } from "@aws-sdk/client-dynamodb";
import { marshall, unmarshall } from "@aws-sdk/util-dynamodb";

import { getSsmParameter } from "../../common/ssmParameter";
import UserEvidenceItem from "../model/userEvidenceItem";
import { config } from "../../common/config";
import TicfManagementRequest from "../../domain/ticfManagementRequest";

const dynamoClient = config.isLocalDev
  ? new DynamoDB({
      endpoint: config.localDynamoDbEndpoint,
      region: config.region,
    })
  : new DynamoDB({ region: config.region });

export async function persistUserEvidence(
  userId: string,
  ticfManagementRequest: TicfManagementRequest,
): Promise<void> {
  const { responseDelay, statusCode, evidence } = ticfManagementRequest;

  const userEvidence: UserEvidenceItem = {
    userId: userId,
    evidence,
    ttl: await getTtl(),
    statusCode: statusCode || 200,
    responseDelay: responseDelay || 0,
  };
  await saveUserEvidence(userEvidence);
}

export async function getUserEvidence(
  userId: string,
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
    Item: marshall(userEvidence, { removeUndefinedValues: true }),
  };
  await dynamoClient.putItem(putItemInput);
}

async function getTtl(): Promise<number> {
  const ticfTtlSeconds: number = parseInt(
    await getSsmParameter(config.ticfParamBasePath + "ticfStubTtl"),
  );
  return Math.floor(Date.now() / 1000) + ticfTtlSeconds;
}

export default persistUserEvidence;
