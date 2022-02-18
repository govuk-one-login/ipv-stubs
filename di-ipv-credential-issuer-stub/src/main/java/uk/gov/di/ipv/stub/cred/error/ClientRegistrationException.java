package uk.gov.di.ipv.stub.cred.error;

public class ClientRegistrationException extends RuntimeException {
    public ClientRegistrationException(String message, Throwable e) {
        super(message, e);
    }
}
