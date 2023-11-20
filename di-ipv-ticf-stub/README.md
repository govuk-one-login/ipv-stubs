### TiCF CRI Stub
This will set up an API gateway in front of a lambda and return TiCF cri VC.
The format of the POST request to the API gateway should look like
```
{
    "vtr": [
        "Cl.Cm.P2"
    ],
    "vot": "P2",
    "vtm": "https://oidc.account.gov.uk/trustmark",
    "sub": "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
    "govuk_signin_journey_id": "44444444-4444-4444-4444-444444444444",
    "https://vocab.account.gov.uk/v1/credentialJWT": [
        "<JWT-encoded VC 1>",
        "<JWT-encoded VC 2>"
    ]
}
```
Response
```
{
    "sub": "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
    "govuk_signin_journey_id": "44444444-4444-4444-4444-444444444444",
    "vtr": [
        "Cl.Cm.P2"
    ],
    "vot": "P2",
    "vtm": "https://oidc.account.gov.uk/trustmark",
    "https://vocab.account.gov.uk/v1/credentialJWT": [
        <Signed JWT TICF VC>
    ]
}
```
VC return
```
{
  "iss": "https://ticf-cri.stubs.account.gov.uk",
  "iat": 1700225169,
  "nbf": 1700225168542,
  "exp": 1700225469,
  "sub": "urn:fdc:gov.uk:2022:56P4CMsGh_02YOlWpd8PAOI-2sVlB2nsNU7mcLZYhYw=",
  "aud": "https://development-di-ipv-core-front.london.cloudapps.digital",
  "vc": {
    "evidence": [
      {
        "type": "RiskAssessment",
        "txn": "27eb16f2-59f9-4aa4-8dc1-e41863ad5169"
      }
    ],
    "type": [
      "VerifiableCredential",
      "RiskAssessmentCredential"
    ]
  },
  "jti": "urn:uuid:40a8486d-54ae-4ced-96bb-9313c81a42dc"
}
```

#### Currently, following SSM parameters are used to control VC structure.
```
/stubs/core/ticf/timeoutVC with value true|false,
```
The Programme has agreed that IPV Core must wait no longer than 2 seconds between request and the VC being returned in a response.
In the cases where the risk assessment could not be fully completed within the 2 sec limit, the TICF CRI will return a lightweight VC as follows :
```
{
  "type": [
    "VerifiableCredential",
    "RiskAssessmentCredential"
  ],
  "evidence": {
    "type": "RiskAssessment"
  }
}
The absence of a "txn" object in the response denotes a VC returned in a timeout scenario
```

```
/stubs/core/ticf/includeCIToVC with value true|false,
```
When this set to 'true' will return VC response with contra-indicator.

There is an API gateway deployed in the stubs build account here:
https://ticf.build.stubs.account.gov.uk
