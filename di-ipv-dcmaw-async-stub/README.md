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

#### Currently, following SSM parameters are used to control VC structure.
To save hits to SSM we have a single config value in the form of a JSON string
```
/stubs/core/dcmawAsync/config

{
  dummyClientId: string;
  dummySecret: string;
  dummyAccessTokenValue: string;
}
```
