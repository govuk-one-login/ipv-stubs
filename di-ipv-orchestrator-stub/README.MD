# Digital Identity IPV Orchestrator Stub
`di-ipv-orchestrator-stub`

An Orchestrator stub using Spark Java

## About

The starting point for manual testing and demonstrating IPV journeys.

## Environment

| Variable                            | Description                                                 | Example Value                                                 |
|-------------------------------------|-------------------------------------------------------------|---------------------------------------------------------------|
| IPV_ENDPOINT                        | The IPV core-front endpoint                                 | `https://di-ipv-core-front.london.cloudapps.digital/`         |
| IPV_BACKCHANNEL_ENDPOINT            | The IPV core-back endpoint                                  | `https://{aws-stage-id}.execute-api.eu-west-2.amazonaws.com/` |
| IPV_BACKCHANNEL_TOKEN_PATH          | The IPV core-back token endpoint path                       | `/dev/token`                                                  |
| IPV_BACKCHANNEL_USER_IDENTITY_PATH  | The IPV core-back user identity path                        | `/dev/user-identity`                                          |
| ORCHESTRATOR_CLIENT_ID              | The id of the orch stub                                     | `some-client-id`                                              |
| ORCHESTRATOR_REDIRECT_URL           | The orch stub call-back url                                 | `http://localhost:8083/callback`                              |
| ORCHESTRATOR_CLIENT_SIGNING_KEY     | The orch stub private key for its client authentication JWT | `{base64 encoded pkcs8 private key}`                          |
| ORCHESTRATOR_CLIENT_JWT_EXPIRY_MINS | The expiry time in mins for the orch client auth JWT        | `10`                                                          |
| PORT                                | The port number the orchestrator should run on              | `8083`                                                        |


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

### Remote Debugging
A remote JVM debugger can be attached on port `5007`