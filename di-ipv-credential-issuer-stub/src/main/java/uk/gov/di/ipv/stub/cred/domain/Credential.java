package uk.gov.di.ipv.stub.cred.domain;

import java.util.Map;

public class Credential {
    private final Map<String, Object> attributes;
    private final Map<String, Object> evidence;
    private final String userId;
    private final String clientId;
    private final Long nbf;

    public Credential(
            Map<String, Object> attributes,
            Map<String, Object> evidence,
            String userId,
            String clientId,
            Long nbf) {
        this.attributes = attributes;
        this.evidence = evidence;
        this.userId = userId;
        this.clientId = clientId;
        this.nbf = nbf;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Map<String, Object> getEvidence() {
        return evidence;
    }

    public String getUserId() {
        return userId;
    }

    public String getClientId() {
        return clientId;
    }

    public Long getNbf() {
        return nbf;
    }
}
