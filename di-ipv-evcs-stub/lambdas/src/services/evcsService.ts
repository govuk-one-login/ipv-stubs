import {
  AttributeValue,
  PutItemCommandOutput,
  QueryInput,
  TransactWriteItem,
  Update,
  UpdateItemCommandOutput,
  UpdateItemInput,
} from "@aws-sdk/client-dynamodb";
import { marshall, unmarshall } from "@aws-sdk/util-dynamodb";

import ServiceResponse, {
  createServiceResponseWithMessageId,
  createServiceResponse,
  createServiceResponseWithMessage,
  GetResponse,
} from "../domain/serviceResponse";
import {
  CreateVcStates,
  stateTransitions,
  StatusCodes,
  VCProvenance,
  VcState,
} from "../domain/enums";
import EvcsVcItem from "../model/evcsVcItem";

import { config } from "../common/config";
import { v4 as uuid } from "uuid";
import {
  EvcsItemForUpdate,
  PatchRequest,
  PostIdentityRequest,
  PostRequest,
} from "../domain/requests";
import { VcDetails } from "../domain/vcDetails";
import { StoredIdentityResponse } from "../domain/sharedTypes";
import EvcsStoredIdentityItem from "../model/storedIdentityItem";
import { StoredIdentityRecordType } from "../domain/enums/StoredIdentityRecordType";
import { dynamoClient } from "../clients/dynamodbClient";
import { PostVcsRequest } from "../domain/requests/postVcsRequest";
import { PatchVcsRequest } from "../domain/requests/patchVcsRequest";
import { getErrorMessage, getSignatureFromJwt } from "../common/utils";

export async function processPostUserVCsRequest(
  userId: string,
  postRequest: PostRequest[],
): Promise<ServiceResponse> {
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
    return createServiceResponseWithMessageId(StatusCodes.Accepted, uuid());
  } catch (error) {
    console.error(error);
    return createServiceResponse(StatusCodes.InternalServerError);
  }
}

export async function processPostUserVCsRequestV2(
  postRequest: PostVcsRequest,
): Promise<ServiceResponse> {
  try {
    console.info(`Process and save VCs`);

    // Check posted VC states
    if (postRequest.vcs.some((vc) => !(vc.state in CreateVcStates))) {
      return createServiceResponseWithMessage(
        StatusCodes.Conflict,
        "At least one VC is in the wrong state",
      );
    }

    // Check that none of the posted VCs already exist
    const vcRecords = await getCurrentVcsForUser(postRequest.userId);
    const requestVcs = postRequest.vcs.map((vcDetails) => vcDetails.vc);

    if (
      vcRecords.some((vcRecord) => requestVcs.includes(vcRecord.vc.S || ""))
    ) {
      return createServiceResponseWithMessage(
        StatusCodes.Conflict,
        "At least one VC already exists in EVCS",
      );
    }

    const allPromises: Promise<PutItemCommandOutput>[] = [];
    const ttl = await getTtl();
    for (const vcDetails of postRequest.vcs) {
      const vcItem: EvcsVcItem = {
        userId: postRequest.userId,
        vc: vcDetails.vc,
        vcSignature: getSignatureFromJwt(vcDetails.vc),
        state: vcDetails.state,
        metadata: vcDetails.metadata != undefined ? vcDetails.metadata : {},
        provenance:
          vcDetails.provenance != undefined
            ? vcDetails.provenance
            : VCProvenance.ONLINE,
        ttl,
      };
      allPromises.push(saveUserEvcsItem(vcItem));
    }

    console.info(`Saving all user VC's.`);
    await Promise.all(allPromises);
    return createServiceResponseWithMessageId(StatusCodes.Accepted, uuid());
  } catch (error) {
    console.error(error);
    return createServiceResponse(StatusCodes.InternalServerError);
  }
}

export async function processPatchUserVCsRequestV2(
  patchRequest: PatchVcsRequest,
): Promise<ServiceResponse> {
  try {
    console.info(`Patch user record.`);

    // Check that the request VCs already exist
    const allVcStates: VcState[] = Object.values(VcState);
    const getResponse = await processGetUserVCsRequest(
      patchRequest.userId,
      allVcStates,
    );
    if (
      getResponse.statusCode !== StatusCodes.Success ||
      !getResponse.response ||
      (!!getResponse.response && !("vcs" in getResponse.response))
    ) {
      console.error(
        `Failed to read existing user VCs from EVCS with ${getResponse.statusCode}`,
      );
      return createServiceResponse(StatusCodes.InternalServerError);
    }

    const existingVcs = getResponse.response.vcs;

    const requestVcSignatureToState = new Map(
      patchRequest.vcs.map((vcItem) => [vcItem.signature, vcItem.state]),
    );
    const existingVcSignatureToState = new Map(
      existingVcs.map((vcItem) => [
        getSignatureFromJwt(vcItem.vc),
        vcItem.state,
      ]),
    );

    for (const requestSignature of requestVcSignatureToState.keys()) {
      const existingState = existingVcSignatureToState.get(requestSignature);
      if (!existingState) {
        return createServiceResponseWithMessage(
          StatusCodes.NotFound,
          "At least one request VC doesn't exist in EVCS",
        );
      }
    }

    // Check that the state transitions are allowed
    try {
      validateStateTransitions(
        existingVcSignatureToState,
        requestVcSignatureToState,
      );
    } catch (error) {
      return {
        response: { messageId: getErrorMessage(error) },
        statusCode: StatusCodes.Conflict,
      };
    }

    const allPromises: Promise<UpdateItemCommandOutput>[] = [];
    const ttl = await getTtl();
    for (const patchRequestItem of patchRequest.vcs) {
      const vcItem: EvcsItemForUpdate = {
        userId: patchRequest.userId,
        vcSignature: patchRequestItem.signature,
        state: patchRequestItem.state,
        metadata: patchRequestItem.metadata,
        ttl,
      };
      allPromises.push(updateUserVC(vcItem));
    }

    console.info(`Updating user VC's.`);
    await Promise.all(allPromises);
    return createServiceResponse(StatusCodes.NoContent);
  } catch (error) {
    console.error(error);
    return createServiceResponseWithMessage(
      StatusCodes.InternalServerError,
      "Unable to update VCs",
    );
  }
}

export async function processPostIdentityRequest(
  request: PostIdentityRequest,
): Promise<ServiceResponse> {
  console.info("Put user record");
  const userId = request.userId;

  try {
    const transactItems: TransactWriteItem[] = [];

    if (request.vcs) {
      // Read user's existing VCs
      const allVcStates: VcState[] = Object.values(VcState);
      const getResponse = await processGetUserVCsRequest(userId, allVcStates);

      if (
        getResponse.statusCode !== StatusCodes.Success ||
        !getResponse.response ||
        (!!getResponse.response && !("vcs" in getResponse.response))
      ) {
        console.error(
          `Failed to read existing user VCs from EVCS with ${getResponse.statusCode}`,
        );
        return createServiceResponse(StatusCodes.InternalServerError);
      }

      const existingVcs = getResponse.response.vcs;
      const existingVcsSignatures = existingVcs.map(({ vc }) =>
        getSignatureFromJwt(vc),
      );
      const newVcs = request.vcs;

      const vcsToUpdate = newVcs.filter(({ vc }) =>
        existingVcsSignatures.includes(getSignatureFromJwt(vc)),
      );

      try {
        const updatableVcsSignatureToState = new Map(
          vcsToUpdate.map((vc) => [getSignatureFromJwt(vc.vc), vc.state]),
        );
        const existingVcsSignatureToState = new Map<string, VcState>();
        for (const vc of existingVcs) {
          const sig = getSignatureFromJwt(vc.vc);
          if (updatableVcsSignatureToState.has(sig)) {
            existingVcsSignatureToState.set(sig, vc.state);
          }
        }
        validateStateTransitions(
          existingVcsSignatureToState,
          updatableVcsSignatureToState,
        );
      } catch (error) {
        return {
          response: { messageId: getErrorMessage(error) },
          statusCode: StatusCodes.Conflict,
        };
      }

      const ttl = await getTtl();
      const updateExistingVcsRequests = vcsToUpdate.map((vc) => {
        const vcSignature = getSignatureFromJwt(vc.vc);
        const mappedVcToUpdateItem = createUpdateItemInput({
          userId: userId,
          vcSignature: vcSignature,
          state: vc.state,
          metadata: vc.metadata,
          provenance: vc.provenance,
          ttl: ttl,
        }) as Update;

        return { Update: mappedVcToUpdateItem };
      });

      const vcsToAdd = newVcs.filter(
        ({ vc }) => !existingVcsSignatures.includes(getSignatureFromJwt(vc)),
      );
      const putNewUserVsRequests = vcsToAdd.map(
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

      transactItems.push(...updateExistingVcsRequests, ...putNewUserVsRequests);
    }

    // Save stored identity object if present
    const storedIdentityItem: EvcsStoredIdentityItem = {
      userId: request.userId,
      recordType: StoredIdentityRecordType.GPG45,
      storedIdentity: request.si.jwt,
      levelOfConfidence: request.si.vot,
      metadata: request.si.metadata,
      isValid: true,
      expired: false,
    };
    transactItems.push({ Put: createPutItem(storedIdentityItem) });

    await dynamoClient.transactWriteItems({
      TransactItems: transactItems,
    });

    return createServiceResponseWithMessageId(StatusCodes.Accepted, uuid());
  } catch (error) {
    console.error("Failed to complete transaction.", error);
    return createServiceResponse(StatusCodes.InternalServerError);
  }
}

function validateStateTransitions(
  existingVcsSignatureToState: Map<string, VcState>,
  newVcsSignatureToState: Map<string, VcState>,
) {
  for (const [
    existingSignature,
    existingState,
  ] of existingVcsSignatureToState) {
    const newState = newVcsSignatureToState.get(existingSignature);
    if (!newState) {
      throw new Error(
        "No matching signature while validating state transitions.",
      );
    }

    const allowedTransitions = stateTransitions[newState];
    const hasRules = allowedTransitions.length > 0;
    const isAllowed = allowedTransitions.includes(existingState);

    if (hasRules && !isAllowed) {
      throw new Error(
        `State VC transition from: ${existingState} to ${newState} is not allowed`,
      );
    }
  }
}

export async function processGetIdentityRequest(
  userId: string,
): Promise<ServiceResponse> {
  console.log(userId, config.evcsStoredIdentityObjectTableName);
  const getItemResponse = await dynamoClient.getItem({
    TableName: config.evcsStoredIdentityObjectTableName,
    Key: {
      userId: {
        S: userId,
      },
      recordType: {
        S: StoredIdentityRecordType.GPG45,
      },
    },
  });

  if (
    !getItemResponse.Item?.storedIdentity?.S ||
    !getItemResponse.Item?.isValid?.BOOL
  ) {
    return createServiceResponseWithMessage(StatusCodes.NotFound, "Not found");
  }

  const vcRecords = await getCurrentVcsForUser(userId);

  const response: StoredIdentityResponse = {
    si: {
      vc: getItemResponse.Item.storedIdentity.S,
      unsignedVot: getItemResponse.Item.levelOfConfidence.S,
    },
    vcs:
      vcRecords.map((vc) => ({
        vc: vc.vc.S || "",
        state: vc.state.S,
        metadata: vc.metadata?.M ? unmarshall(vc.metadata.M) : undefined,
      })) || [],
  };

  return createServiceResponse(StatusCodes.Success, response);
}

export async function invalidateUserSi(
  userId: string,
): Promise<ServiceResponse> {
  try {
    const queryInput: QueryInput = {
      TableName: config.evcsStoredIdentityObjectTableName,
      KeyConditionExpression: "#userId = :userId",
      ExpressionAttributeNames: {
        "#userId": "userId",
      },
      ExpressionAttributeValues: {
        ":userId": marshall(userId),
      },
    };

    const storedIdentities = (await dynamoClient.query(queryInput)).Items ?? [];

    if (storedIdentities.length === 0) {
      console.info("No stored identity found for user");
      return {
        statusCode: StatusCodes.NotFound,
      };
    }

    const updateTransactItems: TransactWriteItem[] = storedIdentities
      .map((si) => unmarshall(si) as EvcsStoredIdentityItem)
      .map((parsedSi) => ({
        Update: {
          TableName: config.evcsStoredIdentityObjectTableName,
          Key: {
            userId: marshall(userId),
            recordType: marshall(parsedSi.recordType),
          },
          UpdateExpression: `set #isValid = :isValid`,
          ExpressionAttributeNames: {
            "#isValid": "isValid",
          },
          ExpressionAttributeValues: {
            ":isValid": marshall(false),
          },
        },
      }));

    await dynamoClient.transactWriteItems({
      TransactItems: updateTransactItems,
    });

    return createServiceResponse(StatusCodes.NoContent);
  } catch (error) {
    console.error(
      "Transaction failed. Failed to invalidate stored identity",
      error,
    );
    return createServiceResponse(StatusCodes.InternalServerError);
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
      const vcItem: EvcsItemForUpdate = {
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
    return createServiceResponse(StatusCodes.NoContent);
  } catch (error) {
    console.error(error);
    return createServiceResponseWithMessage(
      StatusCodes.InternalServerError,
      "Unable to update VCs",
    );
  }
}

export function createPutItem(evcsItem: EvcsVcItem | EvcsStoredIdentityItem) {
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

async function getCurrentVcsForUser(
  userId: string,
): Promise<Record<string, AttributeValue>[]> {
  const value = await dynamoClient.query({
    TableName: config.evcsStubUserVCsTableName,
    KeyConditionExpression: "#userId = :userId",
    FilterExpression: "#state IN (:state0)",
    ExpressionAttributeNames: {
      "#userId": "userId",
      "#state": "state",
    },
    ExpressionAttributeValues: {
      ":userId": {
        S: userId,
      },
      ":state0": {
        S: "CURRENT",
      },
    },
  });

  return value.Items || [];
}

export function createUpdateItemInput(evcsVcItem: EvcsItemForUpdate) {
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
            ":metadataValue": {
              M: marshall(evcsVcItem.metadata, { removeUndefinedValues: true }),
            },
          }
        : {}),
    },
  };
}

async function updateUserVC(evcsVcItem: EvcsItemForUpdate) {
  const updateItemInput: UpdateItemInput = createUpdateItemInput(evcsVcItem);
  updateItemInput.ReturnValues = "ALL_NEW";
  return dynamoClient.updateItem(updateItemInput);
}

async function getTtl(): Promise<number> {
  const evcsTtlSeconds: number = parseInt(
    process.env.EVCS_STUB_TTL ?? "604800",
  );
  return Math.floor(Date.now() / 1000) + evcsTtlSeconds;
}

function isEvcsVcItem(
  evcsSaveItem: EvcsVcItem | EvcsStoredIdentityItem,
): evcsSaveItem is EvcsVcItem {
  return (evcsSaveItem as EvcsVcItem).vcSignature !== undefined;
}
