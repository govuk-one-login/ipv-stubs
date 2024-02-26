# CIMIT lambda stubs
This will create stubs for the CIMIT lambdas getContraIndicatorCredential,
putContraIndicators, postMitigations and stubManagement.

Sample postMitigations POST request should look like
```
{
	"govuk_signin_journey_id": "value1",
	"ip_address": "value2",
	"signed_jwts": ["value3"]
}
```
Sample putContraIndicators PUT request should look like
```
{
  "govuk_signin_journey_id": "value1",
  "ip_address": "value2",
  "signed_jwt": "value3"
}
```
Sample getContraIndicatorCredential GET request should look like
```
{
  "govuk_signin_journey_id": "value1",
  "ip_address": "value2",
  "user_id": "value3"
}
```

## Management API

There is a management API which can be used to set up state in the CiMit stub. Below are some sample requests

### Create a new CI

```bash
curl -X POST -d '[{"code": "X01", "issuer": "https://issuer.example", "mitigations": ["M01"], "issuanceDate": "2007-12-03T10:15:30.00Z", "document": "a-document-identifier"}]' -H "x-api-key: <API gateway API key>" https://<cimit-stub-domain>/user/<userId>/cis
```

### Replace an existing CI

```bash
curl -X PUT -d '[{"code": "X01", "issuer": "https://issuer.example", "mitigations": ["M01"], "issuanceDate": "2007-12-03T10:15:30.00Z", "document": "a-document-identifier"}]' -H "x-api-key: <API gateway API key>" https://<cimit-stub-domain>/user/<userId>/cis
```

### Update a CI with a pending mitigation

```bash
curl -X POST -d '{"mitigations": ["M02", "M03"], "vcJti": "<jti from mitigating VC>"}' -H "x-api-key: <API gateway API key>" https://<cimit-stub-domain>/user/<userId>/mitigations/<CIcode>
```

## Environment variables

* IS_LOCAL - This only needs to be assigned when running locally.