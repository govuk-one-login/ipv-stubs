package uk.gov.di.ipv.stub.cred.service;

import com.nimbusds.oauth2.sdk.AuthorizationCode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthCodeService {
    private final Map<String, String> authCodes;

    public AuthCodeService() {
        this.authCodes = new ConcurrentHashMap<>();
    }

    public void persist(AuthorizationCode authCode, String resourcePayload) {
        this.authCodes.put(authCode.getValue(), resourcePayload);
    }

    public String getPayload(String authCode) {
        return this.authCodes.get(authCode);
    }

    public void revoke(String authCode) {
        this.authCodes.remove(authCode);
    }
}
