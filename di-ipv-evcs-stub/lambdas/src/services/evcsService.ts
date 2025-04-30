import {
  AttributeValue,
  DynamoDB,
  PutItemCommandOutput,
  QueryInput,
  TransactWriteItem,
  TransactWriteItemsCommand,
  Update,
  UpdateItemCommandOutput,
  UpdateItemInput,
} from "@aws-sdk/client-dynamodb";
import { marshall, unmarshall } from "@aws-sdk/util-dynamodb";

import PostRequest from "../domain/postRequest";
import ServiceResponse, { GetResponse } from "../domain/serviceResponse";
import { VcState } from "../domain/enums/vcState";
import EvcsVcItem from "../model/evcsVcItem";

import { config } from "../common/config";
import { getSsmParameter } from "../common/ssmParameter";
import { v4 as uuid } from "uuid";
import PatchRequest from "../domain/patchRequest";
import VCProvenance from "../domain/enums/vcProvenance";
import PutRequest from "../domain/putRequest";
import { VcDetails } from "../domain/sharedTypes";
import { StatusCodes } from "../domain/enums/statusCodes";
import EvcsStoredIdentityItem from "../model/storedIdentityItem";

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
        vcSignature: getSignatureFromJwt(postRequestItem.vc),
        state: postRequestItem.state,
        metadata:
          postRequestItem.metadata != undefined ? postRequestItem.metadata : {},
        provenance:
          postRequestItem.provenance != undefined
            ? postRequestItem.provenance
            : VCProvenance.ONLINE,
        ttl,
      };
      allPromises.push(saveUserEvcsItem(vcItem));
    }

    console.info(`Saving all user VC's.`);
    await Promise.all(allPromises);
    response = {
      response: {
        messageId: uuid(),
      },
      statusCode: StatusCodes.Accepted,
    };
  } catch (error) {
    console.error(error);
    response = {
      response: {
        messageId: "",
      },
      statusCode: StatusCodes.InternalServerError,
    };
  }
  return response;
}

export async function processPutUserVCsRequest(
  putRequest: PutRequest,
): Promise<ServiceResponse> {
  console.info("Put user record");
  const userId = putRequest.userId;
  const newUserVcs = putRequest.vcs;

  try {
    const ttl = await getTtl();
    // Read user's existing VCs
    const getResponse = await processGetUserVCsRequest(userId, [
      VcState.CURRENT,
      VcState.PENDING_RETURN,
    ]);

    if (
      getResponse.statusCode !== StatusCodes.Success ||
      !getResponse.response ||
      (!!getResponse.response && !("vcs" in getResponse.response))
    ) {
      console.error("Failed to read existing user VCs from EVCS");
      return {
        response: { messageId: "" },
        statusCode: getResponse.statusCode,
      };
    }

    // Update existing user VCs in EVCS with new states
    const existingUserVcs = getResponse.response.vcs;
    const newVcSignatures = newUserVcs.map(({ vc }) => getSignatureFromJwt(vc));
    const updateExistingVcsRequests = existingUserVcs.map(
      ({ vc, state: currentState, metadata }) => {
        const vcSignature = getSignatureFromJwt(vc);

        const mappedVcToUpdateItem = createUpdateItemInput({
          vcSignature,
          state: getUpdatedState(vcSignature, newVcSignatures, currentState),
          metadata,
          userId,
          ttl,
        }) as Update;

        return { Update: mappedVcToUpdateItem };
      },
    );

    // Write new VCs with new state using PUT
    const putNewUserVsRequests = newUserVcs.map(
      ({ vc, state, metadata, provenance }: VcDetails) => {
        const vcItemForStorage: EvcsVcItem = {
          userId,
          vcSignature: getSignatureFromJwt(vc),
          vc,
          state,
          provenance: provenance || VCProvenance.ONLINE,
          metadata,
          ttl,
        };
        return { Put: createPutItem(vcItemForStorage) };
      },
    );

    const transactItems: TransactWriteItem[] = [
      ...updateExistingVcsRequests,
      ...putNewUserVsRequests,
    ];

    // Save stored identity object if present
    if (putRequest.si) {
      const storedIdentityItem: EvcsStoredIdentityItem = {
        userId: putRequest.userId,
        jwtSignature: getSignatureFromJwt(putRequest.si.jwt),
        storedIdentity: putRequest.si.jwt,
        levelOfConfidence: putRequest.si.vot,
        metadata: putRequest.si.metadata,
        isValid: true,
      };
      transactItems.push({ Put: createPutItem(storedIdentityItem) });
    }

    const transactionCommand = new TransactWriteItemsCommand({
      TransactItems: transactItems,
    });
    await dynamoClient.send(transactionCommand);

    return {
      response: { messageId: uuid() },
      statusCode: StatusCodes.Accepted,
    };
  } catch (error) {
    return {
      response: { messageId: "" },
      statusCode: StatusCodes.InternalServerError,
    };
  }
}

export async function processGetUserVCsRequest(
  userId: string,
  requestedStates: string[],
): Promise<GetResponse> {
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
    statusCode: StatusCodes.Success,
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
      const vcItem: Omit<EvcsVcItem, "vc"> = {
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
      statusCode: StatusCodes.NoContent,
    };
  } catch (error) {
    console.error(error);
    return {
      response: { message: "Unable to update VCs" },
      statusCode: StatusCodes.InternalServerError,
    };
  }
}

function createPutItem(evcsItem: EvcsVcItem | EvcsStoredIdentityItem) {
  return {
    TableName: isEvcsVcItem(evcsItem)
      ? config.evcsStubUserVCsTableName
      : config.evcsStoredIdentityObjectTableName,
    Item: marshall(evcsItem, {
      removeUndefinedValues: true,
    }),
  };
}

async function saveUserEvcsItem(
  evcsItem: EvcsVcItem | EvcsStoredIdentityItem,
): Promise<PutItemCommandOutput> {
  const putItemInput = createPutItem(evcsItem);
  return dynamoClient.putItem(putItemInput);
}

function createUpdateItemInput(evcsVcItem: Omit<EvcsVcItem, "vc">) {
  return {
    TableName: config.evcsStubUserVCsTableName,
    Key: {
      userId: { S: evcsVcItem.userId },
      vcSignature: { S: evcsVcItem.vcSignature },
    },
    UpdateExpression: `set #state = :stateValue${evcsVcItem.metadata ? ", #metadata = :metadataValue" : ""}`,
    ExpressionAttributeNames: {
      "#state": "state",
      ...(evcsVcItem.metadata ? { "#metadata": "metadata" } : {}),
    },
    ExpressionAttributeValues: {
      ":stateValue": marshall(evcsVcItem.state),
      ...(evcsVcItem.metadata
        ? {
            ":metadata": {
              M: marshall(evcsVcItem.metadata, { removeUndefinedValues: true }),
            },
          }
        : {}),
    },
  };
}

async function updateUserVC(evcsVcItem: Omit<EvcsVcItem, "vc">) {
  const updateItemInput: UpdateItemInput = createUpdateItemInput(evcsVcItem);
  updateItemInput.ReturnValues = "ALL_NEW";
  return dynamoClient.updateItem(updateItemInput);
}

async function getTtl(): Promise<number> {
  const evcsTtlSeconds: number = parseInt(
    await getSsmParameter(config.evcsParamBasePath + "evcsStubTtl"),
  );
  return Math.floor(Date.now() / 1000) + evcsTtlSeconds;
}

function getSignatureFromJwt(jwt: string): string {
  return jwt.split(".")[2];
}

function isEvcsVcItem(
  evcsSaveItem: EvcsVcItem | EvcsStoredIdentityItem,
): evcsSaveItem is EvcsVcItem {
  return (evcsSaveItem as EvcsVcItem).vcSignature !== undefined;
}

function getUpdatedState(
  currentVcSignature: string,
  newVcSignatures: string[],
  currentVcState: VcState,
): VcState {
  if (!newVcSignatures.includes(currentVcSignature)) {
    switch (currentVcState) {
      case VcState.CURRENT:
        return VcState.HISTORIC;
      case VcState.PENDING_RETURN:
        return VcState.ABANDONED;
      default:
        return currentVcState;
    }
  }
  return currentVcState;
}
