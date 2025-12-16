import { isRunningLocally} from "../common/configService";
import { DynamoDB } from "@aws-sdk/client-dynamodb";

export const dynamoDBClient= config.isLocalDev
  ? new DynamoDB({
      endpoint: config.localDynamoDbEndpoint,
      region: config.region,
    })
  : new DynamoDB({ region: config.region });
