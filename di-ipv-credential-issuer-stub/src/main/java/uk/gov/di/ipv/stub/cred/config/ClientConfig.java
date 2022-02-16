package uk.gov.di.ipv.stub.cred.config;

import java.util.Map;

public class ClientConfig {
    private String signingCert;
    private Map<String, String> jwtAuthentication;

    public String getSigningCert() {
        return signingCert;
    }

    public Map<String, String> getJwtAuthentication() {
        return jwtAuthentication;
    }
}
