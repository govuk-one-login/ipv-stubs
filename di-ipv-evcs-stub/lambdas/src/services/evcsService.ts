import { DynamoDB, PutItemInput } from "@aws-sdk/client-dynamodb";
import { marshall } from "@aws-sdk/util-dynamodb";

import PostRequest from "../domain/postRequest";
import EvcsVcItem from "../model/evcsVcItem";

import { config } from "../common/config";
import { getSsmParameter } from "../common/ssmParameter";

const dynamoClient = config.isLocalDev
  ? new DynamoDB({
      endpoint: config.localDynamoDbEndpoint,
      region: config.region,
    })
  : new DynamoDB({ region: config.region });

export async function processPostUserVCsRequest(
    userId: string,
    postRequest: PostRequest
  ): Promise<void> {

    for (const persistVC of postRequest.persistVCs) {
        const vcItem: EvcsVcItem = {
            userId: userId,
            vc: persistVC.vc,
            state: persistVC.state,
            metadata: persistVC.metadata!,
            provenience: persistVC.provenience!,
            ttl: await getTtl(),
          };
          await saveUserVC(vcItem);
    }
  }

  async function saveUserVC(evcsVcItem: EvcsVcItem) {
    console.info(`Save user vc.- ${JSON.stringify(config.evcsStubUserVCsTableName)}`);
    const putItemInput: PutItemInput = {
      TableName: config.evcsStubUserVCsTableName,
      Item: marshall(evcsVcItem,  {
          removeUndefinedValues: true
        }
      ),
    };
    await dynamoClient.putItem(putItemInput);
  }

  async function getTtl(): Promise<number> {
    const evcsTtlSeconds: number = parseInt(await getSsmParameter(config.evcsParamBasePath + "evcsStubTtl"))
    return Math.floor(Date.now() / 1000) + evcsTtlSeconds;
  }