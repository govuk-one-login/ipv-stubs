package uk.gov.di.ipv.stub.cred.config;

import java.util.Map;

public class ClientConfig {
    private String signingPublicJwk;
    private Map<String, String> jwtAuthentication;

    public String getSigningPublicJwk() {
        return signingPublicJwk;
    }

    public Map<String, String> getJwtAuthentication() {
        return jwtAuthentication;
    }

    public void setSigningPublicJwk(String signingPublicJwk) {
        this.signingPublicJwk = signingPublicJwk;
    }

    public void setJwtAuthentication(Map<String, String> jwtAuthentication) {
        this.jwtAuthentication = jwtAuthentication;
    }
}
