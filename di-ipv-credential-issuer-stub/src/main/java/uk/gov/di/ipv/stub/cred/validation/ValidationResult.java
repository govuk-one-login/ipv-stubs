package uk.gov.di.ipv.stub.cred.validation;

import com.nimbusds.oauth2.sdk.ErrorObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ValidationResult {
    private final boolean valid;
    private final ErrorObject error;

    public static ValidationResult createValidResult() {
        return new ValidationResult(true, null);
    }
}
