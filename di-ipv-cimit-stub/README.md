### CIMIT lambda stubs
This will create stubs for the CIMIT lambdas getContraIndicators, getContraIndicatorCredential,
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
Sample getContraIndicators GET request should look like
```
{
  "govuk_signin_journey_id": "value1",
  "ip_address": "value2",
  "user_id": "value3"
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