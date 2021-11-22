package uk.gov.di.ipv.stub.cred.validation;

import java.util.Objects;

public class Validator {

    public static boolean isNullBlankOrEmpty(String value) {
        return Objects.isNull(value) || value.isEmpty() || value.isBlank();
    }
}
