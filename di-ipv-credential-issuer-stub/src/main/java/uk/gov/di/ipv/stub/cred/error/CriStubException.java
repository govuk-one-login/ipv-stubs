package uk.gov.di.ipv.stub.cred.error;

public class CriStubException extends Exception {

    private String description;

    public CriStubException(String message, String description) {
        super(message);
        this.description = description;
    }

    public CriStubException(String message, Throwable cause) {
        super(message, cause);
    }

    public CriStubException(String message, String description, Throwable cause) {
        super(message, cause);
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
