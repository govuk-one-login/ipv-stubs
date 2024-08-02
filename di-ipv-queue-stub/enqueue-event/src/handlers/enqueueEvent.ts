import { APIGatewayProxyHandlerV2 } from "aws-lambda";
import { enqeueEvent, getOrCreateSqsQueueUrl } from "../services/queueService";
import { jsonResponse } from "../services/responseService";

interface EnqueueEventPayload {
    queueName: string;
    queueEvent: object;
    delaySeconds: string;
}

export const handler: APIGatewayProxyHandlerV2 = async (event, context) => {
    const accountId = context.invokedFunctionArn.split(":")[4];
    const enqueueEventPayload = JSON.parse(event.body!) as EnqueueEventPayload;

    if (!enqueueEventPayload.queueName?.startsWith("stubQueue_")) {
        return jsonResponse(400, { errorMessage: "Queue name must start with 'stubQueue_'" });
    }

    const queueUrl = await getOrCreateSqsQueueUrl(accountId, enqueueEventPayload.queueName);

    await enqeueEvent(enqueueEventPayload, queueUrl);

    return jsonResponse(200, {
        status: "enqeueued",
        queueArn: `arn:aws:sqs:eu-west-2:${accountId}:${enqueueEventPayload.queueName}`
    });
};
