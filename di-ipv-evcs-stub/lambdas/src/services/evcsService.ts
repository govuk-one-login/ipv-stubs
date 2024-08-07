import {
  DynamoDB,
  QueryInput,
  PutItemInput,
  UpdateItemInput,
  PutItemCommandOutput,
  UpdateItemCommandOutput,
  AttributeValue,
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
import VCProvenance from "../domain/enums/vcProvenance";

const dynamoClient = config.isLocalDev
  ? new DynamoDB({
      endpoint: config.localDynamoDbEndpoint,
      region: config.region,
    })
  : new DynamoDB({ region: config.region });

export async function processPostUserVCsRequest(
  userId: string,
  postRequest: PostRequest[],
): Promise<ServiceResponse> {
  let response: ServiceResponse = {
    response: {},
  };
  try {
    console.info(`Post user record.`);
    const allPromises: Promise<PutItemCommandOutput>[] = [];
    const ttl = await getTtl();
    for (const postRequestItem of postRequest) {
      const vcItem: EvcsVcItem = {
        userId,
        vc: postRequestItem.vc,
        vcSignature: postRequestItem.vc.split(".")[2],
        state: postRequestItem.state,
        metadata:
          postRequestItem.metadata != undefined ? postRequestItem.metadata : {},
        provenance:
          postRequestItem.provenance != undefined
            ? postRequestItem.provenance
            : VCProvenance.ONLINE,
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
  requestedStates: string[],
): Promise<ServiceResponse> {
  console.info(`Get user record.`);

  const filterExpressions: string[] = [];
  const ExpressionAttributeValues: Record<string, AttributeValue> = {};
  ExpressionAttributeValues[`:userIdValue`] = { S: userId };
  requestedStates.forEach((data, index) => {
    filterExpressions.push(`:state${index}`);
    ExpressionAttributeValues[`:state${index}`] = { S: data };
  });
  const FilterExpression = `#state IN (${filterExpressions})`;

  const getItemInput: QueryInput = {
    TableName: config.evcsStubUserVCsTableName,
    KeyConditionExpression: "#userId = :userIdValue",
    FilterExpression: FilterExpression,
    ExpressionAttributeNames: {
      "#userId": "userId",
      "#state": "state",
    },
    ExpressionAttributeValues: ExpressionAttributeValues,
  };
  const items = (await dynamoClient.query(getItemInput)).Items ?? [];
  console.info(`Total user VCs retrieved - ${items?.length}`);
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
    },
    statusCode: 200,
  };
}

export async function processPatchUserVCsRequest(
  userId: string,
  patchRequest: PatchRequest[],
): Promise<ServiceResponse> {
  try {
    console.info(`Patch user record.`);
    const allPromises: Promise<UpdateItemCommandOutput>[] = [];
    const ttl = await getTtl();
    for (const patchRequestItem of patchRequest) {
      const vcItem: EvcsVcItem = {
        userId,
        vcSignature: patchRequestItem.signature,
        state: patchRequestItem.state,
        metadata: patchRequestItem.metadata,
        ttl,
      };
      allPromises.push(updateUserVC(vcItem));
    }

    console.info(`Updating user VC's.`);
    await Promise.all(allPromises);
    return {
      statusCode: 204,
    };
  } catch (error) {
    console.error(error);
    return {
      response: { message: "Unable to update VCs" },
      statusCode: 500,
    };
  }
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
  const updateItemInput: UpdateItemInput = {
    TableName: config.evcsStubUserVCsTableName,
    Key: {
      userId: { S: evcsVcItem.userId },
      vcSignature: { S: evcsVcItem.vcSignature },
    },
    UpdateExpression: "set #state = :stateValue",
    ExpressionAttributeNames: { "#state": "state" },
    ExpressionAttributeValues: { ":stateValue": marshall(evcsVcItem.state) },
    ReturnValues: "ALL_NEW",
  };

  if (evcsVcItem.metadata) {
    updateItemInput.UpdateExpression += ", #metadata = :metadataValue";
    updateItemInput.ExpressionAttributeNames!["#metadata"] = "metadata";
    updateItemInput.ExpressionAttributeValues![":metadataValue"] = {
      M: marshall(evcsVcItem.metadata, { removeUndefinedValues: true }),
    };
  }

  return dynamoClient.updateItem(updateItemInput);
}

async function getTtl(): Promise<number> {
  const evcsTtlSeconds: number = parseInt(
    await getSsmParameter(config.evcsParamBasePath + "evcsStubTtl"),
  );
  return Math.floor(Date.now() / 1000) + evcsTtlSeconds;
}
