import { marshall, unmarshall } from "@aws-sdk/util-dynamodb";
import { config, getSsmParameter } from "./config";
import { Response, UserManagementRequest } from "./types";
import dynamoClient from "./dynamoDbClient";
import cases from "../cases";

// Store the AIS response we want to return for a user.
export default async function persistFutureAisResponse(
  userId: string,
  userManagementRequest: UserManagementRequest,
): Promise<void> {
  const { statusCode, responseDelay, intervention } = userManagementRequest;

  console.info("Build response.");
  const response: Response = {
    userId: userId,
    statusCode: statusCode || 200,
    ttl: await getTtl(),
    responseDelay: responseDelay || 0,
    responseBody: cases[intervention],
  };

  console.info("Store response.");
  await dynamoClient.putItem({
    TableName: config.aisStubUserEvidenceTableName,
    Item: marshall(response, { removeUndefinedValues: true }),
  });
}

// Get the response stored for that user by use of the management endpoint.
export async function getAisResponse(userId: string): Promise<Response | null> {
  console.info("Get response.");
  const { Item } = await dynamoClient.getItem({
    TableName: config.aisStubUserEvidenceTableName,
    Key: marshall({
      userId: userId,
    }),
  });

  if (!Item) {
    console.info("Response not found for user.");
    return null;
  }

  return unmarshall(Item) as Response;
}

async function getTtl(): Promise<number> {
  const ttlSeconds: number = parseInt(
    await getSsmParameter(config.aisParamBasePath + "aisStubTtl"),
  );
  return Math.floor(Date.now() / 1000) + ttlSeconds;
}
