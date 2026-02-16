# CIMIT lambda stubs

## Summary

This stub simulates the CIMIT service and also includes a management end point to configure specific behaviours. The
CIMIT service interaction is reasonably complicated, and the stub is even more so due to its management interface.
This summary aims to explain broadly how the stub works and is designed to be used.

### CIMIT behaviour

The CIMIT service has three endpoints that core uses

#### PutContraIndicators

This endpoint accepts a new VC and checks it for contra indicators. It can also notice if a CI is already mitigated by
a VC it has seen previously.

#### PostMitigations

This endpoint accepts an array of VCs (although core normally only sends one at a time) and checks them to see if they
mitigate an existing CI.

#### GetContraIndicators

This endpoint returns a VC containing details of any current CIs and mitigations for the user.

### Stub behaviour

#### PutContraIndicators

The stub analyses the supplied VC and if it contains any CIs then the stub writes a `CimitStubItem` record (including the document field) to the dynamo table specified by
`CIMIT_STUB_TABLE_NAME` for each CI. This table is keyed by `UserId`.

#### PostMitigations

For each JWT, the stub extracts the ID and searches the dynamo table specified by `PENDING_MITIGATIONS_TABLE` for mitigations
associated with that JWT ID. If a mitigation exists then the stub searches `CIMIT_STUB_TABLE_NAME` for any CIs associated with
the `UserId`. If a CI exists in the table as a `CimitStubItem` then the mitigation is either appended to any existing mitigations on the `CimitStubItem` (if
the management API method was POSTed to) or the mitigation replaces any existing mitigations on the `CimitStubItem` (if the management API method was PUT to).

Sample postMitigations POST request should look like
```
headers:
{
    "govuk-signin-journey-id": "value1",
    "ip-address": "value2",
    "x-api-key": "apiKey"
}
body:
{
    "signed_jwts": ["value3"]
}
```

#### GetContraIndicators

This endpoint searches `CIMIT_STUB_TABLE_NAME` by `UserId` and deduplicates and puts any matching `CimitStubItem`s into the returned VC. (Duplicate
CIs are defined as having the same CI code and document values.)

Sample getContraIndicatorCredential GET request should look like
```
query params: user_id
headers:
{
  "govuk-signin-journey-id": "value1",
  "ip-address": "value2",
  "x-api-key": "apiKey"
}
```

#### POST to `/user/<userId>/cis`

This is a management endpoint. It accepts data about CIs in the form of an array of `UserCisRequest`s for a user and writes it to `CIMIT_STUB_TABLE_NAME`. This data can
include mitigations on the CIs.

##### Examples

Create a new CI
```bash
curl -X POST -d '[{"code": "X01", "issuer": "https://issuer.example", "mitigations": ["M01"], "issuanceDate": "2007-12-03T10:15:30.00Z", "document": "a-document-identifier"}]' -H "x-api-key: <API gateway API key>" https://<cimit-stub-domain>/user/<userId>/cis
```

Replace an existing CI
```bash
curl -X PUT -d '[{"code": "X01", "issuer": "https://issuer.example", "mitigations": ["M01"], "issuanceDate": "2007-12-03T10:15:30.00Z", "document": "a-document-identifier"}]' -H "x-api-key: <API gateway API key>" https://<cimit-stub-domain>/user/<userId>/cis
```

#### PUT to `/user/<userId>/cis`

This is the same as `POST`ing except that any `CimitStubItem`s that already exist for the user are deleted first.

Sample putContraIndicators PUT request should look like
```
headers:
{
    "govuk-signin-journey-id": "value1",
    "ip-address": "value2",
    "x-api-key": "apiKey"
}
body:
{
    "signed_jwt": "value3"
}
```

#### PUT or POST to `/user/<userId>/mitigations/<ciCode>`

This is a management endpoint. The body of this call should be a `UserMitigationRequest` and contains a JWT ID (JTI) for the VC that should trigger a mitigation, and the list of mitigations to use.
The `UserId` and CI code are passed in the URL.
This data is put into a `PendingMitigationItem` and stored in the dynamo table specified by `PENDING_MITIGATIONS_TABLE`.

This method is the way that CRI stubs (`../di-ipv-credential-issuer-stub`) action any mitigations you specify to them.

##### Examples
Update a CI with a pending mitigation
```bash
curl -X POST -d '{"mitigations": ["M02", "M03"], "vcJti": "<jti from mitigating VC>"}' -H "x-api-key: <API gateway API key>" https://<cimit-stub-domain>/user/<userId>/mitigations/<CiCode>
```

#### PUT or POST to `/user/<userId>/premitigations/<ciCode>`

This is a management endpoint for setting up pre-mitigations that will be automatically applied to CIs when they are created.
This is needed to simulate scenarios where the CI is mitigated by a VC that CIMIT has received previously.

The request body should be a `UserPreMitigationRequest` containing the list of mitigations to apply.
The `UserId` and CI code are passed in the URL.
This data is put into a `PreMitigationItem` and stored in the dynamo table specified by `PRE_MITIGATIONS_TABLE`.

When a CI is subsequently created via `PutContraIndicators`, the stub checks for any matching pre-mitigations and automatically applies them to the new CI.

##### Examples
Set up a pre-mitigation for a CI
```bash
curl -X POST -d '{"mitigations": ["M01"]}' -H "x-api-key: <API gateway API key>" https://<cimit-stub-domain>/user/<userId>/premitigations/<CiCode>
```

## Environment variables

* IS_LOCAL - This only needs to be assigned when running locally.