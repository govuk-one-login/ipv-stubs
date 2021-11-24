package uk.gov.di.ipv.stub.cred.validation;

public enum ErrorMessageKeys {
    MISSING_REDIRECT_URI("missing_redirect_uri"),
    MISSING_RESPONSE_TYPE("missing_response_type"),
    INVALID_RESPONSE_TYPE("invalid_response_type"),
    MISSING_CLIENT_ID("missing_client_id");

    private final String key;

    ErrorMessageKeys(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }
}
