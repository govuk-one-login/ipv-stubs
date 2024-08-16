package uk.gov.di.ipv.stub.cred.service;

import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;
import uk.gov.di.ipv.stub.cred.validation.Validator;

import java.util.Map;
import java.util.Objects;
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

    public ValidationResult validateAccessToken(String accessTokenString) {
        if (Validator.isNullBlankOrEmpty(accessTokenString)) {
            return new ValidationResult(false, OAuth2Error.INVALID_REQUEST);
        }

        if (Objects.isNull(this.getPayload(accessTokenString))) {
            return new ValidationResult(false, OAuth2Error.INVALID_CLIENT);
        }

        try {
            BearerAccessToken.parse(accessTokenString);
        } catch (ParseException e) {
            return new ValidationResult(false, OAuth2Error.INVALID_REQUEST);
        }

        return ValidationResult.createValidResult();
    }
}
