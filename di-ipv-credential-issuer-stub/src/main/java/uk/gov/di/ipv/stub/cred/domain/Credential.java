package uk.gov.di.ipv.stub.cred.domain;

import java.util.Map;

public class Credential {
    private final Map<String, Object> attributes;
    private final Map<String, Object> evidence;

    public Credential(Map<String, Object> attributes, Map<String, Object> evidence) {
        this.attributes = attributes;
        this.evidence = evidence;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Map<String, Object> getEvidence() {
        return evidence;
    }
}
