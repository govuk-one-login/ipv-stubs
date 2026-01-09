import { dynamoDBClient } from "../clients/dynamoDBClient";
import * as config from "./configService";
import { marshall, unmarshall } from "@aws-sdk/util-dynamodb";
import * as cimitStubItemService from "./cimitStubItemService";
import { CimitStubItem } from "./contraIndicatorTypes";

interface UserMitigationRequest {
  mitigations: string[];
  vcJti: string;
}

interface PendingMitigationItem {
  vcJti: string;
  mitigatedCi: string;
  mitigationCodes: string[];
  requestMethod: string;
  ttl: number;
}

export async function persistPendingMitigation(
  userMitigationRequest: UserMitigationRequest,
  ci: string,
  method: string,
): Promise<void> {
  console.log("Creating pending mitigation", {
    request: userMitigationRequest,
    ci,
    requestMethod: method,
  });

  const item = fromMitigationRequestAndMethod(
    userMitigationRequest,
    ci,
    method,
  );
  await createPendingMitigationItem(item);
}

export async function completePendingMitigation(
  jwtId: string,
  userId: string,
): Promise<void> {
  const pendingMitigationItem = await getPendingMitigationItem(jwtId);

  if (!pendingMitigationItem) {
    console.log("No pending mitigations found", { jwtId, userId });
    return;
  }

  const cimitItems = await cimitStubItemService.getCiForUserId(
    userId,
    pendingMitigationItem.mitigatedCi,
  );

  if (!cimitItems || cimitItems.length === 0) {
    console.warn("No CI found for attempted mitigation", {
      jwtId,
      userId,
      ci: pendingMitigationItem.mitigatedCi,
    });
    return;
  }

  const itemToMitigate = cimitItems
    .sort((a, b) => a.issuanceDate.localeCompare(b.issuanceDate))
    .pop();

  if (!itemToMitigate) {
    throw new Error("No CI found to mitigate");
  }

  switch (pendingMitigationItem.requestMethod) {
    case "PUT":
      itemToMitigate.mitigations = pendingMitigationItem.mitigationCodes;
      break;
    case "POST":
      addMitigations(itemToMitigate, pendingMitigationItem.mitigationCodes);
      break;
    default:
      throw new Error(
        `Method not supported: ${pendingMitigationItem.requestMethod}`,
      );
  }

  await cimitStubItemService.persistCimitStubItem(itemToMitigate);
  console.log("CI mitigated", {
    jwtId,
    userId,
    ci: pendingMitigationItem.mitigatedCi,
  });
}

function fromMitigationRequestAndMethod(
  request: UserMitigationRequest,
  ci: string,
  method: string,
): PendingMitigationItem {
  return {
    vcJti: request.vcJti,
    mitigatedCi: ci,
    mitigationCodes: request.mitigations,
    requestMethod: method,
    ttl: 0,
  };
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

async function createPendingMitigationItem(
  item: PendingMitigationItem,
): Promise<void> {
  const nowInSeconds = Math.floor(Date.now() / 1000);
  const ttlSeconds = await config.getCimitStubTtl();
  item.ttl = nowInSeconds + ttlSeconds;

  await dynamoDBClient.putItem({
    TableName: config.getPendingMitigationsTableName(),
    Item: marshall(item, { removeUndefinedValues: true }),
  });
}

async function getPendingMitigationItem(
  vcJti: string,
): Promise<PendingMitigationItem | null> {
  const result = await dynamoDBClient.getItem({
    TableName: config.getPendingMitigationsTableName(),
    Key: marshall({ vcJti }),
  });

  return result.Item
    ? (unmarshall(result.Item) as PendingMitigationItem)
    : null;
}
