package uk.gov.di.ipv.stub.cred.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import spark.QueryParamsMap;
import uk.gov.di.ipv.stub.cred.config.CriType;
import uk.gov.di.ipv.stub.cred.fixtures.TestFixtures;
import uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SystemStubsExtension.class)
class ValidatorTest {

    @SystemStub
    private final EnvironmentVariables environmentVariables = new EnvironmentVariables("CLIENT_CONFIG", TestFixtures.CLIENT_CONFIG);;

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
    void shouldReturnSuccessfulValidationOnValidEvidenceGpg() {
        ValidationResult result = Validator.verifyGpg45(CriType.EVIDENCE_CRI_TYPE, "1", "3", null, null, null);
        assertTrue(result.isValid());
    }

    @Test
    void shouldReturnFailedValidationIfInvalidNumberParamProvidedInEvidence() {
        ValidationResult result = Validator.verifyGpg45(CriType.EVIDENCE_CRI_TYPE, "abc", "/&43", null, null, null);
        assertFalse(result.isValid());
        assertEquals("Invalid numbers provided for evidence strength and validity", result.getError().getDescription());
    }

    @Test
    void shouldReturnFailedValidationIfScoresProvidedForInvalidCriTypeOnEvidence() {
        ValidationResult result = Validator.verifyGpg45(CriType.EVIDENCE_CRI_TYPE, "1", "2", null, "4", null);
        assertFalse(result.isValid());
        assertEquals("Invalid GPG45 score provided", result.getError().getDescription());
    }

    @Test
    void shouldReturnSuccessfulValidationOnValidActivityGpg() {
        ValidationResult result = Validator.verifyGpg45(CriType.ACTIVITY_CRI_TYPE, null, null, "1", null, null);
        assertTrue(result.isValid());
    }

    @Test
    void shouldReturnFailedValidationIfInvalidNumberParamProvidedInActivity() {
        ValidationResult result = Validator.verifyGpg45(CriType.ACTIVITY_CRI_TYPE, null, null, "abc", null, null);
        assertFalse(result.isValid());
        assertEquals("Invalid number provided for activity", result.getError().getDescription());
    }

    @Test
    void shouldReturnFailedValidationIfScoresProvidedForInvalidCriTypeOnActivity() {
        ValidationResult result = Validator.verifyGpg45(CriType.ACTIVITY_CRI_TYPE, "1", "2", "1", null, null);
        assertFalse(result.isValid());
        assertEquals("Invalid GPG45 score provided", result.getError().getDescription());
    }

    @Test
    void shouldReturnSuccessfulValidationOnValidFraudGpg() {
        ValidationResult result = Validator.verifyGpg45(CriType.FRAUD_CRI_TYPE, null, null, null, "1", null);
        assertTrue(result.isValid());
    }

    @Test
    void shouldReturnFailedValidationIfInvalidNumberParamProvidedInFraud() {
        ValidationResult result = Validator.verifyGpg45(CriType.FRAUD_CRI_TYPE, null, null, null, "abc", null);
        assertFalse(result.isValid());
        assertEquals("Invalid number provided for fraud", result.getError().getDescription());
    }

    @Test
    void shouldReturnFailedValidationIfScoresProvidedForInvalidCriTypeOnFraud() {
        ValidationResult result = Validator.verifyGpg45(CriType.FRAUD_CRI_TYPE, "1", "2", null, "1", null);
        assertFalse(result.isValid());
        assertEquals("Invalid GPG45 score provided", result.getError().getDescription());
    }

    @Test
    void shouldReturnSuccessfulValidationOnValidVerificationGpg() {
        ValidationResult result = Validator.verifyGpg45(CriType.VERIFICATION_CRI_TYPE, null, null, null, null, "1");
        assertTrue(result.isValid());
    }

    @Test
    void shouldReturnFailedValidationIfInvalidNumberParamProvidedInVerfication() {
        ValidationResult result = Validator.verifyGpg45(CriType.VERIFICATION_CRI_TYPE, null, null, null, null, "abc");
        assertFalse(result.isValid());
        assertEquals("Invalid number provided for verification", result.getError().getDescription());
    }

    @Test
    void shouldReturnFailedValidationIfScoresProvidedForInvalidCriTypeOnVerification() {
        ValidationResult result = Validator.verifyGpg45(CriType.VERIFICATION_CRI_TYPE, "1", "2", null, null, "1");
        assertFalse(result.isValid());
        assertEquals("Invalid GPG45 score provided", result.getError().getDescription());
    }

    @Test
    void redirectUrlIsInvalidShouldReturnFalseForValidUrlWithOnlyOneRegistered() {
        QueryParamsMap queryParams = mock(QueryParamsMap.class);
        when(queryParams.value(RequestParamConstants.REDIRECT_URI)).thenReturn("https://valid.example.com");
        when(queryParams.value(RequestParamConstants.CLIENT_ID)).thenReturn("clientIdValid");

        assertFalse(Validator.redirectUrlIsInvalid(queryParams));
    }

    @Test
    void redirectUrlIsInvalidShouldReturnFalseForValidUrlWithMultipleRegistered() {
        QueryParamsMap queryParams = mock(QueryParamsMap.class);
        when(queryParams.value(RequestParamConstants.REDIRECT_URI)).thenReturn("https://valid3.example.com");
        when(queryParams.value(RequestParamConstants.CLIENT_ID)).thenReturn("clientIdValidMultipleUri");

        assertFalse(Validator.redirectUrlIsInvalid(queryParams));
    }

    @Test
    void redirectUrlIsInvalidShouldReturnTrueForUnregisteredUrl() {
        QueryParamsMap queryParams = mock(QueryParamsMap.class);
        when(queryParams.value(RequestParamConstants.REDIRECT_URI)).thenReturn("https://example.com");
        when(queryParams.value(RequestParamConstants.CLIENT_ID)).thenReturn("clientIdNonRegistered");

        assertTrue(Validator.redirectUrlIsInvalid(queryParams));
    }
}
