import { config } from "../config/config";
import { DynamoDB } from "@aws-sdk/client-dynamodb";

export const dynamoClient = config.isLocalDev
  ? new DynamoDB({
      endpoint: config.localDynamoDbEndpoint,
      region: config.region,
    })
  : new DynamoDB({ region: config.region });
