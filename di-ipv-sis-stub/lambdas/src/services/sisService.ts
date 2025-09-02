import { QueryInput } from "@aws-sdk/client-dynamodb";
import { config } from "../config/config";
import { marshall, unmarshall } from "@aws-sdk/util-dynamodb";
import { dynamoClient } from "../clients/dynamodbClient";
import { UserIdentity } from "../domain/userIdentity";
import { getVotForUserIdentity } from "../utils/votHelper";

const GPG45_RECORD_TYPE = "idrec:gpg45";

export const getUserIdentity = async (
  userId: string,
  requestedVtrs: string[],
): Promise<UserIdentity | null> => {
  console.info("Getting user's SIS record");

  const getItemInput: QueryInput = {
    TableName: config.evcsStoredIdentityObjectTableName,
    KeyConditionExpression:
      "userId = :userIdValue AND recordType = :recordTypeValue",
    ExpressionAttributeValues: {
      ":userIdValue": marshall(userId),
      ":recordTypeValue": marshall(GPG45_RECORD_TYPE),
    },
  };

  const response = await dynamoClient.query(getItemInput);

  if (!response.Items || response.Items.length === 0) {
    return null;
  }

  const parsedResponse = response.Items.map((siItem) => {
    const { storedIdentity, levelOfConfidence, isValid } = unmarshall(siItem);

    return {
      content: storedIdentity,
      vot: levelOfConfidence,
      isValid,
    };
  });

  const validatedResponse = validateUserIdentityResponse(parsedResponse[0]);
  const matchedProfile = validatedResponse.vot
    ? getVotForUserIdentity(
        validatedResponse.vot,
        requestedVtrs,
        validatedResponse.isValid,
      )
    : undefined;

  return {
    ...validatedResponse,
    vot: matchedProfile,
    isValid: !!(
      validatedResponse.isValid &&
      matchedProfile &&
      matchedProfile != "P0"
    ),
    // defaulting to false as the ttl is set to the default
    // retention of VCs which is 120 years
    expired: false,
    // defaulting to true for below as we don't use these
    kidValid: true,
    signatureValid: true,
  };
};

/* eslint-disable  @typescript-eslint/no-explicit-any */
const validateUserIdentityResponse = (userIdentityResponse: any) => {
  const requiredProperties = ["vot", "content"];

  const missingProperties = requiredProperties.filter(
    (key) => !userIdentityResponse[key],
  );

  if (missingProperties.length != 0) {
    console.info(`Missing required properties: ${missingProperties}`);
  }

  return userIdentityResponse;
};
