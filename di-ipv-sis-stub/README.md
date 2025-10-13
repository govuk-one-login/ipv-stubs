# SIS Stub
## Summary

This will set up an API Gateway in front of a lambda to return the stored identity (SI) record for a given user.


## `GET /user-identity`

Gets a user's SI record.
The endpoint does not accept any query/path parameters and does not accept a request body.
It does however require an `Authorization: Bearer ...` header and an api key passed into the header.

### Example Response
```
{
    "vot": "P2",
    "content": {
        "sub": "eae01ac290a984d0ea7c33cc45e36f12", // pragma: allowlist secret
        "vot": "P2",
        "vtm": "https://oidc.account.gov.uk/trustmark",
        "https://vocab.account.gov.uk/v1/credentialJWT": [
            "N7PxhfkFkmyTQFKyAXMyS_H6NuF-wDzEktb_dVurulSRMMXhnxhbR2rxs9Tc-KQB0iXb1_9aBI8XCy2AbGQvFQ", // pragma: allowlist secret
            "S4NJPciimbfx08js9m98hsrKL4bJHtBQyKGtrdIzIfYmBPjrU9paz_u_1hCrHZ8ijyQo5RPmQlMP-_c5euvZHw", // pragma: allowlist secret
            "A9OHuKI8N5h4C457Q4qtNvkSFKfFeVM4sEGwqRPcSHiQylzhxRyq00e1DUQLmSdie9XIk0CfiQSA_r7-mmCbAw", // pragma: allowlist secret
            "y44v0pEA88zuDhDFDCDcPgn96p9bSFojxvPA1BxGXNxD0nPzQN6MRhmOYpSQx8Mov_3KYAxnfyiwRzeArXJkqA" // pragma: allowlist secret
        ]
        "https://vocab.account.gov.uk/v1/coreIdentity": {
          "name": [
            {
              "nameParts": [
                {
                  "type": "GivenName",
                  "value": "KENNETH"
                },
                {
                  "type": "FamilyName",
                  "value": "DECERQUEIRA"
                }
              ]
            }
          ],
          "birthDate": [
            {
              "value": "1965-07-08"
            }
          ]
        },
        "https://vocab.account.gov.uk/v1/address": [
          {
            "addressCountry": "GB",
            "addressLocality": "BATH",
            "buildingName": "",
            "buildingNumber": "8",
            "postalCode": "BA2 5AA",
            "streetName": "HADLEY ROAD",
            "subBuildingName": "",
            "uprn": 100120012077,
            "validFrom": "1000-01-01"
          }
        ],
        "https://vocab.account.gov.uk/v1/passport": [
          {
            "documentNumber": "321654987",
            "expiryDate": "2030-01-01",
            "icaoIssuerCode": "GBR"
          }
        ]
    }
    "isValid": true,
    "expired": false,
    "govukSigninJourneyId": "someId",
    "vtr": ["P2"]
}
```

