package uk.gov.di.ipv.stub.cred.validation;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValidatorTest {
    @Test
    void shouldReturnFalseWhenNullBlankOrEmptyStringProvided() {
        String[] testCases = new String[] { null, "", "  " };
        Arrays.stream(testCases).forEach(testCase -> assertTrue(Validator.isNullBlankOrEmpty(testCase)));
    }

    @Test
    void shouldReturnTrueWhenNonNullBlankOrEmptyStringProvided() {
        assertFalse(Validator.isNullBlankOrEmpty("Test value"));
    }
}
