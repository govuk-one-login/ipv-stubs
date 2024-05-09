package uk.gov.di.ipv.stub.cred.domain;

public interface AuthRequest {
    String clientId();

    String request();

    String resourceId();

    String credentialSubjectJson();

    String evidenceJson();

    String error();

    String errorDescription();

    String errorEndpoint();

    String userInfoError();

    Long nbf();

    Mitigations mitigations();

    F2fDetails f2f();
}
