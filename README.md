# Digital Identity IPV Stubs
`di-ipv-stubs`

This is the home for application stubs used be the Identity Proofing and Verification (IPV) system within the GDS digital identity platform, GOV.UK OneLogin.

## Orchestrator Stub
`di-ipv-orchestrator-stub` [/di-ipv-orchestrator-stub](/di-ipv-orchestrator-stub)

Owned by the IPV Core team, this is the starting point for manual testing and demonstrating IPV journeys.

The Orchestrator Stub allows the user to select desired attributes then initiate an OAuth user journey with the IPV system. The users will be redirected through IPV Core to complete the IPV process and returned to the Orchestrator Stub.

On completion of the user journey the Orchestrator Stub receives an authorisation code which it will exchange for an access token. The stub will then use this token to access the protected resource from IPV Core. Finally, the Orchestrator Stub will display the contents of the protected resource.

By default, the orchestrator stub is protected by HTTP basic authentication. The username and password are configured in SSM under `/stubs/<environment>/orch/env/ORCHESTRATOR_BASIC_AUTH_USERNAME` and`/stubs/<environment>/orch/env/ORCHESTRATOR_BASIC_AUTH_PASSOWRD`.
To turn off HTTP basic authentication override the `OrchestratorBasicAuthEnable` deployment template parameter with the value `false` (note that the SSM username and password value must still exist even when HTTP basic authentication is disabled or deployments will fail.)

## Credential Issuer Stubs
`di-ipv-credential-issuer-stub` [/di-ipv-credential-issuer-stub](/di-ipv-credential-issuer-stub)

The Credential Issuer Stubs can be used as an endpoint for testing Credential Issuer OAuth flows from the IPV Core system. It provides an `/authorize`, `/token` and `/credentials/issue` endpoint to access the protected resource.

These stubs are owned by the IPV Core team.

## DCMAW-Async stub
`di-ipv-dcmaw-async-stub` [/di-ipv-dcmaw-async-stub](https://github.com/govuk-one-login/ipv-stubs/tree/main/di-ipv-dcmaw-async-stub)

This is the stub for the V2 app. It doesn't need a user interface like the old DCMAW CRI stub as interactions with this CRI are different so is in its own directory outside of `di-ipv-credential-issuer-stub`.
To test a full user journey with this stub, see [here](https://govukverify.atlassian.net/wiki/spaces/DID/pages/5448237347/How+to+go+through+a+v2+App+MAM+Journey+in+a+dev+environment) for instructions on running the full user flow, including priming the stub to return a VC.

This stub is owned by the IPV Core team.

## TICF Stub

`di-ipv-ticf-stub` [/di-ipv-ticf-stub](https://github.com/govuk-one-login/ipv-stubs/tree/main/di-ipv-ticf-stub)

This stubs out the Threat Intelligence and Counter Fraud (TICF) system for IPV Core. TICF is called at the end of every user journey as an added form of risk-assessment (see [here](https://team-manual.account.gov.uk/teams/IPV-Core-team/How-IPV-Core-Works/understanding-ticf-issuer/) for more details).

This stub is owned by the IPV Core team.

## CIMIT Stub
`di-ipv-cimit-stub` [/di-ipv-cimit-stub](https://github.com/govuk-one-login/ipv-stubs/tree/main/di-ipv-cimit-stub)

This is the stub for the Contra-Indicators and Mitigations (CIMIT) system which IPV Core uses to record contra-indicators (CIs) and mitigations against a user during the IPV process.

This stub is owned by the IPV Core team.

## EVCS Stub
`di-ipv-evcs-stub` [/di-ipv-evcs-stub](https://github.com/govuk-one-login/ipv-stubs/tree/main/di-ipv-evcs-stub)

The Encrypted Verifiable Credential Store (EVCS) is where VCs attained by a user during their IPV journey is persisted for long-term store.
For more information on how IPV Core uses this service, see the [team manual doc](https://team-manual.account.gov.uk/teams/IPV-Core-team/How-IPV-Core-Works/understanding-vc-storage/#encrypted-vc-storage-evcs).

This stubs out that service and is owned by the IPV Core and Trust and Reuse teams.

## AIS Stub
`di-ipv-ais-stub` [/di-ipv-ais-stub](https://github.com/govuk-one-login/ipv-stubs/tree/main/di-ipv-ais-stub)

This is a stub for the Account Intervention Service (AIS) which receives signals that indicate suspicious activity with a user and their IPV journey. IPV Core integrates with this service
at the beginning and end of a user journey and routes the user accordingly depending on the interventions returned by AIS.

This stub is owned by the IPV Core team.

## SIS Stub
`di-ipv-sis-stub` [/di-sis-stub](https://github.com/govuk-one-login/ipv-stubs/tree/main/di-ipv-sis-stub)

The Stored Identity Service (SIS) returns the stored identity for a given user. IPV Core temporarily integrates with this service in order to check that the stored identities created by IPV Core and stored in EVCS are accurate and representative of the VCs a user holds.

This stub is owned by the IPV Core team.

## Queue Stub

`di-ipv-queue-stub` [/di-ipv-queue-stub](https://github.com/govuk-one-login/ipv-stubs/tree/main/di-ipv-queue-stub)

This defines the lambda which pushes messages onto anSQS queue. This is currently used for testing F2F asynchronous VC returns.

This stub is owned by the IPV Core team.

## IPV Core Stubs
`di-ipv-core-stub` [/di-ipv-core-stub](https://github.com/govuk-one-login/ipv-stubs/tree/main/di-ipv-core-stub)

This stubs out the IPV Core system for the Orange team (responsible for Address and Experian KBV CRIs) and Lime team (responsible for Fraud, Driving Licence and Passport CRIs).

## Pre-Commit Checking / Verification

There is a `.pre-commit-config.yaml` configuration setup in this repo, this uses [pre-commit](https://pre-commit.com/) to verify your commit before actually committing, it runs the following checks:

- Check Json files for formatting issues
- Fixes end of file issues (it will auto correct if it spots an issue - you will need to run the git commit again after it has fixed the issue)
- It automatically removes trailing whitespaces (again will need to run commit again after it detects and fixes the issue)
- Detects aws credentials or private keys accidentally added to the repo
- runs cloud formation linter and detects issues
- runs checkov and checks for any issues
- runs detect-secrets to check for secrets accidentally added - where these are false positives, the `.secrets.baseline` file should be updated by running `detect-secrets scan > .secrets.baseline`

### Dependency Installation

To use this locally you will first need to install the dependencies, this can be done in 2 ways:

#### Method 1 - Python pip

Run the following in a terminal:

```
sudo -H pip3 install checkov pre-commit cfn-lint
```

this should work across platforms

#### Method 2 - Brew

If you have brew installed please run the following:

```
brew install pre-commit ;\
brew install cfn-lint ;\
brew install checkov
```

### Post Installation Configuration

once installed run:

```
pre-commit install
```

To update the various versions of the pre-commit plugins, this can be done by running:

```
pre-commit autoupdate && pre-commit install
```

This will install / configure the pre-commit git hooks, if it detects an issue while committing it will produce an output like the following:

```
 git commit -a
check json...........................................(no files to check)Skipped
fix end of files.........................................................Passed
trim trailing whitespace.................................................Passed
detect aws credentials...................................................Passed
detect private key.......................................................Passed
AWS CloudFormation Linter................................................Failed
- hook id: cfn-python-lint
- exit code: 4
W3011 Both UpdateReplacePolicy and DeletionPolicy are needed to protect Resources/PublicHostedZone from deletion
core/deploy/dns-zones/template.yaml:20:3
Checkov..............................................(no files to check)Skipped
- hook id: checkov
```

## Testing against a stub deployed to dev
To test dev deployed changes made on a stub with the rest of IPV Core, the IPV Core config
needs to be updated to point at these dev-deployed stubs instead of those in prod.

### Using the dev-deploy tool
The [dev-deploy](https://github.com/govuk-one-login/ipv-core-common-infra/tree/main/utils/dev-deploy) tool can be used to
update the credential issuer configs automatically using the `--use-dev` or `-ud` option:
```
dev-deploy update -u <user> -s <service> -ud <dev-stub>
```
This automatically updates the API invoke URLs for the specified CRI stub to use the custom dev domains as well as
api keys, if required (for more info on this option see [here](https://github.com/govuk-one-login/ipv-core-common-infra/blob/main/utils/dev-deploy/docs/cli-userguide.md#update)).
Other parts of the ipv core config can still be overridden by updating the user dev-deploy config (see below).

## Manually updating the user dev-deploy config
The user dev-deploy config can be manually updated with the dev-specific invoke URLs as well as other parameters
to override the specified parameters in the [main dev configs](https://github.com/govuk-one-login/ipv-core-common-infra/tree/main/utils/config-mgmt/app/configs).

#### 1. Finding the dev-specific invoke URLs
These can be obtained by:
1. Getting the invoke URL from AWS ApiGateway
2. Finding the domain mapping in the stub's deploy template
3. Looking for the appropriate URL in AWS ApiGateway under "Custom domain names"

It's important to use the custom domain name when testing a public-facing API (see [here](#403-errors-when-testing-a-public-facing-api)).

#### 2. Update your user dev-deploy config
Update your user dev-deploy config found in [core-common-infra](https://github.com/govuk-one-login/ipv-core-common-infra/tree/main/utils/dev-deploy/configs/user-deployments),
making sure to place the new config in the same way as the main deployment config. For example, pointing to your
dev-deployed TICF stub should look something like this:
```
parameters:
    secrets-manager:
       dev/core/credentialIssuers/ticf/connections/stub/apiKey: "my-dev-ticf-api-key" # pragma: allowlist secret
    ssm:
       core/credentialIssuers/ticf/connections/stub: '{
                "credentialUrl": "https://ticf-dev-theab.02.core.dev.stubs.account.gov.uk/risk-assessment",
                "signingKey": "the-signing-key",
                "componentId": "https://ticf.stubs.account.gov.uk",
                "requiresApiKey": "true", # pragma: allowlist secret
                "requestTimeout": 5
              }'
```
This will override the specified configs in the main deployment configs.
If testing TICF, CIMIT or another api which requires an api key, remember to override these too.

#### 403 errors when testing a public-facing api
When working on a public-facing api and testing requests sent from a VPC e.g. those from core-back, if the invoke URL
does not use a custom domain name, these requests will return a 403 error. This is because, instead of being routed through the internet
these requests are incorrectly routed through the VPC endpoint which can only send requests to private APIs. See [here](https://repost.aws/knowledge-center/api-gateway-vpc-connections)
for further explanation and guidance on fixing the issue.