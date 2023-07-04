import { Handler } from "aws-lambda";
import {
    SQSClient,
    GetQueueUrlCommand,
    GetQueueUrlCommandInput,
    GetQueueUrlCommandOutput,
    SendMessageCommand,
    SendMessageCommandInput,
    QueueDoesNotExist,
    AddPermissionCommand,
    CreateQueueCommand,
    CreateQueueCommandInput, AddPermissionCommandInput, CreateQueueCommandOutput
} from "@aws-sdk/client-sqs";
import {
    GetSecretValueCommand,
    SecretsManagerClient
} from "@aws-sdk/client-secrets-manager";
import {SSMClient, GetParameterCommand, GetParameterCommandOutput, GetParameterCommandInput} from "@aws-sdk/client-ssm";
import { buildSignedJwt } from 'di-stub-oauth-client';
import type { RequestPayload } from '../types';
import type { SignedJwtParams } from 'di-stub-oauth-client';
export const handler: Handler = async (
    event: any,
    context:any
) => {
    let aws_account_id = JSON.stringify(context.invokedFunctionArn).split(':')[4];
    const body: RequestPayload = JSON.parse(event.body);

    const awsConfig = {
        region: "eu-west-2"
    }
    const sqsClient = new SQSClient(awsConfig);
    let getMessageQueueUrlInput: GetQueueUrlCommandInput = {
        QueueName: body.queueName
    }
    let getMessageQueueUrlCommand = new GetQueueUrlCommand(getMessageQueueUrlInput);
    let getMessageQueueUrlOutput: GetQueueUrlCommandOutput;
    let queueUrl: string;
    try {
        getMessageQueueUrlOutput = await sqsClient.send(getMessageQueueUrlCommand);
        queueUrl = getMessageQueueUrlOutput.QueueUrl;
    } catch (error) {

        if (error instanceof QueueDoesNotExist) {
            //queue doesn't exist - let's make one
            let redrivePolicy = {
                deadLetterTargetArn: `arn:aws:sqs:eu-west-2:${aws_account_id}:stubQueue_F2FDLQ`,
                maxReceiveCount: "1"
            };
            let createQueueCommandInput: CreateQueueCommandInput = {
                QueueName: body.queueName,
                Attributes: {
                    KmsMasterKeyId: 'alias/sqs/QueuesKmsKey',
                    RedrivePolicy: JSON.stringify(redrivePolicy),
                    VisibilityTimeout: "360"
                }
            }
            let createQueueCommand = new CreateQueueCommand(createQueueCommandInput);
            let createQueueCommandOutput: CreateQueueCommandOutput = await sqsClient.send(createQueueCommand);
            queueUrl = createQueueCommandOutput.QueueUrl;
            let addQueuePermissionCommandInput: AddPermissionCommandInput = {
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
            await sqsClient.send(addQueuePermissionCommand);

        } else {
            throw(error)
        }
    }

    let queueBody;
    if(!!body.queueEvent ) {
        queueBody = body.queueEvent;
    }else if(!!body.error) {
        queueBody = {
            sub: body.sub,
            state: body.state,
            error: body.error,
            error_description: body.errorDescription
        };
    }else{
        let buildJwtParams: SignedJwtParams;

        if(!!body.privateSigningKey) { //We have a key provided
            buildJwtParams = {
                issuer: body.issuer,
                customClaims: body.customClaims,
                privateSigningKey: body.privateSigningKey
            }
        }else if(!!body.privateSigningKeyId){ //A Key ID has been provided
            buildJwtParams = {
                issuer: body.issuer,
                customClaims: body.customClaims,
                privateSigningKeyId: body.privateSigningKeyId
            }
        }else if(!!body.secretId){ //A secret manager key name is provided
            let secretsManagerClient = new SecretsManagerClient(awsConfig);
            const getSecretValueCommandInput: GetSecretValueCommandInput = {
                SecretId: body.secretId
            }
            const getSecretValueCommand = new GetSecretValueCommand(getSecretValueCommandInput);
            let getSecretValueCommandOutput: GetQueueUrlCommandOutput = await secretsManagerClient.send(getSecretValueCommand);
            let secretEntry = JSON.parse(getSecretValueCommandOutput.SecretString);

            buildJwtParams = {
                issuer: body.issuer,
                customClaims: body.customClaims,
                privateSigningKey: secretEntry[body.secretId]
            }
        }else{ //An SSM parameter is provided
            let ssmClient = new SSMClient(awsConfig);
            const getParameterCommandInput: GetParameterCommandInput = {
                Name: body.parameterName
            }
            const getParameterCommand = new GetParameterCommand(getParameterCommandInput);
            let getParameterCommandOutput: GetParameterCommandOutput = await ssmClient.send(getParameterCommand);
            buildJwtParams = {
                issuer: body.issuer,
                customClaims: body.customClaims,
                privateSigningKey: getParameterCommandOutput.Parameter.Value
            }
        }

        const returnJwt = await buildSignedJwt(buildJwtParams);
        queueBody = {
            sub: body.sub,
            state: body.state,
            'https://vocab.account.gov.uk/v1/credentialJWT': [returnJwt]
        }
    }

    let sendSqsMessageInput:SendMessageCommandInput = {
        QueueUrl: queueUrl,
        MessageBody: JSON.stringify(queueBody),
        DelaySeconds: 5
    }

    let sendSqsMessageCommand = new SendMessageCommand(sendSqsMessageInput);
    await sqsClient.send(sendSqsMessageCommand);

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