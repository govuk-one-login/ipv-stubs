import { DynamoDB, QueryInput, PutItemInput } from "@aws-sdk/client-dynamodb";
import { marshall, unmarshall } from "@aws-sdk/util-dynamodb";

import PostRequest from "../domain/postRequest";
import ServiceResponse from "../domain/serviceResponse";
import VcState from "../domain/enums/vcState";
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
          vcSignature: persistVC.vc.split('.')[2],
          state: persistVC.state,
          metadata: persistVC.metadata!,
          provenance: persistVC.provenance!,
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
  const getItemInput: QueryInput = {
    TableName: config.evcsStubUserVCsTableName,
    KeyConditionExpression: "#userId = :userIdValue",
    FilterExpression: "#state = :stateValue",
    ExpressionAttributeNames: {
      '#userId': 'userId',
      '#state': 'state'
    },
    ExpressionAttributeValues : {
      ':userIdValue': { S: userId },
      ':stateValue': { S: VcState.CURRENT }
    }
  };
  console.info(`Query Input - ${JSON.stringify(getItemInput)}`);
  const items = (await dynamoClient.query(getItemInput)).Items ?? [];
  console.info(`Total VCs retrived - ${items?.length}`);
  const vcItems = items
                  .map((item) => unmarshall(item) as EvcsVcItem)
                  .map((evcsItem) => ({
                    vc: evcsItem.vc,
                    state: evcsItem.state as VcState,
                    metadata: evcsItem.metadata,
                  }));

  return {
    response: {
      vcs: vcItems,
      afterKey: "pagination will be implemented later"
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