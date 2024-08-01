# IPV Queue stub

This will set up an API gateway in front of a lambda and push messages onto an SQS queue.
Currently used for testing F2F asynchronous VC return.

## Sending messages

The format of the POST request to the API gateway should look like

```json
{
    "queueName": "stubQueue_<AnyQueueName>",
    "queueEvent": "<This can be  any shape - it will be sent to the queue as a string>",
    "delaySeconds": "<optional number of seconds to wait before sending to the queue>"
}
```

The name of the queue you are pushing to must have the `stubQueue_` prefix,
and **if the queue doesn't exist it will be created**.

The response should be:

```json
{
    "status": "enqueued",
    "queueArn": "<the queue arn>"
}
```

The API gateway URL is in the outputs of the cloudformation stack, and are:
- [https://queue.stubs.account.gov.uk] for stubs-prod
- [https://queue.build.stubs.account.gov.uk] for stubs-build

The queues and the KMS keys to read them are only accessible from the IPV Core dev and build accounts.
