package uk.gov.di.ipv.stub.cred.validation;

import com.nimbusds.oauth2.sdk.ErrorObject;
import spark.QueryParamsMap;
import uk.gov.di.ipv.stub.cred.config.ClientConfig;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.config.CriType;
import uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Validator {

    private static final String INVALID_GPG45_SCORE_ERROR_CODE = "1001";
    private static final String INVALID_EVIDENCE_VALUES_ERROR_CODE = "1002";
    private static final String INVALID_ACTIVITY_VALUES_ERROR_CODE = "1003";
    private static final String INVALID_FRAUD_VALUES_ERROR_CODE = "1004";
    private static final String INVALID_VERIFICATION_VALUES_ERROR_CODE = "1005";

    private static final String REDIRECT_URI_SEPARATOR = ",";

    private Validator() {}

    public static boolean isNullBlankOrEmpty(String value) {
        return Objects.isNull(value) || value.isEmpty() || value.isBlank();
    }

    public static ValidationResult verifyGpg45(CriType criType, String strengthValue, String validityValue, String activityValue, String fraudValue, String verificationValue) {
        switch (criType) {
            case EVIDENCE_CRI_TYPE:
                try {
                    Integer.parseInt(strengthValue);
                    Integer.parseInt(validityValue);

                    return areStringsNullOrEmpty(Arrays.asList(activityValue, fraudValue, verificationValue));
                } catch (NumberFormatException e) {
                    return new ValidationResult(false, new ErrorObject(INVALID_EVIDENCE_VALUES_ERROR_CODE, "Invalid numbers provided for evidence strength and validity"));
                }
            case ACTIVITY_CRI_TYPE:
                try {
                    Integer.parseInt(activityValue);

                    return areStringsNullOrEmpty(Arrays.asList(strengthValue, validityValue, fraudValue, verificationValue));
                } catch (NumberFormatException e) {
                    return new ValidationResult(false, new ErrorObject(INVALID_ACTIVITY_VALUES_ERROR_CODE, "Invalid number provided for activity"));
                }
            case FRAUD_CRI_TYPE:
                try {
                    Integer.parseInt(fraudValue);

                    return areStringsNullOrEmpty(Arrays.asList(strengthValue, validityValue, activityValue, verificationValue));
                } catch (NumberFormatException e) {
                    return new ValidationResult(false, new ErrorObject(INVALID_FRAUD_VALUES_ERROR_CODE, "Invalid number provided for fraud"));
                }
            case VERIFICATION_CRI_TYPE:
                try {
                    Integer.parseInt(verificationValue);

                    return areStringsNullOrEmpty(Arrays.asList(strengthValue, validityValue, activityValue, fraudValue));
                } catch (NumberFormatException e) {
                    return new ValidationResult(false, new ErrorObject(INVALID_VERIFICATION_VALUES_ERROR_CODE, "Invalid number provided for verification"));
                }
            default:
                return ValidationResult.createValidResult();
        }
    }

    public static boolean redirectUrlIsInvalid(QueryParamsMap queryParams) {
        String redirectUri = queryParams.value(RequestParamConstants.REDIRECT_URI);
        String clientId = queryParams.value(RequestParamConstants.CLIENT_ID);

        ClientConfig clientConfig = CredentialIssuerConfig.getClientConfig(clientId);
        List<String> validRedirectUrls = Arrays.asList(clientConfig.getJwtAuthentication().get("validRedirectUrls").split(REDIRECT_URI_SEPARATOR));
        return !validRedirectUrls.contains(redirectUri);
    }

    private static ValidationResult areStringsNullOrEmpty(List<String> values) {
        for (String value: values) {
            if (!isNullBlankOrEmpty(value)) {
                return new ValidationResult(false, new ErrorObject(INVALID_GPG45_SCORE_ERROR_CODE, "Invalid GPG45 score provided"));
            }
        }
        return ValidationResult.createValidResult();
    }
}
