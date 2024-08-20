package uk.gov.di.ipv.stub.cred.validation;

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.GrantType;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.stub.cred.config.CriType;
import uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants;
import uk.gov.di.ipv.stub.cred.service.AuthCodeService;
import uk.gov.di.ipv.stub.cred.utils.StubSsmClient;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.stub.cred.fixtures.TestFixtures.CLIENT_CONFIG;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SystemStubsExtension.class)
class ValidatorTest {

    @Mock private Context mockContext;
    @Mock private AuthCodeService mockAuthCodeService;

    @SystemStub
    private final EnvironmentVariables environmentVariables =
            new EnvironmentVariables("ENVIRONMENT", "TEST");

    @BeforeAll
    public static void setUp() {
        StubSsmClient.setClientConfigParams(CLIENT_CONFIG);
    }

    @Test
    void shouldReturnFalseWhenNullBlankOrEmptyStringProvided() {
        String[] testCases = new String[] {null, "", "  "};
        Arrays.stream(testCases)
                .forEach(testCase -> assertTrue(Validator.isNullBlankOrEmpty(testCase)));
    }

    @Test
    void shouldReturnTrueWhenNonNullBlankOrEmptyStringProvided() {
        assertFalse(Validator.isNullBlankOrEmpty("Test value"));
    }

    @Test
    void shouldReturnSuccessfulValidationOnValidEvidenceGpg() {
        ValidationResult result =
                Validator.verifyGpg45(CriType.EVIDENCE_CRI_TYPE, "1", "3", null, null, null);
        assertTrue(result.isValid());
    }

    @Test
    void shouldReturnFailedValidationIfInvalidNumberParamProvidedInEvidence() {
        ValidationResult result =
                Validator.verifyGpg45(CriType.EVIDENCE_CRI_TYPE, "abc", "/&43", null, null, null);
        assertFalse(result.isValid());
        assertEquals(
                "Invalid numbers provided for evidence strength and validity",
                result.getError().getDescription());
    }

    @Test
    void shouldReturnFailedValidationIfScoresProvidedForInvalidCriTypeOnEvidence() {
        ValidationResult result =
                Validator.verifyGpg45(CriType.EVIDENCE_CRI_TYPE, "1", "2", null, "4", null);
        assertFalse(result.isValid());
        assertEquals("Invalid GPG45 score provided", result.getError().getDescription());
    }

    @Test
    void shouldReturnSuccessfulValidationOnValidActivityGpg() {
        ValidationResult result =
                Validator.verifyGpg45(CriType.ACTIVITY_CRI_TYPE, null, null, "1", null, null);
        assertTrue(result.isValid());
    }

    @Test
    void shouldReturnFailedValidationIfInvalidNumberParamProvidedInActivity() {
        ValidationResult result =
                Validator.verifyGpg45(CriType.ACTIVITY_CRI_TYPE, null, null, "abc", null, null);
        assertFalse(result.isValid());
        assertEquals("Invalid number provided for activity", result.getError().getDescription());
    }

    @Test
    void shouldReturnFailedValidationIfScoresProvidedForInvalidCriTypeOnActivity() {
        ValidationResult result =
                Validator.verifyGpg45(CriType.ACTIVITY_CRI_TYPE, "1", "2", "1", null, null);
        assertFalse(result.isValid());
        assertEquals("Invalid GPG45 score provided", result.getError().getDescription());
    }

    @Test
    void shouldReturnSuccessfulValidationOnValidFraudGpg() {
        ValidationResult result =
                Validator.verifyGpg45(CriType.FRAUD_CRI_TYPE, null, null, null, "1", null);
        assertTrue(result.isValid());
    }

    @Test
    void shouldReturnSuccessfulValidationOnValidFraudWithActivityGpg() {
        ValidationResult result =
                Validator.verifyGpg45(CriType.FRAUD_CRI_TYPE, null, null, "1", "1", null);
        assertTrue(result.isValid());
    }

    @Test
    void shouldReturnFailedValidationIfInvalidNumberParamProvidedInFraud() {
        ValidationResult result =
                Validator.verifyGpg45(CriType.FRAUD_CRI_TYPE, null, null, null, "abc", null);
        assertFalse(result.isValid());
        assertEquals("Invalid number provided for fraud", result.getError().getDescription());
    }

    @Test
    void shouldReturnFailedValidationIfScoresProvidedForInvalidCriTypeOnFraud() {
        ValidationResult result =
                Validator.verifyGpg45(CriType.FRAUD_CRI_TYPE, "1", "2", null, "1", null);
        assertFalse(result.isValid());
        assertEquals("Invalid GPG45 score provided", result.getError().getDescription());
    }

    @Test
    void shouldReturnSuccessfulValidationOnValidVerificationGpg() {
        ValidationResult result =
                Validator.verifyGpg45(CriType.VERIFICATION_CRI_TYPE, null, null, null, null, "1");
        assertTrue(result.isValid());
    }

    @Test
    void shouldReturnFailedValidationIfInvalidNumberParamProvidedInVerfication() {
        ValidationResult result =
                Validator.verifyGpg45(CriType.VERIFICATION_CRI_TYPE, null, null, null, null, "abc");
        assertFalse(result.isValid());
        assertEquals(
                "Invalid number provided for verification", result.getError().getDescription());
    }

    @Test
    void shouldReturnFailedValidationIfScoresProvidedForInvalidCriTypeOnVerification() {
        ValidationResult result =
                Validator.verifyGpg45(CriType.VERIFICATION_CRI_TYPE, "1", "2", null, null, "1");
        assertFalse(result.isValid());
        assertEquals("Invalid GPG45 score provided", result.getError().getDescription());
    }

    @Test
    void redirectUrlIsInvalidShouldReturnFalseForValidUrlWithOnlyOneRegistered() {
        assertFalse(Validator.redirectUrlIsInvalid("clientIdValid", "https://valid.example.com"));
    }

    @Test
    void redirectUrlValidationShouldAcceptARedirectUriWithAPaaSDomain() {

        assertFalse(Validator.redirectUrlIsInvalid("clientIdValid", "https://valid.example.com"));
    }

    @Test
    void redirectUrlIsInvalidShouldReturnFalseForValidUrlWithMultipleRegistered() {
        assertFalse(
                Validator.redirectUrlIsInvalid(
                        "clientIdValidMultipleUri", "https://valid3.example.com"));
    }

    @Test
    void redirectUrlIsInvalidShouldReturnTrueForUnregisteredUrl() {
        assertTrue(Validator.redirectUrlIsInvalid("clientIdNonRegistered", "https://example.com"));
    }

    @Test
    void validateTokenRequestShouldFailIfNoClientIdAndNoClientAssertion() {
        setupQueryParamsMap(
                Map.of(
                        RequestParamConstants.CLIENT_ID, "",
                        RequestParamConstants.CLIENT_ASSERTION_TYPE, "some-assertion-type",
                        RequestParamConstants.CLIENT_ASSERTION, ""));

        Validator validator = new Validator(mockAuthCodeService);

        ValidationResult validationResult = validator.validateTokenRequest(mockContext);

        ErrorObject validationError = validationResult.getError();
        assertFalse(validationResult.isValid());
        assertEquals("invalid_client", validationError.getCode());
        assertEquals("Client authentication failed", validationError.getDescription());
    }

    @Test
    void validateTokenRequestShouldFailIfNoClientIdAndNoClientAssertionType() {
        setupQueryParamsMap(
                Map.of(
                        RequestParamConstants.CLIENT_ID, "",
                        RequestParamConstants.CLIENT_ASSERTION_TYPE, "",
                        RequestParamConstants.CLIENT_ASSERTION, "a-client-assertion"));

        Validator validator = new Validator(mockAuthCodeService);

        ValidationResult validationResult = validator.validateTokenRequest(mockContext);

        ErrorObject validationError = validationResult.getError();
        assertFalse(validationResult.isValid());
        assertEquals("invalid_client", validationError.getCode());
        assertEquals("Client authentication failed", validationError.getDescription());
    }

    @Test
    void validateTokenRequestShouldFailIfNoClientIdAndNoClientAssertionTypeOrClientAssertion() {
        setupQueryParamsMap(
                Map.of(
                        RequestParamConstants.CLIENT_ID, "",
                        RequestParamConstants.CLIENT_ASSERTION_TYPE, "",
                        RequestParamConstants.CLIENT_ASSERTION, ""));

        Validator validator = new Validator(mockAuthCodeService);

        ValidationResult validationResult = validator.validateTokenRequest(mockContext);

        ErrorObject validationError = validationResult.getError();
        assertFalse(validationResult.isValid());
        assertEquals("invalid_client", validationError.getCode());
        assertEquals("Client authentication failed", validationError.getDescription());
    }

    @Test
    void validateTokenRequestShouldFailIfNoClientIdAndNoClientConfig() {
        setupQueryParamsMap(
                Map.of(
                        RequestParamConstants.CLIENT_ID, "No-config-for-me",
                        RequestParamConstants.CLIENT_ASSERTION_TYPE, "a-client-assertion-type",
                        RequestParamConstants.CLIENT_ASSERTION, "a-client-assertion"));

        Validator validator = new Validator(mockAuthCodeService);

        ValidationResult validationResult = validator.validateTokenRequest(mockContext);

        ErrorObject validationError = validationResult.getError();
        assertFalse(validationResult.isValid());
        assertEquals("invalid_client", validationError.getCode());
        assertEquals("Client authentication failed", validationError.getDescription());
    }

    @Test
    void validateTokenRequestShouldFailIfNoGrantType() {
        setupQueryParamsMap(
                Map.of(
                        RequestParamConstants.CLIENT_ID, "clientIdValid",
                        RequestParamConstants.CLIENT_ASSERTION_TYPE, "a-client-assertion-type",
                        RequestParamConstants.CLIENT_ASSERTION, "a-client-assertion",
                        RequestParamConstants.GRANT_TYPE, ""));

        Validator validator = new Validator(mockAuthCodeService);

        ValidationResult validationResult = validator.validateTokenRequest(mockContext);

        ErrorObject validationError = validationResult.getError();
        assertFalse(validationResult.isValid());
        assertEquals("unsupported_grant_type", validationError.getCode());
        assertEquals("Unsupported grant type", validationError.getDescription());
    }

    @Test
    void validateTokenRequestShouldFailIfNoWrongType() {
        setupQueryParamsMap(
                Map.of(
                        RequestParamConstants.CLIENT_ID, "clientIdValid",
                        RequestParamConstants.CLIENT_ASSERTION_TYPE, "a-client-assertion-type",
                        RequestParamConstants.CLIENT_ASSERTION, "a-client-assertion",
                        RequestParamConstants.GRANT_TYPE, "not-an-auth-code-grant"));

        Validator validator = new Validator(mockAuthCodeService);

        ValidationResult validationResult = validator.validateTokenRequest(mockContext);

        ErrorObject validationError = validationResult.getError();
        assertFalse(validationResult.isValid());
        assertEquals("unsupported_grant_type", validationError.getCode());
        assertEquals("Unsupported grant type", validationError.getDescription());
    }

    @Test
    void validateTokenRequestShouldFailIfNoAuthCode() {
        setupQueryParamsMap(
                Map.of(
                        RequestParamConstants.CLIENT_ID, "clientIdValid",
                        RequestParamConstants.CLIENT_ASSERTION_TYPE, "a-client-assertion-type",
                        RequestParamConstants.CLIENT_ASSERTION, "a-client-assertion",
                        RequestParamConstants.GRANT_TYPE, GrantType.AUTHORIZATION_CODE.getValue(),
                        RequestParamConstants.AUTH_CODE, ""));

        Validator validator = new Validator(mockAuthCodeService);

        ValidationResult validationResult = validator.validateTokenRequest(mockContext);

        ErrorObject validationError = validationResult.getError();
        assertFalse(validationResult.isValid());
        assertEquals("invalid_grant", validationError.getCode());
        assertEquals("Invalid grant", validationError.getDescription());
    }

    @Test
    void validateTokenRequestShouldFailIfNoPayloadAssociatedWithAuthCode() {
        setupQueryParamsMap(
                Map.of(
                        RequestParamConstants.CLIENT_ID, "clientIdValid",
                        RequestParamConstants.CLIENT_ASSERTION_TYPE, "a-client-assertion-type",
                        RequestParamConstants.CLIENT_ASSERTION, "a-client-assertion",
                        RequestParamConstants.GRANT_TYPE, GrantType.AUTHORIZATION_CODE.getValue(),
                        RequestParamConstants.AUTH_CODE, "a-legit-auth-code"));

        when(mockAuthCodeService.getPayload("a-legit-auth-code")).thenReturn(null);
        Validator validator = new Validator(mockAuthCodeService);

        ValidationResult validationResult = validator.validateTokenRequest(mockContext);

        ErrorObject validationError = validationResult.getError();
        assertFalse(validationResult.isValid());
        assertEquals("invalid_grant", validationError.getCode());
        assertEquals("Invalid grant", validationError.getDescription());
    }

    @Test
    void validateTokenRequestShouldFailIfNoRedirectUri() {
        setupQueryParamsMap(
                Map.of(
                        RequestParamConstants.CLIENT_ID, "clientIdValid",
                        RequestParamConstants.CLIENT_ASSERTION_TYPE, "a-client-assertion-type",
                        RequestParamConstants.CLIENT_ASSERTION, "a-client-assertion",
                        RequestParamConstants.GRANT_TYPE, GrantType.AUTHORIZATION_CODE.getValue(),
                        RequestParamConstants.AUTH_CODE, "a-legit-auth-code",
                        RequestParamConstants.REDIRECT_URI, ""));

        when(mockAuthCodeService.getPayload("a-legit-auth-code")).thenReturn("something");
        Validator validator = new Validator(mockAuthCodeService);

        ValidationResult validationResult = validator.validateTokenRequest(mockContext);

        ErrorObject validationError = validationResult.getError();
        assertFalse(validationResult.isValid());
        assertEquals("invalid_request", validationError.getCode());
        assertEquals("Invalid request", validationError.getDescription());
    }

    @Test
    void validateTokenRequestShouldPassForValidParams() {
        setupQueryParamsMap(
                Map.of(
                        RequestParamConstants.CLIENT_ID, "clientIdValid",
                        RequestParamConstants.CLIENT_ASSERTION_TYPE, "a-client-assertion-type",
                        RequestParamConstants.CLIENT_ASSERTION, "a-client-assertion",
                        RequestParamConstants.GRANT_TYPE, GrantType.AUTHORIZATION_CODE.getValue(),
                        RequestParamConstants.AUTH_CODE, "a-legit-auth-code",
                        RequestParamConstants.REDIRECT_URI, "https://example.com"));

        when(mockAuthCodeService.getPayload("a-legit-auth-code")).thenReturn("something");
        Validator validator = new Validator(mockAuthCodeService);

        ValidationResult validationResult = validator.validateTokenRequest(mockContext);

        assertTrue(validationResult.isValid());
    }

    @Test
    void validateRedirectUrlsMatchShouldReturnValidIfTheyMatch() {
        Validator validator = new Validator(mockAuthCodeService);

        ValidationResult validationResult =
                validator.validateRedirectUrlsMatch("https://example.com", "https://example.com");

        assertTrue(validationResult.isValid());
    }

    @Test
    void validateRedirectUrlsMatchShouldReturnValidIfTheyAreBothNull() {
        Validator validator = new Validator(mockAuthCodeService);

        ValidationResult validationResult = validator.validateRedirectUrlsMatch(null, null);

        assertTrue(validationResult.isValid());
    }

    @Test
    void validateRedirectUrlsMatchShouldReturnValidIfTheyAreBothEmptyString() {
        Validator validator = new Validator(mockAuthCodeService);

        ValidationResult validationResult = validator.validateRedirectUrlsMatch("", "");

        assertTrue(validationResult.isValid());
    }

    @Test
    void validateRedirectUrlsMatchShouldReturnNotValidIfTheyAreDifferent() {
        Validator validator = new Validator(mockAuthCodeService);

        ValidationResult validationResult =
                validator.validateRedirectUrlsMatch(
                        "https://example.com", "https://different.example.com");

        ErrorObject validationError = validationResult.getError();
        assertFalse(validationResult.isValid());
        assertEquals("invalid_grant", validationError.getCode());
        assertEquals("Invalid grant", validationError.getDescription());
    }

    @Test
    void validateRedirectUrlsMatchShouldReturnNotValidIfTheFirstValueIsNull() {
        Validator validator = new Validator(mockAuthCodeService);

        ValidationResult validationResult =
                validator.validateRedirectUrlsMatch(null, "https://example.com");

        ErrorObject validationError = validationResult.getError();
        assertFalse(validationResult.isValid());
        assertEquals("invalid_grant", validationError.getCode());
        assertEquals("Invalid grant", validationError.getDescription());
    }

    @Test
    void validateRedirectUrlsMatchShouldReturnNotValidIfTheSecondValueIsNull() {
        Validator validator = new Validator(mockAuthCodeService);

        ValidationResult validationResult =
                validator.validateRedirectUrlsMatch("https://example.com", null);

        ErrorObject validationError = validationResult.getError();
        assertFalse(validationResult.isValid());
        assertEquals("invalid_grant", validationError.getCode());
        assertEquals("Invalid grant", validationError.getDescription());
    }

    private void setupQueryParamsMap(Map<String, String> params) {
        params.forEach((key, value) -> when(mockContext.queryParam(key)).thenReturn(value));
    }
}
