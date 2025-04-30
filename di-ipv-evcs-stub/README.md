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
    "jwt": "zzJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjJhNjkzNjFkLTAzOTctNGU4OS04ZmFlLTI4YjFjMmZlZDYxNCJ9.eyJzdWIiOiJ1cm46ZmRjOmdvdi51azoyMDIyOkpHMFJKSTFwWWJuYW5idlBzLWo0ajUtYS1QRmNtaHJ5OVF1OU5DRXA1ZDQiLCJuYmYiOjE2NzAzMzY0NDEsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuYWNjb3VudC5nb3YudWsvIiwidm90IjoiUDIiLCJleHAiOjE2ODI5NTkwMzEsImlhdCI6MTY4Mjk1ODczMSwidnRtIjoiaHR0cHM6Ly9vaWRjLmFjY291bnQuZ292LnVrL3RydXN0bWFyayIsInZjIjp7InR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlSWRlbnRpdHlDcmVkZW50aWFsIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJKYW5lIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJXcmlnaHQiLCJ0eXBlIjoiRmFtaWx5TmFtZSJ9XSwidmFsaWRGcm9tIjoiMjAxOS0wNC0wMSJ9LHsibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IkphbmUiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IldyaWdodCIsInR5cGUiOiJGYW1pbHlOYW1lIn1dLCJ2YWxpZFVudGlsIjoiMjAxOS0wNC0wMSJ9XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5ODktMDctMDYifV19fSwiYXVkIjoiaXB2QXVkaWVuY2UifQ.zf0yp7B1an7cEwBui7GFCF9NNCJhHxTZuMSh5ehZPmZ4J527okK3pRgdSpWX8DlBFiZS-rXA496egfcfI-neGQ", # pragma: allowlist secret
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

