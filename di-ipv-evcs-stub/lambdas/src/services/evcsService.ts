import { DynamoDB, GetItemInput, PutItemInput } from "@aws-sdk/client-dynamodb";
import { marshall, unmarshall } from "@aws-sdk/util-dynamodb";

import PostRequest from "../domain/postRequest";
import VcStateMetadata from "../domain/vcStateMetadata";
import ServiceResponse from "../domain/serviceResponse";
import EvcsVcItem from "../model/evcsVcItem";

import { config } from "../common/config";
import { getSsmParameter } from "../common/ssmParameter";
import { v4 as uuid } from "uuid";

const dynamoClient = config.isLocalDev
  ? new DynamoDB({
      endpoint: config.localDynamoDbEndpoint,
      region: config.region,
    })
  : new DynamoDB({ region: config.region });

export async function processPostUserVCsRequest(
    userId: string,
    postRequest: PostRequest
  ): Promise<ServiceResponse> {
  console.info(`Post user record.`);
  for (const persistVC of postRequest.persistVCs) {
      const vcItem: EvcsVcItem = {
          userId: userId,
          vc: persistVC.vc,
          state: persistVC.state,
          metadata: persistVC.metadata!,
          provenience: persistVC.provenience!,
          ttl: await getTtl(),
        };
        await saveUserVC(vcItem);
  }

  return {
    response: {
      messageId: uuid()
    },
    statusCode: 202
  };
}

export async function processGetUserVCsRequest(
  userId: string
): Promise<ServiceResponse> {
  console.info(`Get user record.`);
  const getItemInput: GetItemInput = {
    TableName: config.evcsStubUserVCsTableName,
    Key: marshall({
      userId: userId,
    }),
  };
  const items = (await dynamoClient.scan(getItemInput)).Items;
  console.info(`Total VCs retrived - ${JSON.stringify(items?.length)}`);
  const evcsVcItems: EvcsVcItem[] = [];
  items?.forEach((item) => {
    evcsVcItems.push(unmarshall(item) as EvcsVcItem);
  });

  const vcItems: Array<VcStateMetadata> = [];
  for (const item of evcsVcItems) {
    const vcItem: VcStateMetadata = {
        vc: item.vc,
        state: item.state,
        metadata: item.metadata!,
    };
    vcItems.push(vcItem);
  }

  return {
    response: {
      vcs: vcItems,
      afterKey: "chk what to pass"
    },
    statusCode: 200
  };
}

async function saveUserVC(evcsVcItem: EvcsVcItem) {
  console.info(`Save user vc.`);
  const putItemInput: PutItemInput = {
    TableName: config.evcsStubUserVCsTableName,
    Item: marshall(evcsVcItem,  {
        removeUndefinedValues: true
      }
    ),
  };
  await dynamoClient.putItem(putItemInput);
}

async function getTtl(): Promise<number> {
  const evcsTtlSeconds: number = parseInt(await getSsmParameter(config.evcsParamBasePath + "evcsStubTtl"))
  return Math.floor(Date.now() / 1000) + evcsTtlSeconds;
}