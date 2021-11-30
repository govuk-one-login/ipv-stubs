# Digital Identity IPV Credential Issuer Stub
`di-ipv-credential-issuer-stub`

An Credential Issuer stub using Spark Java

## About

The stub application to simulate the credential issuers to be used during the IPV journeys.

## Environment

Variable | Description | Example Value
--- | --- | --- |
CREDENTIAL_ISSUER_PORT     | The port number the credential issuer should run on | `8084` |

## Running locally

To run locally, you can run the startup script.
```shell
./startup.sh
```
This will first build the project with gradle,
and after it will attempt to run it.

Once started, visit the authorize endpoint with the required query params
 [http://localhost:8084/authorize?response_type=code&client_id=test&redirect_uri=http://example.com](http://localhost:8084/authorize?response_type=code&client_id=test&redirect_uri=http://example.com).

By clicking on the _Generate Auth Code_ button you should be redirected to the given redirect_uri with an authentication code as a query param.
