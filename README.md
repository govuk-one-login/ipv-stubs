# Digital Identity IPV Stubs
`di-ipv-stubs`

This is the home for application stubs used be the Identity Proofing and Verification (IPV) system within the GDS digital identity platform, GOV.UK Sign In.

## Orchestrator Stub
`di-ipv-orchestrator-stub` [/di-ipv-orchestrator-stub](/di-ipv-orchestrator-stub)

Starting point for manual testing and demonstrating IPV journeys.

The Orchestrator Stub allows the user to select desired attributes then initiate an OAuth user journey with the IPV system. The users will be redirected to the IPV system to complete the IPV process and returned to the Orchestrator Stub.

On completion of the user journey the Orchestrator Stub receives an authorisation code which it will exchange for an access token and in turn use to access the protected resource in the IPV system. Finally the Orchestrator Stub will display the contents of the protected resource.

By default the orchestrator stub is protected by HTTP basic authentication. The username and password are configured in SSM under `/stubs/<environment>/orch/env/ORCHESTRATOR_BASIC_AUTH_USERNAME` and`/stubs/<environment>/orch/env/ORCHESTRATOR_BASIC_AUTH_PASSOWRD`.
To turn off HTTP basic authentication override the `OrchestratorBasicAuthEnable` deployment template parameter with the value `false` (note that the SSM username and password value must still exist even when HTTP basic authentication is disabled or deployments will fail.)

## Credential Issuer Stub
`di-ipv-credential-issuer-stub` [/di-ipv-credential-issuer-stub](/di-ipv-credential-issuer-stub)

The Credential Issuer Stub can be used as an endpoint for testing Credential Issuer OAuth flows from the core IPV system. It provides an '/authorize' user endpoint, token exchange and access to a protected resource.

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
