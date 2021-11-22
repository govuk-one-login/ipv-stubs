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

Once started, visit [http://localhost:8084/authorize](http://localhost:8084/authorize).
By clicking on the _Generate Auth Code_ button...
