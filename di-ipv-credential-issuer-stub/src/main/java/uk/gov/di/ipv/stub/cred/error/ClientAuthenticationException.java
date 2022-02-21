package uk.gov.di.ipv.stub.cred.error;

public class ClientAuthenticationException extends Exception {
    public ClientAuthenticationException(String message) {
        super(message);
    }

    public ClientAuthenticationException(Throwable e) {
        super(e);
    }
}
