package uk.gov.di.ipv.stub.cred.service;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenService {

    private static final long DEFAULT_ACCESS_TOKEN_TTL_IN_SECS = 3600;
    private final Map<String, String> accessTokens;

    public TokenService() {
        this.accessTokens = new ConcurrentHashMap<>();
    }

    public void persist(AccessToken accessToken, String resourcePayload) {
        this.accessTokens.put(accessToken.toAuthorizationHeader(), resourcePayload);
    }

    public String getPayload(String authorizationHeaderValue) {
        return this.accessTokens.get(authorizationHeaderValue);
    }

    public void revoke(String authorizationHeaderValue) {
        this.accessTokens.remove(authorizationHeaderValue);
    }

    public AccessToken createBearerAccessToken() {
        return new BearerAccessToken(DEFAULT_ACCESS_TOKEN_TTL_IN_SECS, null);
    }
}
