import { isRunningLocally } from "../common/configService";
import { DynamoDB } from "@aws-sdk/client-dynamodb";

const LOCALHOST_URI = "http://localhost:4567";
const REGION = "eu-west-2";

export const dynamoDBClient = isRunningLocally
  ? new DynamoDB({
      endpoint: LOCALHOST_URI,
      region: REGION,
    })
  : new DynamoDB({ region: REGION });
