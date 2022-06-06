package uk.gov.di.ipv.stub.orc.exceptions;

import com.nimbusds.oauth2.sdk.ErrorObject;

public class OauthException extends Exception {
    private final ErrorObject errorObject;

    public OauthException(ErrorObject errorObject) {
        this.errorObject = errorObject;
    }

    public ErrorObject getErrorObject() {
        return errorObject;
    }
}
