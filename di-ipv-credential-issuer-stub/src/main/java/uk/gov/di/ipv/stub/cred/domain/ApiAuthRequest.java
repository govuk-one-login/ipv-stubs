package uk.gov.di.ipv.stub.cred.domain;

public record ApiAuthRequest(
        String clientId,
        String request,
        String credentialSubjectJson,
        String evidenceJson,
        Long nbf,
        Mitigations mitigations,
        F2fDetails f2f,
        RequestedError requestedError)
        implements AuthRequest {}
