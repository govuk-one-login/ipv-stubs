package uk.gov.di.ipv.stub.cred.domain;

public record ApiAuthRequest(
        String clientId,
        String request,
        String resourceId,
        String credentialSubjectJson,
        String evidenceJson,
        Mitigations mitigations,
        boolean sendF2fVcToQueue,
        boolean sendF2fErrorToQueue,
        String f2fQueueName,
        Long nbf,
        String errorEndpoint,
        String error,
        String errorDescription,
        String userInfoError)
        implements AuthRequest {}
