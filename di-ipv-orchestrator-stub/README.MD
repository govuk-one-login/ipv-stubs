# Digital Identity IPV Orchestrator Stub
`di-ipv-orchestrator-stub`

An Orchestrator stub using Spark Java

## About

THe starting point for manual testing and demonstrating IPV journeys.

## Environment

Variable | Description | Example Value
--- | --- | --- |
ORCHESTRATOR_PORT | The port number the orchestrator should run on | `8081` |

## Running locally

To run locally, you can run the startup script.
```shell
./startup.sh
```
This will first build the project with gradle,
and after it will attempt to run it.

Once started, visit [http://localhost:8081/](http://localhost:8081/).
By clicking on the _Prove your identity_ button, you will be redirected
to the IPV engine, which will redirect back to the callback URL.

After, the orchestrator sends some HTTP requests to exchange the code
for a token and to fetch the user info via back-channels.