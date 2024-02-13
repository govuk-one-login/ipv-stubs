# Digital Identity IPV Core Stub

`di-ipv-core-stub`

## About

An integration stub service for testing credential issuer journeys from a Stub IPV Core.

This stub allows us to:

* configure multiple credential issuers
* set the user identity claim expected in the Experian UAT environment (name, date of birth, UKAddress)
* set the user identity claim explicitly in a form (todo)
* send these claims to a credential issuer
* view the credentials returned

## Config

Core Stub config from the `di-ipv-config` repository directory `/stubs/di-ipv-core-stub` will be available at:

* Docker image: `/app/config/`



## Environment

Variable | Description                                              | Example Value
--- |----------------------------------------------------------| --- |
CORE_STUB_PORT             | The port number the IPV Core Stub should run on          | `8085` |
CORE_STUB_CLIENT_ID              | The id of the IPV Core Stub client                       | `ipv-core-stub` |
CORE_STUB_REDIRECT_URL               | The OAuth callback url                                   | `http://localhost:8085/callback` |
CORE_STUB_MAX_SEARCH_RESULTS   | Max search by name results                               | `200` |
CORE_STUB_USER_DATA_PATH  | File path to Experian user data zip file                 | `/app/config/experian-uat-users-large.zip` |
CORE_STUB_CONFIG_FILE  | File path to the credential issuer config                | `/app/config/cris-dev.yaml` for Docker, `config/cris-dev.yaml` for PaaS|
API_KEY_CRI_DEV | API key for a CRI environment, set by hand with `cf` cli ||
API_KEY_CRI_ADDRESS_BUILD | API key for a CRI environment, set by hand with `cf` cli ||
API_KEY_CRI_ADDRESS_STAGING | API key for a CRI environment, set by hand with `cf` cli ||
API_KEY_CRI_ADDRESS_INTEGRATION | API key for a CRI environment, set by hand with `cf` cli ||
API_KEY_CRI_KBV_BUILD | API key for a CRI environment, set by hand with `cf` cli ||
API_KEY_CRI_KBV_STAGING | API key for a CRI environment, set by hand with `cf` cli ||
API_KEY_CRI_KBV_INTEGRATION | API key for a CRI environment, set by hand with `cf` cli ||
API_KEY_CRI_FRAUD_BUILD | API key for a CRI environment, set by hand with `cf` cli ||
API_KEY_CRI_DL_BUILD | API key for a CRI environment, set by hand with `cf` cli ||
API_KEY_CRI_PASSPORTA_BUILD | API key for a CRI environment, set by hand with `cf` cli ||

## Running locally

Update config in the `di-ipv-config` repository file `/di-ipv-core-stub/cris-local-dev.yaml`:
* Update the `tokenUrl` and `credentialUrl` to refer to the relevant API gateway urls. These values should be available as exports from your Address API stack.
* Get the EC signing public key from `https://${AddressApi}.execute-api.${AWS::Region}.amazonaws.com/${Environment}/.well-known/jwks.json`, base64 this JWK, then set as the value for `publicEncryptionJwkBase64`. This value should be available as an export from your Address API stack.

Given a Docker installation on your dev environment, run:
`./startup.sh`

Once started, visit [http://localhost:8085/](http://localhost:8085/).

By clicking on the _Visit Credential Issuers_ button, you will be able to choose which credential issuer to visit.

### Remote Debugging

A remote JVM debugger can be attached on port `8087`
