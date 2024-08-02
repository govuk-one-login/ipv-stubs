import {
  AddPermissionCommand,
  CreateQueueCommand,
  DeleteMessageCommand,
  GetQueueUrlCommand,
  Message,
  QueueDoesNotExist,
  ReceiveMessageCommand,
  SendMessageCommand,
  SQSClient,
} from "@aws-sdk/client-sqs";

const sqsClient = new SQSClient({ region: "eu-west-2" });

export const getOrCreateSqsQueueUrl = async (
  accountId: string,
  queueName: string,
): Promise<string> => {
    try {
        const queueUrlResponse = await sqsClient.send(new GetQueueUrlCommand({
            QueueName: queueName,
        }));
        console.info(`Queue already exists ${queueName}`);
        return queueUrlResponse.QueueUrl!;
    } catch (error) {
        if (error instanceof QueueDoesNotExist) {
            console.info(`Creating queue ${queueName}`);

            // Create the queue if it does not exist
            const redrivePolicy = {
                deadLetterTargetArn: `arn:aws:sqs:eu-west-2:${accountId}:stubQueue_F2FDLQ`,
                maxReceiveCount: "1"
            };
            const createQueueResponse = await sqsClient.send(new CreateQueueCommand({
                QueueName: queueName,
                Attributes: {
                    KmsMasterKeyId: "alias/sqs/QueuesKmsKey",
                    RedrivePolicy: JSON.stringify(redrivePolicy),
                    VisibilityTimeout: "360"
                }
            }));

            console.info(`Created queue ${queueName}`);

            // Add permissions to consume the queue from IPV Core accounts
            await sqsClient.send(new AddPermissionCommand({
                QueueUrl: createQueueResponse.QueueUrl,
                Label: "add read and delete from dev, build, staging, prod perms",
                AWSAccountIds: [
                    "457601271792",
                    "130355686670",
                    "175872367215",
                    "335257547869",
                    "991138514218",
                    "075701497069"
                ],
                Actions: [
                    "ReceiveMessage",
                    "DeleteMessage",
                    "GetQueueAttributes"
                ]
            }));

            console.info("Attached permissions");

            return createQueueResponse.QueueUrl!;
        }
        throw error;
    }
};

export const enqeueEvent = async (
  event: object,
  queueUrl: string,
  delaySeconds?: string,
): Promise<void> => {
    console.info(`Sending message to ${queueUrl}`);
    await sqsClient.send(new SendMessageCommand({
        QueueUrl: queueUrl,
        MessageBody: JSON.stringify(event),
        DelaySeconds: parseInt(delaySeconds || "0"),
    }));
};

export const dequeueEvent = async (
  queueUrl: string,
  waitTime?: string,
): Promise<unknown | null> => {
  console.info(`Retrieving from ${queueUrl}`);
  const response = await sqsClient.send(new ReceiveMessageCommand({
    QueueUrl: queueUrl,
    WaitTimeSeconds: waitTime ? parseInt(waitTime) : undefined,
  }));

  if (response.Messages?.length) {
    console.info("Received message");
    const message = response.Messages[0];

    // Ordinarily this should be done separately
    // to indicate that a message has been successfully processed
    // but for a stub it's ok to consume immediately
    await sqsClient.send(new DeleteMessageCommand({
      QueueUrl: queueUrl,
      ReceiptHandle: message.ReceiptHandle,
    }));

    try {
      return JSON.parse(message.Body!);
    } catch (err) {
      console.error(`Message contains invalid JSON: ${message.Body}`);
      throw err;
    }
  } else {
    console.info("No messages");
    return null;
  }
};
