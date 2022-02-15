package uk.gov.di.ipv.stub.cred.service;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import uk.gov.di.ipv.stub.cred.error.CriStubException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthCodeService {
    private static final String RESOURCE_PAYLOAD = "resourcePayload";
    public static final String REDIRECT_URL = "redirectUrl";
    private final Map<String, Map<String, String>> authCodes;

    public AuthCodeService() {
        this.authCodes = new ConcurrentHashMap<>();
    }

    public void persist(AuthorizationCode authCode, String resourcePayload, String redirectUrl) {
        this.authCodes.put(authCode.getValue(), Map.of(RESOURCE_PAYLOAD, resourcePayload, REDIRECT_URL, redirectUrl));
    }

    public String getPayload(String authCode) {
        try {
            return this.authCodes.get(authCode).get(RESOURCE_PAYLOAD);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public String getRedirectUrl(String authCode) {
        try {
            return this.authCodes.get(authCode).get(REDIRECT_URL);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public void revoke(String authCode) {
        this.authCodes.remove(authCode);
    }
}
