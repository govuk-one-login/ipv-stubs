# Digital Identity IPV Orchestrator Stub
`di-ipv-orchestrator-stub`

An Orchestrator stub using Spark Java

## About

The starting point for manual testing and demonstrating IPV journeys.

## Config

Have a look in `OrchestratorConfig.java` for an up-to-date list of environment variables.
The default values correspond to a local configuration.

## Running locally

To run locally, you can use gradle.

To perform a clean and build:
```shell
./gradlew clean build
```

To run the Orchestrator:
```shell
./gradlew run
```

You can also set up a Gradle run configuration in IntelliJ to run directly within the IDE.

Once started, visit [http://localhost:4500/].
By clicking on the _Prove your identity_ button, you will be redirected
to IPV Core, which will redirect back to the callback URL upon completion.

After, the orchestrator sends some HTTP requests to exchange the code
for a token and to fetch the user info via back-channels.

### Remote Debugging
A remote JVM debugger can be attached on port `5007`
