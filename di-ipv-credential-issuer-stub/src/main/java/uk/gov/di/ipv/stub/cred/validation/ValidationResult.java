package uk.gov.di.ipv.stub.cred.validation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ValidationResult {
    private final boolean valid;
    private final String errorCode;
    private final String errorDescription;

    public ValidationResult(boolean valid) {
        this.valid = valid;
        this.errorCode = null;
        this.errorDescription = null;
    }
}
