import { CimitStubItem } from "../../../common/contraIndicatorTypes";
import * as cimitStubItemService from "../../../common/cimitStubItemService";

export interface UserCisRequest {
  code: string;
  issuanceDate: string;
  issuer: string;
  mitigations: string[];
  document: string;
  txn: string;
}

export async function addUserCis(
  userId: string,
  userCisRequests: UserCisRequest[],
): Promise<void> {
  for (const ciRequest of userCisRequests) {
    const cimitStubItem = cimitStubItemService.fromUserCisRequest(
      ciRequest,
      userId,
    );
    await cimitStubItemService.persistCimitStubItem(cimitStubItem);
  }

  console.log("Inserted User CI data to the Cimit Stub DynamoDB Table.");
}

export async function updateUserCis(
  userId: string,
  userCisRequests: UserCisRequest[],
): Promise<void> {
  const cimitStubItems = await cimitStubItemService.getCIsForUserId(userId);

  if (cimitStubItems.length > 0) {
    await deleteCimitStubItems(cimitStubItems);
  }

  for (const ciRequest of userCisRequests) {
    const cimitStubItem = cimitStubItemService.fromUserCisRequest(
      ciRequest,
      userId,
    );
    await cimitStubItemService.persistCimitStubItem(cimitStubItem);
  }
}

async function deleteCimitStubItems(
  cimitStubItems: CimitStubItem[],
): Promise<void> {
  for (const item of cimitStubItems) {
    await cimitStubItemService.deleteCimitStubItem(item.userId, item.sortKey);
  }
}
