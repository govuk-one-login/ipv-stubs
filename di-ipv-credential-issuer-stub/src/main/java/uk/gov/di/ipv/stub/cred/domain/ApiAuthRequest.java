package uk.gov.di.ipv.stub.cred.domain;

public record ApiAuthRequest(
        String clientId,
        String request,
        String resourceId,
        String credentialSubjectJson,
        String evidenceJson,
        Mitigations mitigations,
        F2fDetails f2f,
        Long nbf,
        RequestedError requestedError)
        implements AuthRequest {}
