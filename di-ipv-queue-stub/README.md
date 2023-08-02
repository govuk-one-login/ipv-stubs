### Enqueue messages
This will set up an API gateway in front of a lambda and push messages onto an SQS queue.
The format of the POST request to the API gateway should look like
```
{
    "queueName": "stubQueue_AnyQueueName",
    "queueEvent": <This can be  any shape - it will be sent to the queue as a string>,
    "delaySeconds": Optional number of seconds to wait before porting  to the queue
}
```
or
```
{
    "queueName": "stubQueue_AnyQueueName",
    "sub": <the sub field in the queue message body>
    "state": <the state field in the queue message body>
    "issuer": <name of the issuer you would like added to the queue>
    "customClaims" : <the vc you want this to put onto the queue>
    "privateSiginingKeyId" : <a key ID that the lamda can access, one is provided by the stack>,
    "delaySeconds": Optional number of seconds to wait before porting  to the queue
}
```
or
```
{
    "queueName": "stubQueue_AnyQueueName",
    "sub": <the sub field in the queue message body>
    "state": <the state field in the queue message body>
    "issuer": <name of the issuer you would like added to the queue>
    "customClaims" : <the vc you want this to put onto the queue>
    "secretId" : <a secret string in secrets manager containing key material>,
    "delaySeconds": Optional number of seconds to wait before porting  to the queue
}
```
or
```
{
    "queueName": "stubQueue_AnyQueueName",
    "sub": <the sub field in the queue message body>
    "state": <the state field in the queue message body>
    "issuer": <name of the issuer you would like added to the queue>
    "customClaims" : <the vc you want this to put onto the queue>
    "parameterName" : <name of a string in ssm containing key material>,
    "delaySeconds": Optional number of seconds to wait before porting  to the queue
}
```
or
```
{
    "queueName": "stubQueue_AnyQueueName",
    "sub": <the sub field in the queue message body>
    "state": <the state field in the queue message body>
    "issuer": <name of the issuer you would like added to the queue>
    "customClaims" : <the vc you want this to put onto the queue>
    "privateSiginingKey" : <a key to sign the JWT>,
    "delaySeconds": Optional number of seconds to wait before porting  to the queue
}
```
or
```
{
    "queueName": "stubQueue_AnyQueueName",
    "sub": <the sub field in the queue message body>
    "state": <the state field in the queue message body>
    "issuer": <name of the issuer you would like added to the queue>
    "error" : <the error to put onto the queue message>
    "error_description" : <the error_description tp put in the message>,
    "delaySeconds": Optional number of seconds to wait before porting  to the queue
}
```
The name of the queue you are pushing to must have the `stubQueue_` prefix
The response should be:
```
{
    "status": "enqueued",
    "queueArn": "<the queue arn>"
}
```
If the queue doesn't exist it will be created. The queues and the KMS keys to read them
are only accessible from the IPV Core dev and build accounts.
The API gateway URL is in the outputs of the cloudformation stack.
There is an API gateway deployed in the stubs build account here:
https://96d4sab539.execute-api.eu-west-2.amazonaws.com/prod
