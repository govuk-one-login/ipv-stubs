package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import org.eclipse.jetty.http.HttpHeader;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.cred.domain.Credential;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.service.TokenService;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;
import uk.gov.di.ipv.stub.cred.validation.Validator;
import uk.gov.di.ipv.stub.cred.vc.VerifiableCredentialGenerator;

import javax.servlet.http.HttpServletResponse;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;

public class F2FHandler {

    private static final String JSON_RESPONSE_TYPE = "application/json;charset=UTF-8";
    private TokenService tokenService;
    private CredentialService credentialService;
    private VerifiableCredentialGenerator verifiableCredentialGenerator;

    public F2FHandler(
            CredentialService credentialService,
            TokenService tokenService,
            VerifiableCredentialGenerator verifiableCredentialGenerator) {
        this.credentialService = credentialService;
        this.tokenService = tokenService;
        this.verifiableCredentialGenerator = verifiableCredentialGenerator;
    }

    public Route getResource =
            (Request request, Response response) -> {
                String accessTokenString = request.headers(HttpHeader.AUTHORIZATION.toString());
                ValidationResult validationResult = validateAccessToken(accessTokenString);

                if (!validationResult.isValid()) {
                    response.status(validationResult.getError().getHTTPStatusCode());
                    return validationResult.getError().getDescription();
                }

                String resourceId = tokenService.getPayload(accessTokenString);
                Credential credential = credentialService.getCredential(resourceId);

                String verifiableCredential;
                try {
                    verifiableCredential =
                            verifiableCredentialGenerator.generate(credential).serialize();
                } catch (NoSuchAlgorithmException | InvalidKeySpecException | JOSEException e) {
                    response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return String.format("Error: Unable to generate VC - '%s'", e.getMessage());
                }

                tokenService.revoke(accessTokenString);

                response.type(JSON_RESPONSE_TYPE);
                response.status(HttpServletResponse.SC_ACCEPTED);

                String subject =
                        SignedJWT.parse(verifiableCredential).getJWTClaimsSet().getSubject();
                var userInfo = new UserInfo(new Subject(subject));
                userInfo.setClaim("https://vocab.account.gov.uk/v1/credentialStatus", "pending");

                return userInfo.toJSONString();
            };

    private ValidationResult validateAccessToken(String accessTokenString) {
        if (Validator.isNullBlankOrEmpty(accessTokenString)) {
            return new ValidationResult(false, OAuth2Error.INVALID_REQUEST);
        }

        if (Objects.isNull(this.tokenService.getPayload(accessTokenString))) {
            return new ValidationResult(false, OAuth2Error.INVALID_CLIENT);
        }

        try {
            AccessToken.parse(accessTokenString);
        } catch (ParseException e) {
            return new ValidationResult(false, OAuth2Error.INVALID_REQUEST);
        }

        return ValidationResult.createValidResult();
    }
}
