import { Handler } from "aws-lambda";
import {
    SQSClient,
    GetQueueUrlCommand,
    GetQueueUrlCommandInput,
    GetQueueUrlCommandOutput,
    SendMessageCommand,
    SendMessageCommandInput,
    SendMessageCommandOutput,
    AddPermissionCommand,
    CreateQueueCommand
} from "@aws-sdk/client-sqs";
export const handler: Handler = async (
    event: any,
) => {
    console.log("event:");
    console.log(event);
    let body = JSON.parse(event.body);
    console.log("body:");
    console.log(body);
    let message= body.queueEvent;
    const sqsConfig = {
        region: "eu-west-2"
    }
    const sqsClient = new SQSClient(sqsConfig);
    let getMessageQueueUrlInput:GetQueueUrlCommandInput = {
        QueueName: body.queueName
    }
    let getMessageQueueUrlCommand = new GetQueueUrlCommand(getMessageQueueUrlInput);
    let getMessageQueueUrlOutput:GetQueueUrlCommandOutput = await sqsClient.send(getMessageQueueUrlCommand);
    if(!getMessageQueueUrlOutput){
        throw new Error("no such queue - needs to be created");
    }
    let sendSqsMessageInput:SendMessageCommandInput = {
        QueueUrl:getMessageQueueUrlOutput.QueueUrl,
        MessageBody: message,
    }
    let sendSqsMessageCommand = new SendMessageCommand(sendSqsMessageInput);
    let sendSqsMessageCommandOutput:SendMessageCommandOutput = await sqsClient.send(sendSqsMessageCommand);


    return "enqueued";
}