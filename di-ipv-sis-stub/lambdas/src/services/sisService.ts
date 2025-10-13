import { QueryInput } from "@aws-sdk/client-dynamodb";
import { config } from "../config/config";
import { marshall, unmarshall } from "@aws-sdk/util-dynamodb";
import { dynamoClient } from "../clients/dynamodbClient";
import {
  StoredIdentityContents,
  StoredIdentityJwt,
  UserIdentity,
} from "../domain/userIdentity";
import { getVotForUserIdentity } from "../utils/votHelper";
import { decodeJwt } from "jose";

const GPG45_RECORD_TYPE = "idrec:gpg45";

const VTM = "https://oidc.account.gov.uk/trustmark";

const IDENTITY_CLAIM = "https://vocab.account.gov.uk/v1/coreIdentity";
const ADDRESS_CLAIM = "https://vocab.account.gov.uk/v1/address";
const PASSPORT_CLAIM = "https://vocab.account.gov.uk/v1/passport";
const DRIVING_PERMIT_CLAIM = "https://vocab.account.gov.uk/v1/drivingPermit";
const SOCIAL_SECURITY_RECORD_CLAIM =
  "https://vocab.account.gov.uk/v1/socialSecurityRecord";

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

  const signedJwt = validatedResponse.siJwt;
  let matchedProfile = validatedResponse.maxVot;
  let siContent: StoredIdentityContents | undefined = undefined;
  if (signedJwt && validatedResponse.maxVot) {
    const decodedSiJwt = decodeJwt<StoredIdentityJwt>(signedJwt);

    matchedProfile = getVotForUserIdentity(
      validatedResponse.maxVot,
      requestedVtrs,
      validatedResponse.isValid,
    );

    siContent = {
      sub: decodedSiJwt.sub,
      vot: matchedProfile,
      // We don't check this value so it can be anything
      vtm: VTM,
      "https://vocab.account.gov.uk/v1/credentialJWT": decodedSiJwt.credentials,
      [IDENTITY_CLAIM]: decodedSiJwt.claims?.[IDENTITY_CLAIM] || undefined,
      [ADDRESS_CLAIM]: decodedSiJwt.claims?.[ADDRESS_CLAIM] || undefined,
      [PASSPORT_CLAIM]: decodedSiJwt.claims?.[PASSPORT_CLAIM] || undefined,
      [DRIVING_PERMIT_CLAIM]:
        decodedSiJwt.claims?.[DRIVING_PERMIT_CLAIM] || undefined,
      [SOCIAL_SECURITY_RECORD_CLAIM]:
        decodedSiJwt.claims?.[SOCIAL_SECURITY_RECORD_CLAIM] || undefined,
    };
  }

  return {
    content: siContent,
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
