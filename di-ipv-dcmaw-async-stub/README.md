# DCMAW Async CRI Stub

## Overview
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

## Management Endpoints
Additionally, a management endpoint is exposed, which can be hit manually (for example via curl).

See `openAPI/dcmaw-async-external.yaml` for the shape of the requests and expected responses. The user id provided must be that of an already initialised DCMAW session (via the `async/credential` request). The oauth state value passed in the original `async/credential` request will have been stored against the user id so that it can be provided in the VC queue message.

### `management/enqueueVc`
The `management/enqueueVc` endpoint will build and sign a VC based on the inputs provided and push a VC message onto the CRI response queue (via the queue stub lambda).

The message will look something like this:
```
{
  "sub": "<user id>",
  "state": "<oauth state>",
  "govuk_signin_journey_id": "<journey id>",
  "https://vocab.account.gov.uk/v1/credentialJWT": ["<signed jwt>"]
}
```

Whilst it is possible to send an evidence and credential subject block in the request body:
```
{
  "credential_subject": {...},
  "evidence": {...}
}
```

It's also possible to enqueue a VC with pre-defined claims to avoid having to create the credential and evidence blocks manually:
```
{
  "userId": "a-user-id",
  "test_user": "kennethD", # Check the api spec to see what values we support for this
  "document_type": "drivingPermit", # Check the api spec to see what values we support for this
  "evidence_type": "success", # Check the api spec to see what values we support for this
  "driving_permit_expiry_date": "2030-01-01" # An optional parameter which overrides the expiry date of the driving permit set by the stub (by default, it is 30 days after the issued at date ie when the VC was generated),
  "ci": ["CI1"]
}
```

### `management/generateVc`
The `management/generateVc` endpoint will build and sign a VC based on the inputs provided and return it directly.

### `management/enqueueError`
The `management/enqueueError` endpoint will push an error message onto the CRI response queue, for example to simulate an "access-denied" error scenario.

### Currently, following SSM parameters are used to control VC structure.
To save hits to SSM we have a single config value in the form of a JSON string
```
/stubs/core/dcmawAsync/config
```
The interface `SsmConfig` can be found in `/lambdas/src/common/config.ts`

## Developing the stub

If you want to run a dev version of the stub and have your dev core-back talk to it:
- Deploy your dev stub `dev-deploy deploy -u <user> -s dcmaw-async-stub`
- In config find the `/core/credentialIssuers/dcmawAsync/activeConnection` value in the relevant dev01 or dev02 file
- Update the value to `dev`
  - You shouldn't have to, but you may also need to manually replace the `${dev-name}`s
- Update your config `dev-deploy params -u <user> -dev0X`
