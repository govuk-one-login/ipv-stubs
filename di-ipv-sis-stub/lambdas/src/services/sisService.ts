import { QueryInput } from "@aws-sdk/client-dynamodb";
import { config } from "../config/config";
import { marshall, unmarshall } from "@aws-sdk/util-dynamodb";
import { dynamoClient } from "../clients/dynamodbClient";
import { StoredIdentityJwt, UserIdentity } from "../domain/userIdentity";
import { getVotForUserIdentity } from "../utils/votHelper";
import { decodeJwt } from "jose";
import { createSignedJwt, updateVotOnSiJwt } from "../utils/signedJwtHelper";

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
      siJwt: storedIdentity,
      maxVot: levelOfConfidence,
      isValid,
    };
  });

  const validatedResponse = validateUserIdentityResponse(parsedResponse[0]);

  let signedJwt = validatedResponse.siJwt;
  let matchedProfile = validatedResponse.maxVot;
  if (signedJwt && validatedResponse.maxVot) {
    const decodedSiJwt = decodeJwt<StoredIdentityJwt>(validatedResponse.siJwt);

    matchedProfile = getVotForUserIdentity(
      validatedResponse.maxVot,
      requestedVtrs,
      validatedResponse.isValid,
    );

    const jwt = updateVotOnSiJwt(decodedSiJwt, matchedProfile);
    signedJwt = await createSignedJwt(jwt);
  }

  return {
    content: signedJwt,
    vot: validatedResponse.maxVot,
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
  const requiredProperties = ["maxVot", "siJwt"];

  const missingProperties = requiredProperties.filter(
    (key) => !userIdentityResponse[key],
  );

  if (missingProperties.length != 0) {
    console.info(`Missing required properties: ${missingProperties}`);
  }

  return userIdentityResponse;
};
