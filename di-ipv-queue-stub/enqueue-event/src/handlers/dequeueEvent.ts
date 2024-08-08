import { APIGatewayProxyHandlerV2 } from "aws-lambda";
import { dequeueEvent, getOrCreateSqsQueueUrl } from "../services/queueService";
import { jsonResponse } from "../services/responseService";

export const handler: APIGatewayProxyHandlerV2 = async (event, context) => {
  const accountId = context.invokedFunctionArn.split(":")[4];
  const queueName = event.pathParameters?.["queueName"];
  const waitTime = event.queryStringParameters?.["waitTime"];

  if (!queueName?.startsWith("stubQueue_")) {
    return jsonResponse(400, { errorMessage: "Queue name must start with 'stubQueue_'" });
  }

  const queueUrl = await getOrCreateSqsQueueUrl(accountId, queueName);

  const message = await dequeueEvent(queueUrl, waitTime);

  if (message) {
    return jsonResponse(200, message);
  } else {
    return { statusCode: 204 };
  }
};
