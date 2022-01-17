# Digital Identity IPV Credential Issuer Stub
`di-ipv-credential-issuer-stub`

A Credential Issuer stub using Spark Java

## About

The stub application to simulate the credential issuers to be used during the IPV journeys.

## Environment

| Variable                | Description                                           | Example Value          |
|-------------------------|-------------------------------------------------------|------------------------|
| CREDENTIAL_ISSUER_PORT  | The port number the credential issuer should run on   | `8084`                 |
| CREDENTIAL_ISSUER_NAME  | The name of the credential issuer, displayed to users | `UK Passport CRI Stub` |
| CREDENTIAL_ISSUER_TYPE  | The name of the credential issuer                     | `CRI`                  |
| CLIENT_CONFIG           | Configuration of accepted clients                     | `{"foo":"bar"}`        |


## Running locally

To run locally, you can run the startup script.
```shell
./startup.sh
```
This will first build the project with gradle,
and after it will attempt to run it.

Once started, visit the `/authorize` endpoint with the required query params
 [http://localhost:8084/authorize?response_type=code&client_id=test&redirect_uri=http://example.com](http://localhost:8084/authorize?response_type=code&client_id=test&redirect_uri=http://example.com).

By clicking on the `Generate Auth Code` button you should be redirected to the given redirect_uri with an authentication code as a query param.

### Remote Debugging
A remote JVM debugger can be attached on port `5006`
