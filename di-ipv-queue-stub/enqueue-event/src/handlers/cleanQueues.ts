import { EventBridgeHandler } from "aws-lambda";
import { deleteQueue, findBranchQueues } from "../services/queueService";

export const handler: EventBridgeHandler<"Scheduled", {}, void> = async () => {
  const queues = await findBranchQueues();
  console.info(`Found ${queues.length} queues`);
  for (const queueUrl in queues) {
    await deleteQueue(queueUrl);
    console.log(`Deleted ${queueUrl}`);
  }
};
