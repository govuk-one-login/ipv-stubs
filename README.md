Do the checks work?
# Digital Identity IPV Stubs
`di-ipv-stubs`

This is the home for application stubs used be the Identity Proofing and Verification (IPV) system within the GDS digital identity platform, GOV.UK Sign In.

## Orchestrator Stub
`di-ipv-orchestrator-stub` [/di-ipv-orchestrator-stub](/di-ipv-orchestrator-stub)

Starting point for manual testing and demonstrating IPV journeys.

The Orchestrator Stub allows the user to select desired attributes then initiate an OAuth user journey with the IPV system. The users will be redirected to the IPV system to complete the IPV process and returned to the Orchestrator Stub.

On completion of the user journey the Orchestrator Stub receives an authorisation code which it will exchange for an access token and in turn use to access the protected resource in the IPV system. Finally the Orchestrator Stub will display the contents of the protected resource.

## Credential Issuer Stub
`di-ipv-credential-issuer-stub` [/di-ipv-credential-issuer-stub](/di-ipv-credential-issuer-stub)

The Credential Issuer Stub can be used as an endpoint for testing Credential Issuer OAuth flows from the core IPV system. It provides an '/authorize' user endpoint, token exchange and access to a protected resource.

