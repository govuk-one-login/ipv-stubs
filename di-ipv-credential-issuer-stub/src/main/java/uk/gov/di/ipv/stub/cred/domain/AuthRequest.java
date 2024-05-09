package uk.gov.di.ipv.stub.cred.domain;

import java.util.List;

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

    List<String> mitigatedCi();

    String cimitStubUrl();

    String cimitStubApiKey();

    boolean sendF2fVcToQueue();

    boolean sendF2fErrorToQueue();

    String f2fQueueName();
}
