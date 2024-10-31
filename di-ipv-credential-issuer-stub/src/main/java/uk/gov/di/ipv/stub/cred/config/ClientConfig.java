package uk.gov.di.ipv.stub.cred.config;

import lombok.Builder;

@Builder
public class ClientConfig {
    private String signingPublicJwk;
    private JwtAuthenticationConfig jwtAuthentication;
    private String audienceForVcJwt;

    public String getSigningPublicJwk() {
        return signingPublicJwk;
    }

    public void setSigningPublicJwk(String signingPublicJwk) {
        this.signingPublicJwk = signingPublicJwk;
    }

    public JwtAuthenticationConfig getJwtAuthentication() {
        return jwtAuthentication;
    }

    public void setJwtAuthentication(JwtAuthenticationConfig jwtAuthentication) {
        this.jwtAuthentication = jwtAuthentication;
    }

    public String getAudienceForVcJwt() {
        return audienceForVcJwt;
    }

    public void setAudienceForVcJwt(String audienceForVcJwt) {
        this.audienceForVcJwt = audienceForVcJwt;
    }
}
