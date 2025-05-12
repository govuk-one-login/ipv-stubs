# AIS Stub
## Summary

This will set up an API gateway in front of a lambda to return account interventions for users.

## Management Endpoint

`POST /ais/{userId}` artificially primes a user with an account intervention response.

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

`GET /management/user/{userId}` fetches the account interventions for that user.

It returns a response with code and body primed by the management endpoint after a delay period.
