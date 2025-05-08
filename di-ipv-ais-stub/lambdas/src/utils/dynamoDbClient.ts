import { config } from "./config";
import { DynamoDB } from "@aws-sdk/client-dynamodb";

const dynamoClient = config.isLocalDev
  ? new DynamoDB({
      endpoint: config.localDynamoDbEndpoint,
      region: config.region,
    })
  : new DynamoDB({ region: config.region });

export default dynamoClient;
