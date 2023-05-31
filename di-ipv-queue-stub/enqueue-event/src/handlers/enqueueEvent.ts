import { Handler } from "aws-lambda";
import {
    SQSClient,
    GetQueueUrlCommand,
    GetQueueUrlCommandInput,
    GetQueueUrlCommandOutput,
    SendMessageCommand,
    SendMessageCommandInput,
    SendMessageCommandOutput,
    QueueDoesNotExist,
    AddPermissionCommand,
    CreateQueueCommand,
    CreateQueueCommandInput, AddPermissionCommandInput, CreateQueueCommandOutput, AddPermissionCommandOutput
} from "@aws-sdk/client-sqs";
export const handler: Handler = async (
    event: any,
    context:any
) => {
    console.log("event:");
    console.log(event);
    let body = JSON.parse(event.body);
    console.log("body:");
    console.log(body);
    let message = body.queueEvent;
    const sqsConfig = {
        region: "eu-west-2"
    }
    const sqsClient = new SQSClient(sqsConfig);
    let getMessageQueueUrlInput:GetQueueUrlCommandInput = {
        QueueName: body.queueName
    }
    let getMessageQueueUrlCommand = new GetQueueUrlCommand(getMessageQueueUrlInput);
    let getMessageQueueUrlOutput: GetQueueUrlCommandOutput;
    let queueUrl: string ;
    try {
        getMessageQueueUrlOutput = await sqsClient.send(getMessageQueueUrlCommand);
        console.log("getMessageQueueUrlOutput:");
        console.log(getMessageQueueUrlOutput);
        queueUrl = getMessageQueueUrlOutput.QueueUrl;
    }catch(error) {
        console.log("in Catch");
        console.log(error);
        if ( error instanceof QueueDoesNotExist) {
            //queue doesn't exist - let's make one
            console.log("missing queue");
            let createQueueCommandInput:CreateQueueCommandInput = {
                QueueName: body.queueName,
                Attributes: {
                    KmsMasterKeyId: 'alias/sqs/QueuesKmsKey'
                }
            }
            let createQueueCommand = new CreateQueueCommand(createQueueCommandInput);
            let createQueueCommandOutput:CreateQueueCommandOutput = await sqsClient.send(createQueueCommand);
            queueUrl = createQueueCommandOutput.QueueUrl;
            let addQueuePermissionCommandInput:AddPermissionCommandInput = {
                QueueUrl: queueUrl,
                Label: "add read and delete from dev and build perms",
                AWSAccountIds: [
                    "457601271792",
                    "130355686670",
                    "175872367215"
                ],
                Actions: [
                    "ReceiveMessage",
                    "DeleteMessage",
                    "GetQueueAttributes"
                ]
            }
            let addQueuePermissionCommand = new AddPermissionCommand(addQueuePermissionCommandInput);
            let addQueuePermissionOutput:AddPermissionCommandOutput = await sqsClient.send(addQueuePermissionCommand);
            console.log("addQueuePermissionOutput:");
            console.log(addQueuePermissionOutput);
        }else{
            throw(error)
        }
    }
    let sendSqsMessageInput:SendMessageCommandInput = {
        QueueUrl: queueUrl,
        MessageBody: JSON.stringify(message),
    }

    console.log("sendSqsMessageInput:");
    console.log(sendSqsMessageInput);
    let sendSqsMessageCommand = new SendMessageCommand(sendSqsMessageInput);
    let sendSqsMessageCommandOutput:SendMessageCommandOutput = await sqsClient.send(sendSqsMessageCommand);
    console.log("sendSqsMessageCommandOutput:");
    console.log(sendSqsMessageCommandOutput);

    let aws_account_id = JSON.stringify(context.invokedFunctionArn).split(':')[4];

    let response = {
        "statusCode": 200,
        "headers": {
            "Content-Type": "application/json"
        },
        "isBase64Encoded": false,
        "body": "{\n  \"status\": \"enqueued\",\n \"queueArn\": \"arn:aws:sqs:eu-west-2:"
            + aws_account_id + ":" + body.queueName + "\" \n}"
    }
    return response;
}