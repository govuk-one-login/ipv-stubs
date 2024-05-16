### EVCS Stub
This will set up an API gateway in front of a lambda.
(ref. - https://govukverify.atlassian.net/wiki/spaces/AB/pages/4083089473/Draft+Proposal+-+API+Spec)

1) The format of the POST (/vcs/<user-id>) request
Save multiple VCs for a specific user
```

```
Response
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
```

```
Response
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
```

```
Response
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

