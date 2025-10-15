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
            "eyJraWQiOiJ0ZXN0LXNpZ25pbmcta2V5IiwidHlwIjoiSldUIiwiYWxnIjoiRVMyNTYifQ.eyJhdWQiOiJodHRwczovL3JldXNlLWlkZW50aXR5LmJ1aWxkLmFjY291bnQuZ292LnVrIiwic3ViIjoiNTExMjdhMTI3NWYxMDVkZmQzYmM1MjY0Yjk0NWZhMzEiLCJuYmYiOjE3NTE4OTQxNzEsImNyZWRlbnRpYWxzIjpbIk9sczNFNnhvV2RmdnAxSmJwQ3FLeUNVSDZhOFdEYnpVUXZVcEhTS0xySjJ4RDd0dGdPUnE4MTIxYkM0N2pkbG1XUGJDdzRaMEN1ekc1Nmg0bXNWNmpRIiwiby1OUDdFY2VGa1JxMEVZaGIydlROYmZWczZ2UkNRSjU4SUpYZURGVGxMZ2ZKWFNMdTdXeHktQV9CVXlYYnFsOGJBaS1IUzAzbk9hc0JlWWdjUEUwcVEiLCJnVDBvSW1hRFROQW5kVXMxQnpFTEZILVE5cWdncDI3bVFUYnZiSWFYaFVmSjZ2clJuQnVlX2pKSV84Mm9va1NSRmt1WlVFQUFsRklfXzJoRGIzb3pwdyIsIlAya0J6U1hOb2QwZmQ2MjNjOWJmTU1HQ0ZiSTRHamlYcWNLY1JrY1NKMU5YRmh0UXltSUxiOERCVFlKUjhuc29KX3N4TXM0UDYxcVZmbE1hbjBNdE5nIl0sImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkubG9jYWwuYWNjb3VudC5nb3YudWsiLCJjbGFpbXMiOnsiaHR0cHM6Ly92b2NhYi5hY2NvdW50Lmdvdi51ay92MS9jb3JlSWRlbnRpdHkiOnsibmFtZSI6W3sibmFtZVBhcnRzIjpbeyJ0eXBlIjoiR2l2ZW5OYW1lIiwidmFsdWUiOiJLRU5ORVRIIn0seyJ0eXBlIjoiRmFtaWx5TmFtZSIsInZhbHVlIjoiREVDRVJRVUVJUkEifV19XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5NjUtMDctMDgifV19LCJodHRwczovL3ZvY2FiLmFjY291bnQuZ292LnVrL3YxL2FkZHJlc3MiOlt7ImFkZHJlc3NDb3VudHJ5IjoiR0IiLCJhZGRyZXNzTG9jYWxpdHkiOiJCQVRIIiwiYnVpbGRpbmdOYW1lIjoiIiwiYnVpbGRpbmdOdW1iZXIiOiI4IiwicG9zdGFsQ29kZSI6IkJBMiA1QUEiLCJzdHJlZXROYW1lIjoiSEFETEVZIFJPQUQiLCJzdWJCdWlsZGluZ05hbWUiOiIiLCJ1cHJuIjoxMDAxMjAwMTIwNzcsInZhbGlkRnJvbSI6IjEwMDAtMDEtMDEifV0sImh0dHBzOi8vdm9jYWIuYWNjb3VudC5nb3YudWsvdjEvcGFzc3BvcnQiOlt7ImRvY3VtZW50TnVtYmVyIjoiMzIxNjU0OTg3IiwiZXhwaXJ5RGF0ZSI6IjIwMzAtMDEtMDEiLCJpY2FvSXNzdWVyQ29kZSI6IkdCUiJ9XX0sInZvdCI6IlAxIiwiaWF0IjoxNzUxODk0MTcxfQ.rTXoZ3c7xZIUBO4W2h__NWMwZfjWk5RcZskBWjH_KRldOgQ4KlmIBsakY456SsbplI6YfniAZo0EC5dVqsuMFw" // pragma: allowlist secret
        ],
        "credentials": [
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

