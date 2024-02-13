package uk.gov.di.ipv.stub.cred.config;

import lombok.Builder;

import java.util.List;

@Builder
public class JwtAuthenticationConfig {
    private String signingPublicJwk;
    private List<String> validRedirectUrls;
    private String authenticationMethod;

    public JwtAuthenticationConfig(
            String signingPublicJwk, List<String> validRedirectUrls, String authenticationMethod) {
        this.signingPublicJwk = signingPublicJwk;
        this.validRedirectUrls = validRedirectUrls;
        this.authenticationMethod = authenticationMethod;
    }

    public String getSigningPublicJwk() {
        return signingPublicJwk;
    }

    public List<String> getValidRedirectUrls() {
        return validRedirectUrls;
    }

    public String getAuthenticationMethod() {
        return authenticationMethod;
    }
}
