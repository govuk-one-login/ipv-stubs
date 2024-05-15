import {
  DynamoDB,
  QueryInput,
  PutItemInput,
  UpdateItemInput,
  PutItemCommandOutput,
  UpdateItemCommandOutput,
} from "@aws-sdk/client-dynamodb";
import { marshall, unmarshall } from "@aws-sdk/util-dynamodb";

import PostRequest from "../domain/postRequest";
import ServiceResponse from "../domain/serviceResponse";
import { VcState } from "../domain/enums/vcState";
import EvcsVcItem from "../model/evcsVcItem";

import { config } from "../common/config";
import { getSsmParameter } from "../common/ssmParameter";
import { v4 as uuid } from "uuid";
import PatchRequest from "../domain/patchRequest";

const dynamoClient = config.isLocalDev
  ? new DynamoDB({
      endpoint: config.localDynamoDbEndpoint,
      region: config.region,
    })
  : new DynamoDB({ region: config.region });

export async function processPostUserVCsRequest(
  userId: string,
  postRequest: PostRequest,
): Promise<ServiceResponse> {
  let response: ServiceResponse = {
    response: {},
  };
  try {
    console.info(`Post user record.`);
    const allPromises: Promise<PutItemCommandOutput>[] = [];
    const ttl = await getTtl();
    for (const persistVC of postRequest.persistVCs) {
      const vcItem: EvcsVcItem = {
        userId,
        vc: persistVC.vc,
        vcSignature: persistVC.vc.split(".")[2],
        state: persistVC.state,
        metadata: persistVC.metadata!,
        provenance: persistVC.provenance!,
        ttl,
      };
      allPromises.push(saveUserVC(vcItem));
    }

    console.info(`Saving all user VC's.`);
    await Promise.all(allPromises);
    response = {
      response: {
        messageId: uuid(),
      },
      statusCode: 202,
    };
  } catch (error) {
    console.error(error);
    response = {
      response: {
        messageId: "",
      },
      statusCode: 500,
    };
  }
  return response;
}

export async function processGetUserVCsRequest(
  userId: string,
): Promise<ServiceResponse> {
  console.info(`Get user record.`);
  const getItemInput: QueryInput = {
    TableName: config.evcsStubUserVCsTableName,
    KeyConditionExpression: "#userId = :userIdValue",
    FilterExpression: "#state = :stateValue",
    ExpressionAttributeNames: {
      "#userId": "userId",
      "#state": "state",
    },
    ExpressionAttributeValues: {
      ":userIdValue": { S: userId },
      ":stateValue": { S: VcState.CURRENT },
    },
  };
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
      afterKey: "pagination will be implemented later",
    },
    statusCode: 200,
  };
}

export async function processPatchUserVCsRequest(
  userId: string,
  postRequest: PatchRequest,
): Promise<ServiceResponse> {
  let response: ServiceResponse = {
    response: {},
  };
  try {
    console.info(`Patch user record.`);
    const allPromises: Promise<UpdateItemCommandOutput>[] = [];
    const ttl = await getTtl();
    for (const updateVC of postRequest.updateVCs) {
      const vcItem: EvcsVcItem = {
        userId: userId,
        vcSignature: updateVC.signature,
        state: updateVC.state,
        metadata: updateVC.metadata!,
        ttl,
      };
      allPromises.push(updateUserVC(vcItem));
    }

    console.info(`Updating user VC's.`);
    await Promise.all(allPromises);
    response = {
      response: {
        // messageId: uuid(),
      },
      statusCode: 204,
    };
  } catch (error) {
    response = {
      response: {
        // messageId: uuid(),
      },
      statusCode: 500,
    };
  }

  return response;
}

async function saveUserVC(evcsVcItem: EvcsVcItem) {
  const putItemInput: PutItemInput = {
    TableName: config.evcsStubUserVCsTableName,
    Item: marshall(evcsVcItem, {
      removeUndefinedValues: true,
    }),
  };
  return dynamoClient.putItem(putItemInput);
}

async function updateUserVC(evcsVcItem: EvcsVcItem) {
  if (!evcsVcItem.metadata) {
    evcsVcItem.metadata = {};
  }
  const updateItemInput: UpdateItemInput = {
    TableName: config.evcsStubUserVCsTableName,
    Key: {
      userId: { S: evcsVcItem.userId },
      vcSignature: { S: evcsVcItem.vcSignature },
    },
    UpdateExpression: "set #state = :stateValue, #metadata = :metadataValue",
    ExpressionAttributeNames: {
      "#state": "state",
      "#metadata": "metadata",
    },
    ExpressionAttributeValues: {
      ":stateValue": { S: evcsVcItem.state },
      ":metadataValue": {
        M: marshall(evcsVcItem.metadata, {
          removeUndefinedValues: true,
        }),
      },
    },
    ReturnValues: "ALL_NEW",
  };
  return dynamoClient.updateItem(updateItemInput);
}

async function getTtl(): Promise<number> {
  const evcsTtlSeconds: number = parseInt(
    await getSsmParameter(config.evcsParamBasePath + "evcsStubTtl"),
  );
  return Math.floor(Date.now() / 1000) + evcsTtlSeconds;
}
