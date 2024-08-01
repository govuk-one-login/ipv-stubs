import { APIGatewayProxyHandlerV2 } from "aws-lambda";
import {
    AddPermissionCommand,
    CreateQueueCommand,
    GetQueueUrlCommand,
    SendMessageCommand,
    SQSClient,
    QueueDoesNotExist,
} from "@aws-sdk/client-sqs";

interface EnqueueEventPayload {
    queueName: string;
    queueEvent: object;
    delaySeconds: string;
}

const sqsClient = new SQSClient({ region: "eu-west-2" });

const getOrCreateSqsQueueUrl = async (accountId: string, queueName: string): Promise<string> => {
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

const enqeueEvent = async (enqueueEventPayload: EnqueueEventPayload, queueUrl: string): Promise<void> => {
    console.info(`Sending message to ${queueUrl}`);
    await sqsClient.send(new SendMessageCommand({
        QueueUrl: queueUrl,
        MessageBody: JSON.stringify(enqueueEventPayload.queueEvent),
        DelaySeconds: parseInt(enqueueEventPayload.delaySeconds || "0"),
    }));
};

export const handler: APIGatewayProxyHandlerV2 = async (event, context) => {
    const accountId = context.invokedFunctionArn.split(":")[4];
    const enqueueEventPayload = JSON.parse(event.body!) as EnqueueEventPayload;

    if (!enqueueEventPayload.queueName?.startsWith("stubQueue_")) {
        throw new Error("Queue name must start 'stubQueue_'");
    }

    const queueUrl = await getOrCreateSqsQueueUrl(accountId, enqueueEventPayload.queueName);

    await enqeueEvent(enqueueEventPayload, queueUrl);

    return {
        "statusCode": 200,
        "headers": {
            "Content-Type": "application/json"
        },
        "isBase64Encoded": false,
        "body": JSON.stringify({
            status: "enqeueued",
            queueArn: `arn:aws:sqs:eu-west-2:${accountId}:${enqueueEventPayload.queueName}`
        }),
    };
};
