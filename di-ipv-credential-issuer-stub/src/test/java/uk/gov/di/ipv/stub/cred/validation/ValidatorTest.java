package uk.gov.di.ipv.stub.cred.validation;

import org.junit.jupiter.api.Test;
import uk.gov.di.ipv.stub.cred.config.CriType;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorTest {
    @Test
    void shouldReturnFalseWhenNullBlankOrEmptyStringProvided() {
        String[] testCases = new String[] { null, "", "  " };
        Arrays.stream(testCases).forEach(testCase -> assertTrue(Validator.isNullBlankOrEmpty(testCase)));
    }

    @Test
    void shouldReturnTrueWhenNonNullBlankOrEmptyStringProvided() {
        assertFalse(Validator.isNullBlankOrEmpty("Test value"));
    }

    @Test
    void shouldReturnSuccessfulValidationOnValidEvidenceGpg() throws Exception {
        ValidationResult result = Validator.verifyGpg45(CriType.EVIDENCE_CRI_TYPE, "1", "3", null, null, null);
        assertTrue(result.isValid());
    }

    @Test
    void shouldReturnFailedValidationIfInvalidNumberParamProvidedInEvidence() throws Exception {
        ValidationResult result = Validator.verifyGpg45(CriType.EVIDENCE_CRI_TYPE, "abc", "/&43", null, null, null);
        assertFalse(result.isValid());
        assertEquals("Invalid numbers provided for evidence strength and validity", result.getError().getDescription());
    }

    @Test
    void shouldReturnFailedValidationIfScoresProvidedForInvalidCriTypeOnEvidence() throws Exception {
        ValidationResult result = Validator.verifyGpg45(CriType.EVIDENCE_CRI_TYPE, "1", "2", null, "4", null);
        assertFalse(result.isValid());
        assertEquals("Invalid GPG45 score provided", result.getError().getDescription());
    }

    @Test
    void shouldReturnSuccessfulValidationOnValidActivityGpg() throws Exception {
        ValidationResult result = Validator.verifyGpg45(CriType.ACTIVITY_CRI_TYPE, null, null, "1", null, null);
        assertTrue(result.isValid());
    }

    @Test
    void shouldReturnFailedValidationIfInvalidNumberParamProvidedInActivity() throws Exception {
        ValidationResult result = Validator.verifyGpg45(CriType.ACTIVITY_CRI_TYPE, null, null, "abc", null, null);
        assertFalse(result.isValid());
        assertEquals("Invalid number provided for activity", result.getError().getDescription());
    }

    @Test
    void shouldReturnFailedValidationIfScoresProvidedForInvalidCriTypeOnActivity() throws Exception {
        ValidationResult result = Validator.verifyGpg45(CriType.ACTIVITY_CRI_TYPE, "1", "2", "1", null, null);
        assertFalse(result.isValid());
        assertEquals("Invalid GPG45 score provided", result.getError().getDescription());
    }

    @Test
    void shouldReturnSuccessfulValidationOnValidFraudGpg() throws Exception {
        ValidationResult result = Validator.verifyGpg45(CriType.FRAUD_CRI_TYPE, null, null, null, "1", null);
        assertTrue(result.isValid());
    }

    @Test
    void shouldReturnFailedValidationIfInvalidNumberParamProvidedInFraud() throws Exception {
        ValidationResult result = Validator.verifyGpg45(CriType.FRAUD_CRI_TYPE, null, null, null, "abc", null);
        assertFalse(result.isValid());
        assertEquals("Invalid number provided for fraud", result.getError().getDescription());
    }

    @Test
    void shouldReturnFailedValidationIfScoresProvidedForInvalidCriTypeOnFraud() throws Exception {
        ValidationResult result = Validator.verifyGpg45(CriType.FRAUD_CRI_TYPE, "1", "2", null, "1", null);
        assertFalse(result.isValid());
        assertEquals("Invalid GPG45 score provided", result.getError().getDescription());
    }

    @Test
    void shouldReturnSuccessfulValidationOnValidVerificationGpg() throws Exception {
        ValidationResult result = Validator.verifyGpg45(CriType.VERIFICATION_CRI_TYPE, null, null, null, null, "1");
        assertTrue(result.isValid());
    }

    @Test
    void shouldReturnFailedValidationIfInvalidNumberParamProvidedInVerfication() throws Exception {
        ValidationResult result = Validator.verifyGpg45(CriType.VERIFICATION_CRI_TYPE, null, null, null, null, "abc");
        assertFalse(result.isValid());
        assertEquals("Invalid number provided for verification", result.getError().getDescription());
    }

    @Test
    void shouldReturnFailedValidationIfScoresProvidedForInvalidCriTypeOnVerification() throws Exception {
        ValidationResult result = Validator.verifyGpg45(CriType.VERIFICATION_CRI_TYPE, "1", "2", null, null, "1");
        assertFalse(result.isValid());
        assertEquals("Invalid GPG45 score provided", result.getError().getDescription());
    }
}
