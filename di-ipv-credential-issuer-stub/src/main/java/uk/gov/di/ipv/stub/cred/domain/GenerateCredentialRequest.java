package uk.gov.di.ipv.stub.cred.domain;

public record GenerateCredentialRequest(
        String userId,
        String clientId,
        String credentialSubjectJson,
        String evidenceJson,
        Long nbf) {}
