# AIS Stub
## Summary

This will set up an API gateway in front of a lambda to return account interventions for users.

## Management Endpoint

`POST /management/user/{userId}` artificially primes a user with an account intervention response.

This endpoint takes a request body which references the intervention description. E.g.:
```json
{
  "statusCode": 200,
  "intervention": "AIS_NO_INTERVENTION",
  "responseDelay": 0
}
```

- `statusCode` set on the response.
- `intervention` mapped to a response body in `/lambdas/src/data`.
- `responseDelay` delays the response when called.

## AIS Endpoint

`GET /ais/{userId}` fetches the account interventions for that user.

It returns a response with code and body primed by the management endpoint after a delay period.

## Testing

In order to test it works, you can run the following on the deployed environment. It shows the call to the management endpoint to stub the account interventions, followed by the call to get account interventions. In this example, we are using the `dev-mikec` environment:

```bash
curl \                                                                                                       
   -d '{ "intervention": "AIS_NO_INTERVENTION" }' \
   -H "Content-Type: application/json" \
   -X POST https://ais-dev-mikec.02.core.dev.stubs.account.gov.uk/management/user/urn:uuid:42810001-5786-4a2d-a8ee-6d54cc386881

curl https://ais-dev-mikec.02.core.dev.stubs.account.gov.uk/ais/urn:uuid:42810001-5786-4a2d-a8ee-6d54cc386881
```
