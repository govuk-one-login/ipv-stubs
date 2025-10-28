# Digital Identity IPV Credential Issuer Stub

`di-ipv-credential-issuer-stub`

Test labeller



A Credential Issuer stub using Spark Java

## About

The stub application to simulate the credential issuers to be used during the IPV journeys.

## Environment

| Variable                | Description                                           | Example Value          |
|-------------------------|-------------------------------------------------------|------------------------|
| CREDENTIAL_ISSUER_PORT  | The port number the credential issuer should run on   | `8084`                 |
| CREDENTIAL_ISSUER_NAME  | The name of the credential issuer, displayed to users | `UK Passport CRI Stub` |
| CLIENT_CONFIG           | Configuration of accepted clients                     | See [test fixtures](https://github.com/alphagov/di-ipv-stubs/blob/main/di-ipv-credential-issuer-stub/src/test/java/uk/gov/di/ipv/stub/cred/fixtures/TestFixtures.java#L6) for example values                       |
| CREDENTIAL_ISSUER_TYPE  | The type of the credential issuer                     | `EVIDENCE`             |
| CLIENT_AUDIENCE         | Hostname that the credential issue should run on      | `http://localhost:8084` |
| VC_ISSUER               | Hostname of the VC issuer                             | `http://localhost:8084` |
| VC_SIGNING_KEY          | Signing key for the VC issuer | See [test fixtures](https://github.com/alphagov/di-ipv-stubs/blob/main/di-ipv-credential-issuer-stub/src/test/java/uk/gov/di/ipv/stub/cred/fixtures/TestFixtures.java#L6) for example values      |

## Development

### Secrets
To set up secrets, copy the `secrets.gradle.template` to `secrets.gradle` and enter values for each variable.

The DI data-vocab classes are published to a GitHub maven repository (https://github.com/govuk-one-login/data-vocab/packages)
and you require a personal access token to download these.

See [Managing your personal access tokens](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)
for instructions on creating a token. The token must have the `read:packages` permission.

## Running locally

### Startup script

To run locally, you can run the startup script.

```shell
./startup.sh
```

This will first build the project with gradle,
and after it will attempt to run it. Java 17 is required.

### IntelliJ Run Configuration

To run locally, you can configure an IntelliJ run configuration. Ensure that Java 17 is installed and [IntelliJ has this defined as a SDK](https://www.jetbrains.com/help/idea/sdk.html#define-sdk). The `di-ipv-credential-issuer-stub` should be opened as the project root.

1. New Run/Build configuration -> New Application
2. Select Java 17
3. Set module as `di-ipv-credential-issuer-stub.main`
4. Select main class as `uk.gov.di.ipv.stub.cred.App`
5. Set [Environment Variables](#environment) as a semi-colon seperated list

### Accessing the Stub

Once started via either method, visit the `/authorize` endpoint with the required query params:
http://localhost:8084/authorize?client_id=ipv-core&request=replace-with-auth-JWT

An auth JWT can be generated using [OAuth stub client](https://github.com/alphagov/di-stub-oauth-client/tree/wip-build-authorize-jar-url) or by contacting a team member for an example JWT.

### Remote Debugging

A remote JVM debugger can be attached on port `5006`
