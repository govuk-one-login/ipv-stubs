import { dynamoDBClient } from "../clients/dynamoDBClient";
import * as config from "./configService";
import { marshall, unmarshall } from "@aws-sdk/util-dynamodb";
import { CimitStubItem } from "./contraIndicatorTypes";

export interface UserPreMitigationRequest {
  mitigations: string[];
}

interface PreMitigationItem {
  userId: string;
  mitigatedCi: string;
  mitigationCodes: string[];
  ttl: number;
}

export async function persistPreMitigation(
  userId: string,
  ci: string,
  userMitigationRequest: UserPreMitigationRequest,
): Promise<void> {
  console.info("Creating pending mitigation", {
    request: userMitigationRequest,
    ci,
  });

  const item = await fromMitigationRequest(userId, ci, userMitigationRequest);
  await persistPendingMitigationItem(item);
}

async function fromMitigationRequest(
  userId: string,
  ci: string,
  request: UserPreMitigationRequest,
): Promise<PreMitigationItem> {
  const nowInSeconds = Math.floor(Date.now() / 1000);
  const ttlSeconds = await config.getCimitStubTtl();
  const ttl = nowInSeconds + ttlSeconds;

  return {
    userId,
    mitigatedCi: ci,
    mitigationCodes: request.mitigations,
    ttl,
  };
}

async function persistPendingMitigationItem(
  item: PreMitigationItem,
): Promise<void> {
  await dynamoDBClient.putItem({
    TableName: config.getPreMitigationsTableName(),
    Item: marshall(item, { removeUndefinedValues: true }),
  });
}

export async function applyPreMitigationsToItems(
  userId: string,
  cimitItems: CimitStubItem[],
): Promise<void> {
  const preMitigationItems = await getAllPreMitigationsForUser(userId);

  for (const cimitItem of cimitItems) {
    const preMitigationItem = preMitigationItems.find(
      (item) => item.mitigatedCi === cimitItem.contraIndicatorCode,
    );

    if (preMitigationItem) {
      addMitigations(cimitItem, preMitigationItem.mitigationCodes);
      console.info("Pre-mitigation applied to new CI", {
        userId,
        ci: cimitItem.contraIndicatorCode,
        mitigations: preMitigationItem.mitigationCodes,
      });
    }
  }
}

function addMitigations(item: CimitStubItem, newMitigations: string[]): void {
  if (!item.mitigations) {
    item.mitigations = newMitigations;
    return;
  }
  if (!newMitigations) {
    return;
  }
  // Don't add duplicate mitigations
  item.mitigations = [
    ...new Set([...item.mitigations, ...newMitigations]),
  ].sort();
}

async function getAllPreMitigationsForUser(
  userId: string,
): Promise<PreMitigationItem[]> {
  const result = await dynamoDBClient.query({
    TableName: config.getPreMitigationsTableName(),
    KeyConditionExpression: "userId = :userId",
    ExpressionAttributeValues: marshall({ ":userId": userId }),
  });

  return (result.Items || []).map(
    (item) => unmarshall(item) as PreMitigationItem,
  );
}
