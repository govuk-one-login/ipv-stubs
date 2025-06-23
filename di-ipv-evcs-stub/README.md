### EVCS Stub
This will set up an API gateway in front of a lambda.
(ref. - https://govukverify.atlassian.net/wiki/spaces/AB/pages/4083089473/Draft+Proposal+-+API+Spec)

1) The format of the POST (/vcs/<user-id>) request
Save multiple VCs for a specific user
Example request
```
[
    {
        "vc": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ", # pragma: allowlist secret
        "state": "CURRENT",
        "metadata": {
            reason: "test-created",
            timestampMs: "1714478033959",
            txmaEventId: "txma-event-id",
            testProperty: "testProperty"
        },
        "provenance": "ONLINE"
    },
    {
        "vc": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ", # pragma: allowlist secret
        "state": "CURRENT",
        "metadata": {
            reason: "test-created",
            timestampMs: "1714478033959",
            txmaEventId: "txma-event-id",
            testProperty: "testProperty"
        },
        "provenance": "ONLINE"
    }
]
```
Responses
```
        "202":
          description: "Accepted"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/PersistResponse"
        "400":
          $ref: "#/components/responses/BadRequest"
        "401":
          $ref: "#/components/responses/Unauthorized"
        "403":
          $ref: "#/components/responses/Forbidden"
        "404":
          $ref: "#/components/responses/NotFound"
        "413":
          $ref: "#/components/responses/ContentTooLarge"
        "429":
          $ref: "#/components/responses/Throttled"
        "500":
          $ref: "#/components/responses/ServerError"
        "502":
          $ref: "#/components/responses/BadGateway"
        "504":
          $ref: "#/components/responses/GatewayTimeout"
        default:
          $ref: "#/components/responses/UnexpectedError"
```

2) The format of the PATCH (/vcs/<user-id>) request
Update multiple VCs' state and metadata for a specific user
Example request
```
[
    {
        "signature": "qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ", # pragma: allowlist secret
        "state": "CURRENT",
        "metadata": {
            reason: "test-created",
            timestampMs: "1714478033959",
            txmaEventId: "txma-event-id",
            testProperty: "testProperty"
        }
    },
    {
        "vc": "NCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ", # pragma: allowlist secret
        "state": "ABANDONED"
    }
]
```
Responses
```
        "204":
          description: "VCs updated successfully"
        "400":
          $ref: "#/components/responses/BatchUpdateError"
        "401":
          $ref: "#/components/responses/Unauthorized"
        "403":
          $ref: "#/components/responses/Forbidden"
        "404":
          $ref: "#/components/responses/BatchUpdateError"
        "413":
          $ref: "#/components/responses/ContentTooLarge"
        "429":
          $ref: "#/components/responses/Throttled"
        "500":
          $ref: "#/components/responses/ServerError"
        "502":
          $ref: "#/components/responses/BadGateway"
        "504":
          $ref: "#/components/responses/GatewayTimeout"
        default:
          $ref: "#/components/responses/UnexpectedError"
```

3) The format of the GET (/vcs/<user-id>) request

Responses
```
        "200":
          description: "Ok"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/VCsResponse"
        "400":
          $ref: "#/components/responses/BadRequest"
        "401":
          $ref: "#/components/responses/Unauthorized"
        "403":
          $ref: "#/components/responses/Forbidden"
        "404":
          $ref: "#/components/responses/NotFound"
        "415":
          $ref: "#/components/responses/UnsupportedMediaType"
        "429":
          $ref: "#/components/responses/Throttled"
        "500":
          $ref: "#/components/responses/ServerError"
        "502":
          $ref: "#/components/responses/BadGateway"
        "504":
          $ref: "#/components/responses/GatewayTimeout"
        default:
          $ref: "#/components/responses/UnexpectedError"
```

There is an API gateway deployed in the stubs build account here:
https://evcs.build.stubs.account.gov.uk/vc-update

4) The format of the PUT (/vcs) request
Example request
```
{
 "userId": "userId",
 "vcs": [
    {
        "vc": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.qf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ", # pragma: allowlist secret
        "state": "CURRENT",
        "metadata": {
            reason: "test-created",
            timestampMs: "1714478033959",
            txmaEventId: "txma-event-id",
            testProperty: "testProperty"
        },
        "provenance": "ONLINE"
    }
 ],
 "si": {
    "jwt": "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1cm46dXVpZDo5NjI4OTgxNS0wZTUyLTQ4MDAtOTZkZi0xZmY3ZGU5ODFjZDQiLCJuYW1lIjoiSm9obiBEb2UiLCJpYXQiOjE3NDYwODkxNTEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYnVpbGQuYWNjb3VudC5nb3YudWsiLCJhdWQiOiJodHRwczovL29yY2guc3R1YnMuYWNjb3VudC5nb3YudWsiLCJuYmYiOiIxNzc3NjI1MTUxIiwidm90IjoiUDIiLCJjcmVkZW50aWFscyI6WyJleUowZVhBaU9pSktWMVFpTENKaGJHY2lPaUpGVXpJMU5pSjkuZXlKemRXSWlPaUoxY200NmRYVnBaRG81TmpJNE9UZ3hOUzB3WlRVeUxUUTRNREF0T1Raa1ppMHhabVkzWkdVNU9ERmpaRFFpTENKaGRXUWlPaUpvZEhSd2N6b3ZMMmxrWlc1MGFYUjVMbUoxYVd4a0xtRmpZMjkxYm5RdVoyOTJMblZySWl3aWJtSm1Jam94TnpRMk1Ea3lOVGszTENKcGMzTWlPaUpvZEhSd2N6b3ZMMkZrWkhKbGMzTXRZM0pwTG5OMGRXSnpMbUZqWTI5MWJuUXVaMjkyTG5Wcklpd2lkbU1pT25zaWRIbHdaU0k2V3lKV1pYSnBabWxoWW14bFEzSmxaR1Z1ZEdsaGJDSXNJa0ZrWkhKbGMzTkRjbVZrWlc1MGFXRnNJbDBzSW1OeVpXUmxiblJwWVd4VGRXSnFaV04wSWpwN0ltRmtaSEpsYzNNaU9sdDdJbUZrWkhKbGMzTkRiM1Z1ZEhKNUlqb2lSMElpTENKaWRXbHNaR2x1WjA1aGJXVWlPaUlpTENKemRISmxaWFJPWVcxbElqb2lTRUZFVEVWWklGSlBRVVFpTENKd2IzTjBZV3hEYjJSbElqb2lRa0V5SURWQlFTSXNJbUoxYVd4a2FXNW5UblZ0WW1WeUlqb2lPQ0lzSW1Ga1pISmxjM05NYjJOaGJHbDBlU0k2SWtKQlZFZ2lMQ0oyWVd4cFpFWnliMjBpT2lJeU1EQXdMVEF4TFRBeEluMWRmWDBzSW1wMGFTSTZJblZ5YmpwMWRXbGtPbU13TlRWbFlXVmpMVEF5WmpVdE5EUTFOQzA1TnpreUxUWXlZemxqTldRM1l6QXdOeUo5LnhkSHlaVUV3d2k2VENpTTM4VXlOZEgtYkhkQjE0QnhtNm0xVWNuWE5SR1Z4cXFHR0R1cWgwODdsTDNtT0ZNd1BFVWxkTEU2TmVKOUI4dFZkSHJrUnlBIl0sImNsYWltcyI6W3siZXhwaXJ5RGF0ZSI6IjIwMzAtMDEtMDEiLCJpY2FvSXNzdWVyQ29kZSI6IkdCUiIsImRvY3VtZW50TnVtYmVyIjoiMzIxNjU0OTg3In0seyJuYW1lIjpbeyJuYW1lUGFydHMiOlt7InR5cGUiOiJHaXZlbk5hbWUiLCJ2YWx1ZSI6IktFTk5FVEgifSx7InR5cGUiOiJGYW1pbHlOYW1lIiwidmFsdWUiOiJERUNFUlFVRUlSQSJ9XX1dLCJiaXJ0aERhdGUiOlt7InZhbHVlIjoiMTk2NS0wNy0wOCJ9XX1dfQ.DjY3mL3f1U1ehHILXz0ifAosUBCLH3HdAnqYX9YH4t7JdQjSECU885RJKUKZqE33vMvG0n-Ip1tULP7hkQ0R_A", # pragma: allowlist secret
    "vot": "P2"
 }
}
```
Responses
```
        202:
          description: VCs Accepted
          content:
            application/json:
              schema:
                type: object
                properties:
                  messageId:
                    type: string
                    description: The SQS message ID.
                    example: "bd8359d9-d559-47dd-9467-2a31e88a9e2d"
        400:
          $ref: '#/components/responses/BadRequest'
        403:
          $ref: '#/components/responses/Forbidden'
        500:
          $ref: '#/components/responses/ServerError'
```

5) The format of the `/identity` POST endpoint
Example request body
```
{
 "userId": "userId",
 "si": {
    "jwt": "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1cm46dXVpZDo5NjI4OTgxNS0wZTUyLTQ4MDAtOTZkZi0xZmY3ZGU5ODFjZDQiLCJuYW1lIjoiSm9obiBEb2UiLCJpYXQiOjE3NDYwODkxNTEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYnVpbGQuYWNjb3VudC5nb3YudWsiLCJhdWQiOiJodHRwczovL29yY2guc3R1YnMuYWNjb3VudC5nb3YudWsiLCJuYmYiOiIxNzc3NjI1MTUxIiwidm90IjoiUDIiLCJjcmVkZW50aWFscyI6WyJleUowZVhBaU9pSktWMVFpTENKaGJHY2lPaUpGVXpJMU5pSjkuZXlKemRXSWlPaUoxY200NmRYVnBaRG81TmpJNE9UZ3hOUzB3WlRVeUxUUTRNREF0T1Raa1ppMHhabVkzWkdVNU9ERmpaRFFpTENKaGRXUWlPaUpvZEhSd2N6b3ZMMmxrWlc1MGFYUjVMbUoxYVd4a0xtRmpZMjkxYm5RdVoyOTJMblZySWl3aWJtSm1Jam94TnpRMk1Ea3lOVGszTENKcGMzTWlPaUpvZEhSd2N6b3ZMMkZrWkhKbGMzTXRZM0pwTG5OMGRXSnpMbUZqWTI5MWJuUXVaMjkyTG5Wcklpd2lkbU1pT25zaWRIbHdaU0k2V3lKV1pYSnBabWxoWW14bFEzSmxaR1Z1ZEdsaGJDSXNJa0ZrWkhKbGMzTkRjbVZrWlc1MGFXRnNJbDBzSW1OeVpXUmxiblJwWVd4VGRXSnFaV04wSWpwN0ltRmtaSEpsYzNNaU9sdDdJbUZrWkhKbGMzTkRiM1Z1ZEhKNUlqb2lSMElpTENKaWRXbHNaR2x1WjA1aGJXVWlPaUlpTENKemRISmxaWFJPWVcxbElqb2lTRUZFVEVWWklGSlBRVVFpTENKd2IzTjBZV3hEYjJSbElqb2lRa0V5SURWQlFTSXNJbUoxYVd4a2FXNW5UblZ0WW1WeUlqb2lPQ0lzSW1Ga1pISmxjM05NYjJOaGJHbDBlU0k2SWtKQlZFZ2lMQ0oyWVd4cFpFWnliMjBpT2lJeU1EQXdMVEF4TFRBeEluMWRmWDBzSW1wMGFTSTZJblZ5YmpwMWRXbGtPbU13TlRWbFlXVmpMVEF5WmpVdE5EUTFOQzA1TnpreUxUWXlZemxqTldRM1l6QXdOeUo5LnhkSHlaVUV3d2k2VENpTTM4VXlOZEgtYkhkQjE0QnhtNm0xVWNuWE5SR1Z4cXFHR0R1cWgwODdsTDNtT0ZNd1BFVWxkTEU2TmVKOUI4dFZkSHJrUnlBIl0sImNsYWltcyI6W3siZXhwaXJ5RGF0ZSI6IjIwMzAtMDEtMDEiLCJpY2FvSXNzdWVyQ29kZSI6IkdCUiIsImRvY3VtZW50TnVtYmVyIjoiMzIxNjU0OTg3In0seyJuYW1lIjpbeyJuYW1lUGFydHMiOlt7InR5cGUiOiJHaXZlbk5hbWUiLCJ2YWx1ZSI6IktFTk5FVEgifSx7InR5cGUiOiJGYW1pbHlOYW1lIiwidmFsdWUiOiJERUNFUlFVRUlSQSJ9XX1dLCJiaXJ0aERhdGUiOlt7InZhbHVlIjoiMTk2NS0wNy0wOCJ9XX1dfQ.DjY3mL3f1U1ehHILXz0ifAosUBCLH3HdAnqYX9YH4t7JdQjSECU885RJKUKZqE33vMvG0n-Ip1tULP7hkQ0R_A", # pragma: allowlist secret
    "vot": "P2"
 }
}
```
Responses
```
        202:
          description: VCs Accepted
          content:
            application/json:
              schema:
                type: object
                properties:
                  messageId:
                    type: string
                    description: The SQS message ID.
                    example: "bd8359d9-d559-47dd-9467-2a31e88a9e2d"
        400:
          $ref: '#/components/responses/BadRequest'
        403:
          $ref: '#/components/responses/Forbidden'
        500:
          $ref: '#/components/responses/ServerError'
```

6) The `/management/stored-identity/{userId}` endpoint is used for testing. It doesn't take a request body and requires only the userId as path parameter.
Example response:
```
[
 {
    "userId": "userId",
    "recordType": "idrec:gpg45",
    "storedIdentity": "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1cm46dXVpZDo5NjI4OTgxNS0wZTUyLTQ4MDAtOTZkZi0xZmY3ZGU5ODFjZDQiLCJuYW1lIjoiSm9obiBEb2UiLCJpYXQiOjE3NDYwODkxNTEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYnVpbGQuYWNjb3VudC5nb3YudWsiLCJhdWQiOiJodHRwczovL29yY2guc3R1YnMuYWNjb3VudC5nb3YudWsiLCJuYmYiOiIxNzc3NjI1MTUxIiwidm90IjoiUDIiLCJjcmVkZW50aWFscyI6WyJleUowZVhBaU9pSktWMVFpTENKaGJHY2lPaUpGVXpJMU5pSjkuZXlKemRXSWlPaUoxY200NmRYVnBaRG81TmpJNE9UZ3hOUzB3WlRVeUxUUTRNREF0T1Raa1ppMHhabVkzWkdVNU9ERmpaRFFpTENKaGRXUWlPaUpvZEhSd2N6b3ZMMmxrWlc1MGFYUjVMbUoxYVd4a0xtRmpZMjkxYm5RdVoyOTJMblZySWl3aWJtSm1Jam94TnpRMk1Ea3lOVGszTENKcGMzTWlPaUpvZEhSd2N6b3ZMMkZrWkhKbGMzTXRZM0pwTG5OMGRXSnpMbUZqWTI5MWJuUXVaMjkyTG5Wcklpd2lkbU1pT25zaWRIbHdaU0k2V3lKV1pYSnBabWxoWW14bFEzSmxaR1Z1ZEdsaGJDSXNJa0ZrWkhKbGMzTkRjbVZrWlc1MGFXRnNJbDBzSW1OeVpXUmxiblJwWVd4VGRXSnFaV04wSWpwN0ltRmtaSEpsYzNNaU9sdDdJbUZrWkhKbGMzTkRiM1Z1ZEhKNUlqb2lSMElpTENKaWRXbHNaR2x1WjA1aGJXVWlPaUlpTENKemRISmxaWFJPWVcxbElqb2lTRUZFVEVWWklGSlBRVVFpTENKd2IzTjBZV3hEYjJSbElqb2lRa0V5SURWQlFTSXNJbUoxYVd4a2FXNW5UblZ0WW1WeUlqb2lPQ0lzSW1Ga1pISmxjM05NYjJOaGJHbDBlU0k2SWtKQlZFZ2lMQ0oyWVd4cFpFWnliMjBpT2lJeU1EQXdMVEF4TFRBeEluMWRmWDBzSW1wMGFTSTZJblZ5YmpwMWRXbGtPbU13TlRWbFlXVmpMVEF5WmpVdE5EUTFOQzA1TnpreUxUWXlZemxqTldRM1l6QXdOeUo5LnhkSHlaVUV3d2k2VENpTTM4VXlOZEgtYkhkQjE0QnhtNm0xVWNuWE5SR1Z4cXFHR0R1cWgwODdsTDNtT0ZNd1BFVWxkTEU2TmVKOUI4dFZkSHJrUnlBIl0sImNsYWltcyI6W3siZXhwaXJ5RGF0ZSI6IjIwMzAtMDEtMDEiLCJpY2FvSXNzdWVyQ29kZSI6IkdCUiIsImRvY3VtZW50TnVtYmVyIjoiMzIxNjU0OTg3In0seyJuYW1lIjpbeyJuYW1lUGFydHMiOlt7InR5cGUiOiJHaXZlbk5hbWUiLCJ2YWx1ZSI6IktFTk5FVEgifSx7InR5cGUiOiJGYW1pbHlOYW1lIiwidmFsdWUiOiJERUNFUlFVRUlSQSJ9XX1dLCJiaXJ0aERhdGUiOlt7InZhbHVlIjoiMTk2NS0wNy0wOCJ9XX1dfQ.DjY3mL3f1U1ehHILXz0ifAosUBCLH3HdAnqYX9YH4t7JdQjSECU885RJKUKZqE33vMvG0n-Ip1tULP7hkQ0R_A", # pragma: allowlist secret
    "levelOfConfidence": "P2",
    "isValid": "true"
 },
 {
    "userId": "userId",
    "recordType": "idrec:Inherited:hmrc",
    "storedIdentity": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1cm46dXVpZDo5NjI4OTgxNS0wZTUyLTQ4MDAtOTZkZi0xZmY3ZGU5ODFjZDQiLCJuYW1lIjoiSm9obiBEb2UiLCJpYXQiOjE3NDYwODkxNTEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYnVpbGQuYWNjb3VudC5nb3YudWsiLCJhdWQiOiJodHRwczovL29yY2guc3R1YnMuYWNjb3VudC5nb3YudWsiLCJuYmYiOiIxNzc3NjI1MTUxIiwidm90IjoiUENMMjUwIiwiY3JlZGVudGlhbHMiOlsiZXlKMGVYQWlPaUpLVjFRaUxDSmhiR2NpT2lKRlV6STFOaUo5LmV5SnpkV0lpT2lKMWNtNDZkWFZwWkRvNU5qSTRPVGd4TlMwd1pUVXlMVFE0TURBdE9UWmtaaTB4Wm1ZM1pHVTVPREZqWkRRaUxDSmhkV1FpT2lKb2RIUndjem92TDJsa1pXNTBhWFI1TG1KMWFXeGtMbUZqWTI5MWJuUXVaMjkyTG5Wcklpd2libUptSWpveE56UTJNRGt5TlRrM0xDSnBjM01pT2lKb2RIUndjem92TDJGa1pISmxjM010WTNKcExuTjBkV0p6TG1GalkyOTFiblF1WjI5MkxuVnJJaXdpZG1NaU9uc2lkSGx3WlNJNld5SldaWEpwWm1saFlteGxRM0psWkdWdWRHbGhiQ0lzSWtGa1pISmxjM05EY21Wa1pXNTBhV0ZzSWwwc0ltTnlaV1JsYm5ScFlXeFRkV0pxWldOMElqcDdJbUZrWkhKbGMzTWlPbHQ3SW1Ga1pISmxjM05EYjNWdWRISjVJam9pUjBJaUxDSmlkV2xzWkdsdVowNWhiV1VpT2lJaUxDSnpkSEpsWlhST1lXMWxJam9pU0VGRVRFVlpJRkpQUVVRaUxDSndiM04wWVd4RGIyUmxJam9pUWtFeUlEVkJRU0lzSW1KMWFXeGthVzVuVG5WdFltVnlJam9pT0NJc0ltRmtaSEpsYzNOTWIyTmhiR2wwZVNJNklrSkJWRWdpTENKMllXeHBaRVp5YjIwaU9pSXlNREF3TFRBeExUQXhJbjFkZlgwc0ltcDBhU0k2SW5WeWJqcDFkV2xrT21Nd05UVmxZV1ZqTFRBeVpqVXRORFExTkMwNU56a3lMVFl5WXpsak5XUTNZekF3TnlKOS54ZEh5WlVFd3dpNlRDaU0zOFV5TmRILWJIZEIxNEJ4bTZtMVVjblhOUkdWeHFxR0dEdXFoMDg3bEwzbU9GTXdQRVVsZExFNk5lSjlCOHRWZEhya1J5QSJdLCJjbGFpbXMiOlt7ImV4cGlyeURhdGUiOiIyMDMwLTAxLTAxIiwiaWNhb0lzc3VlckNvZGUiOiJHQlIiLCJkb2N1bWVudE51bWJlciI6IjMyMTY1NDk4NyJ9LHsibmFtZSI6W3sibmFtZVBhcnRzIjpbeyJ0eXBlIjoiR2l2ZW5OYW1lIiwidmFsdWUiOiJLRU5ORVRIIn0seyJ0eXBlIjoiRmFtaWx5TmFtZSIsInZhbHVlIjoiREVDRVJRVUVJUkEifV19XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5NjUtMDctMDgifV19XX0.jxVtrbdCPhJaVLjpSRms2hZTq5bgfsF0yqkz-Q34fZc", # pragma: allowlist secret
    "levelOfConfidence": "PCL250",
    "isValid": "true"
 }
]
```
