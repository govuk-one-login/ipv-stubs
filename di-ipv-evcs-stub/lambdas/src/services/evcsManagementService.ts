import { QueryInput } from "@aws-sdk/client-dynamodb";
import { config } from "../common/config";
import { marshall, unmarshall } from "@aws-sdk/util-dynamodb";
import { GetStoredIdentity } from "../domain/serviceResponse";
import { dynamoClient } from "../clients/dynamodbClient";
import { StoredIdentityDetails } from "../domain/requests";
import { createPutItem } from "./evcsService";
import EvcsStoredIdentityItem from "../model/storedIdentityItem";
import { StatusCodes } from "../domain/enums";
import { StoredIdentityRecordType } from "../domain/enums/StoredIdentityRecordType";

export interface CreateStoredIdentityRequest {
  userId: string;
  si: StoredIdentityDetails;
}

export async function processGetStoredIdentity(
  userId: string,
): Promise<GetStoredIdentity> {
  console.info("Getting user's stored identity record");

  const getItemInput: QueryInput = {
    TableName: config.evcsStoredIdentityObjectTableName,
    KeyConditionExpression: "userId = :userIdValue",
    ExpressionAttributeValues: {
      ":userIdValue": marshall(userId),
    },
  };

  const response = await dynamoClient.query(getItemInput);

  if (!response.Items || response.Items.length === 0) {
    return {
      storedIdentities: [],
    };
  }

  const parsedResponse = response.Items.map((siItem) => {
    const {
      recordType,
      storedIdentity,
      levelOfConfidence,
      isValid,
      expired = false,
    } = unmarshall(siItem);
    return {
      userId,
      recordType,
      storedIdentity,
      levelOfConfidence,
      isValid,
      expired,
    };
  });

  return {
    storedIdentities: parsedResponse,
  };
}

export async function processCreateStoredIdentity(
  createSiRequest: CreateStoredIdentityRequest,
) {
  try {
    const evcsStoredIdentityItem: EvcsStoredIdentityItem = {
      userId: createSiRequest.userId,
      recordType: StoredIdentityRecordType.GPG45,
      storedIdentity: createSiRequest.si.jwt,
      levelOfConfidence: createSiRequest.si.vot,
      metadata: createSiRequest.si.metadata,
      isValid: true,
      expired: createSiRequest.si.expired || false,
    };
    const putItem = createPutItem(evcsStoredIdentityItem);

    await dynamoClient.putItem(putItem);

    return {
      response: { result: "success" },
      statusCode: StatusCodes.Accepted,
    };
  } catch (error) {
    console.error("Failed to create SI for user: ", error);
    return {
      response: { result: "fail" },
      statusCode: StatusCodes.InternalServerError,
    };
  }
}
