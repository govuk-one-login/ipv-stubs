### DCMAW Async CRI Stub
This will set up an API gateway in front of a lambda and return access tokens and interim DCMAW cri VCs.
The async/token POST request to the API gateway should look like
```
Content-Type: application/x-www-form-urlencoded
Authorization: Basic BASE64_ENCODED_CLIENT_ID_AND_SECRET

grant_type=client_credentials
```
Response
```
HTTP/1.1 200 OK
{
    "access_token": "<Set in config, use same every time>",
    "token_type": "Bearer",
    "expires_in": 3600
}
```
The async/credential POST request to the API gateway should look like
```
Content-Type: application/json
Authorization: Bearer ACCESS_TOKEN

{
  "sub": "<user id>",
  "govuk_signin_journey_id": "44444444-4444-4444-4444-444444444444",
  "client_id": "CLIENT_ID",
  "state": "RANDOM_VALUE",
  "redirect_uri": "https://example/redirect"
}
```
Response
```
HTTP/1.1 201 Created
{
    "sub": "<user id as supplied in the request>",
    "https://vocab.account.gov.uk/v1/credentialStatus": "pending"
}
```

#### Additionally a management endpoint is exposed, which can be hit manually (for example via curl).
The `management/enqueueVc` endpoint will build and sign a VC based on the inputs provided and push a VC message onto the CRI response queue (via the queue stub lambda).

The `management/generateVc` endpoint will build and sign a VC based on the inputs provided and return it directly.

The `management/enqueueError` endpoint will push an error message onto the CRI response queue, for example to simulate an "access-denied" error scenario.

See `openAPI/dcmaw-async-external.yaml` for the shape of the requests and expected responses. The user id provided must be that of an already initialised DCMAW session (via the `async/credential` request). The oauth state value passed in the original `async/credential` request will have been stored against the user id so that it can be provided in the VC queue message.

#### Currently, following SSM parameters are used to control VC structure.
To save hits to SSM we have a single config value in the form of a JSON string
```
/stubs/core/dcmawAsync/config
```
The interface `SsmConfig` can be found in `/lambdas/src/common/config.ts`
