package uk.gov.di.ipv.stub.cred.domain;

import java.util.Map;

public record Credential(
        Map<String, Object> credentialSubject,
        Map<String, Object> evidence,
        String userId,
        String clientId,
        Long nbf) {}
