### CIMIT lambda stubs
This will create stub lambda's for CIMIT lambda's getContraIndicators, getContraIndicatorCredential,
putContraIndicators and postMitigations
The format of the postMitigations POST request should look like
```
{
	"govuk_signin_journey_id": "value1",
	"ip_address": "value2",
	"signed_jwts": ["value3"]
}
```
The format of the putContraIndicators PUT request should look like
```
{
}
```
The format of the getContraIndicators GET request should look like
```
{
}
```
The format of the getContraIndicatorCredential GET request should look like
```
{
  "govuk_signin_journey_id": "value1",
  "ip_address": "value2",
  "user_id": "value3"
}
```