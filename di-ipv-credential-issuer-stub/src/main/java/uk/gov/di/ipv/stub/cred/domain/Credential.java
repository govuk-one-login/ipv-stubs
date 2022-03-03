package uk.gov.di.ipv.stub.cred.domain;

import java.util.Map;

public class Credential {
    private final Map<String, Object> attributes;
    private final Map<String, Object> gpg45Score;

    public Credential(Map<String, Object> attributes, Map<String, Object> gpg45Score) {
        this.attributes = attributes;
        this.gpg45Score = gpg45Score;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Map<String, Object> getGpg45Score() {
        return gpg45Score;
    }
}
