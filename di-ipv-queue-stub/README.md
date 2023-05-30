### Enqueue messages
This will set up an API gateway in front of a lambda and push messages onto an SQS queue.
The format of the POST request to the API gateway should look like
```
{
  "queueName": "stubQueue_F2FMessageQueue",
  "queueEvent": {
    "eventName": "Name of the event",
    "eventDescription": "Description - queueEvent can be any shape"
  }
}
```
The name of the queue you are pushing to must have the `stubQueue_` prefix
If the queue doesn't exist it will be created. The queues and the KMS keys to read them
are only accessible from the IPV Core dev and build accounts.