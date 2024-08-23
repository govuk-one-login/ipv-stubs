package uk.gov.di.ipv.stub.cred.domain;

public interface AuthRequest extends DecryptJarRequest {
    String credentialSubjectJson();

    String evidenceJson();

    RequestedError requestedError();

    Long nbf();

    Mitigations mitigations();

    F2fDetails f2f();
}
