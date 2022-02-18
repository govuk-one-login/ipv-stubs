package uk.gov.di.ipv.stub.cred.service;

import com.nimbusds.oauth2.sdk.AuthorizationCode;

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
        this.authCodes.put(
                authCode.getValue(),
                Map.of(RESOURCE_PAYLOAD, resourcePayload, REDIRECT_URL, redirectUrl));
    }

    public String getPayload(String authCode) {
        return getAuthCodesEntryAttribute(authCode, RESOURCE_PAYLOAD);
    }

    public String getRedirectUrl(String authCode) {
        return getAuthCodesEntryAttribute(authCode, REDIRECT_URL);
    }

    private String getAuthCodesEntryAttribute(String authCode, String attribute) {
        Map<String, String> maybeAuthCodesEntry = this.authCodes.get(authCode);
        if (!(maybeAuthCodesEntry == null)) {
            return maybeAuthCodesEntry.get(attribute);
        }
        return null;
    }

    public void revoke(String authCode) {
        this.authCodes.remove(authCode);
    }
}
