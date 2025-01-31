package uk.gov.di.ipv.stub.cred.validation;

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.stub.cred.config.ClientConfig;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.config.CriType;
import uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants;
import uk.gov.di.ipv.stub.cred.service.AuthCodeService;
import uk.gov.di.ipv.stub.cred.service.ConfigService;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Validator.class);
    private static final String LOCALHOST_DOMAIN = "localhost";
    private static final String INVALID_GPG45_SCORE_ERROR_CODE = "1001";
    private static final String INVALID_EVIDENCE_VALUES_ERROR_CODE = "1002";
    private static final String INVALID_ACTIVITY_VALUES_ERROR_CODE = "1003";
    private static final String INVALID_FRAUD_VALUES_ERROR_CODE = "1004";
    private static final String INVALID_VERIFICATION_VALUES_ERROR_CODE = "1005";
    private static final String INVALID_EVIDENCE_DRIVING_LICENCE_VALUES_ERROR_CODE = "1006";
    private static final String INVALID_DOC_CHECK_APP_VALUES_ERROR_CODE = "1007";
    private static final String INVALID_F2F_VALUES_ERROR_CODE = "1008";

    public static final String API_GATEWAY_CALLBACK_SUFFIX =
            "execute-api.eu-west-2.amazonaws.com/credential-issuer/callback";

    private final AuthCodeService authCodeService;

    public Validator(AuthCodeService authCodeService) {
        this.authCodeService = authCodeService;
    }

    public static boolean isNullBlankOrEmpty(String value) {
        return Objects.isNull(value) || value.isEmpty() || value.isBlank();
    }

    public static boolean isNullBlankOrEmpty(Object value) {
        return Objects.isNull(value) || value.toString().isEmpty() || value.toString().isBlank();
    }

    public static ValidationResult verifyGpg45(
            CriType criType,
            String strengthValue,
            String validityValue,
            String activityValue,
            String fraudValue,
            String verificationValue) {
        switch (criType) {
            case EVIDENCE_CRI_TYPE:
                try {
                    Integer.parseInt(strengthValue);
                    Integer.parseInt(validityValue);

                    return areStringsNullOrEmpty(
                            Arrays.asList(activityValue, fraudValue, verificationValue));
                } catch (NumberFormatException e) {
                    return new ValidationResult(
                            false,
                            new ErrorObject(
                                    INVALID_EVIDENCE_VALUES_ERROR_CODE,
                                    "Invalid numbers provided for evidence strength and validity"));
                }
            case EVIDENCE_DRIVING_LICENCE_CRI_TYPE:
                try {
                    Integer.parseInt(strengthValue);
                    Integer.parseInt(validityValue);
                    Integer.parseInt(activityValue);

                    return areStringsNullOrEmpty(Arrays.asList(fraudValue, verificationValue));
                } catch (NumberFormatException e) {
                    return new ValidationResult(
                            false,
                            new ErrorObject(
                                    INVALID_EVIDENCE_DRIVING_LICENCE_VALUES_ERROR_CODE,
                                    "Invalid numbers provided for evidence strength, validity and activity"));
                }
            case DOC_CHECK_APP_CRI_TYPE:
                try {
                    Integer.parseInt(strengthValue);
                    Integer.parseInt(validityValue);
                    Integer.parseInt(activityValue);

                    return areStringsNullOrEmpty(Arrays.asList(fraudValue));
                } catch (NumberFormatException e) {
                    return new ValidationResult(
                            false,
                            new ErrorObject(
                                    INVALID_DOC_CHECK_APP_VALUES_ERROR_CODE,
                                    "Invalid numbers provided for evidence strength, validity and activity"));
                }
            case F2F_CRI_TYPE:
                try {
                    Integer.parseInt(strengthValue);
                    Integer.parseInt(validityValue);
                    Integer.parseInt(verificationValue);

                    return areStringsNullOrEmpty(Arrays.asList(activityValue, fraudValue));
                } catch (NumberFormatException e) {
                    return new ValidationResult(
                            false,
                            new ErrorObject(
                                    INVALID_F2F_VALUES_ERROR_CODE,
                                    "Invalid numbers provided for evidence strength, validity and verification"));
                }
            case ACTIVITY_CRI_TYPE:
                try {
                    Integer.parseInt(activityValue);

                    return areStringsNullOrEmpty(
                            Arrays.asList(
                                    strengthValue, validityValue, fraudValue, verificationValue));
                } catch (NumberFormatException e) {
                    return new ValidationResult(
                            false,
                            new ErrorObject(
                                    INVALID_ACTIVITY_VALUES_ERROR_CODE,
                                    "Invalid number provided for activity"));
                }
            case FRAUD_CRI_TYPE:
                try {
                    Integer.parseInt(fraudValue);

                    return areStringsNullOrEmpty(
                            Arrays.asList(strengthValue, validityValue, verificationValue));
                } catch (NumberFormatException e) {
                    return new ValidationResult(
                            false,
                            new ErrorObject(
                                    INVALID_FRAUD_VALUES_ERROR_CODE,
                                    "Invalid number provided for fraud"));
                }
            case VERIFICATION_CRI_TYPE:
                try {
                    Integer.parseInt(verificationValue);

                    return areStringsNullOrEmpty(
                            Arrays.asList(strengthValue, validityValue, activityValue, fraudValue));
                } catch (NumberFormatException e) {
                    return new ValidationResult(
                            false,
                            new ErrorObject(
                                    INVALID_VERIFICATION_VALUES_ERROR_CODE,
                                    "Invalid number provided for verification"));
                }
            default:
                return ValidationResult.createValidResult();
        }
    }

    public static boolean redirectUrlIsInvalid(String clientId, String redirectUri) {
        ClientConfig clientConfig = ConfigService.getClientConfig(clientId);

        LOGGER.info("Validating client redirect uri: {}", redirectUri);

        if (isRedirectUriLocalhost(redirectUri)
                || isRedirectUriAmazonApiGatewayCallback(redirectUri)
                || isRedirectUriDevDomain(redirectUri)) {
            return false;
        }

        List<String> validRedirectUrls = clientConfig.getJwtAuthentication().getValidRedirectUrls();

        validRedirectUrls.forEach(url -> LOGGER.info("Configured redirect url: {}", url));
        return !validRedirectUrls.contains(redirectUri);
    }

    private static boolean isRedirectUriLocalhost(String redirectUri) {
        return redirectUri != null && redirectUri.contains(LOCALHOST_DOMAIN);
    }

    private static boolean isRedirectUriDevDomain(String redirectUri) {
        return redirectUri != null && redirectUri.contains(CredentialIssuerConfig.DEV_DOMAIN);
    }

    private static boolean isRedirectUriAmazonApiGatewayCallback(String redirectUri) {
        return redirectUri != null && redirectUri.contains(API_GATEWAY_CALLBACK_SUFFIX);
    }

    public ValidationResult validateRedirectUrlsMatch(
            String redirectUrlFromAuthEndpoint, String redirectUrlFromTokenEndpoint) {
        LOGGER.info("Validating the client redirect url");
        LOGGER.info("Original redirect url from auth endpoint: {}", redirectUrlFromAuthEndpoint);
        LOGGER.info("New redirect url from token endpoint: {}", redirectUrlFromTokenEndpoint);
        if (Validator.isNullBlankOrEmpty(redirectUrlFromAuthEndpoint)
                && Validator.isNullBlankOrEmpty(redirectUrlFromTokenEndpoint)) {
            return ValidationResult.createValidResult();
        }

        if (Validator.isNullBlankOrEmpty(redirectUrlFromAuthEndpoint)) {
            return new ValidationResult(false, OAuth2Error.INVALID_GRANT);
        }

        if (!redirectUrlFromAuthEndpoint.equals(redirectUrlFromTokenEndpoint)) {
            return new ValidationResult(false, OAuth2Error.INVALID_GRANT);
        }

        return ValidationResult.createValidResult();
    }

    public ValidationResult validateTokenRequest(Context ctx) {
        String clientIdValue = ctx.formParam(RequestParamConstants.CLIENT_ID);
        String assertionType = ctx.formParam(RequestParamConstants.CLIENT_ASSERTION_TYPE);
        String assertion = ctx.formParam(RequestParamConstants.CLIENT_ASSERTION);

        if (Validator.isNullBlankOrEmpty(clientIdValue)
                && (Validator.isNullBlankOrEmpty(assertionType)
                        || Validator.isNullBlankOrEmpty(assertion))) {
            LOGGER.error("Missing client id or client assertion param values");
            return new ValidationResult(false, OAuth2Error.INVALID_CLIENT);
        }

        if (!Validator.isNullBlankOrEmpty(clientIdValue)
                && ConfigService.getClientConfig(clientIdValue) == null) {
            LOGGER.error("Failed to find config for provided client id: {}", clientIdValue);
            return new ValidationResult(false, OAuth2Error.INVALID_CLIENT);
        }

        String grantTypeValue = ctx.formParam(RequestParamConstants.GRANT_TYPE);
        if (Validator.isNullBlankOrEmpty(grantTypeValue)
                || !grantTypeValue.equalsIgnoreCase(GrantType.AUTHORIZATION_CODE.getValue())) {
            return new ValidationResult(false, OAuth2Error.UNSUPPORTED_GRANT_TYPE);
        }

        String authCodeValue = ctx.formParam(RequestParamConstants.AUTH_CODE);
        if (Validator.isNullBlankOrEmpty(authCodeValue)) {
            LOGGER.error("Missing authorization code");
            return new ValidationResult(false, OAuth2Error.INVALID_GRANT);
        }
        if (Objects.isNull(this.authCodeService.getPayload(authCodeValue))) {
            LOGGER.error("Invalid authorization code provided");
            return new ValidationResult(false, OAuth2Error.INVALID_GRANT);
        }

        String redirectUriValue = ctx.formParam(RequestParamConstants.REDIRECT_URI);
        if (Validator.isNullBlankOrEmpty(redirectUriValue)) {
            LOGGER.error("Invalid Redirect URI: {}", redirectUriValue);
            return new ValidationResult(false, OAuth2Error.INVALID_REQUEST);
        }

        return ValidationResult.createValidResult();
    }

    private static ValidationResult areStringsNullOrEmpty(List<String> values) {
        for (String value : values) {
            if (!isNullBlankOrEmpty(value)) {
                return new ValidationResult(
                        false,
                        new ErrorObject(
                                INVALID_GPG45_SCORE_ERROR_CODE, "Invalid GPG45 score provided"));
            }
        }
        return ValidationResult.createValidResult();
    }
}
