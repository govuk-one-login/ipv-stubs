package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import org.eclipse.jetty.http.HttpHeader;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.service.TokenService;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;

import javax.servlet.http.HttpServletResponse;

public class F2FHandler {

    private static final String JSON_RESPONSE_TYPE = "application/json;charset=UTF-8";
    private TokenService tokenService;
    private CredentialService credentialService;

    public F2FHandler(CredentialService credentialService, TokenService tokenService) {
        this.credentialService = credentialService;
        this.tokenService = tokenService;
    }

    public Route getResource =
            (Request request, Response response) -> {
                String accessTokenString = request.headers(HttpHeader.AUTHORIZATION.toString());
                ValidationResult validationResult =
                        tokenService.validateAccessToken(accessTokenString);

                if (!validationResult.isValid()) {
                    response.status(validationResult.getError().getHTTPStatusCode());
                    return validationResult.getError().getDescription();
                }

                String resourceId = tokenService.getPayload(accessTokenString);
                String credentialSignedJwt = credentialService.getCredentialSignedJwt(resourceId);
                String subject =
                        SignedJWT.parse(credentialSignedJwt).getJWTClaimsSet().getSubject();

                tokenService.revoke(accessTokenString);

                response.type(JSON_RESPONSE_TYPE);
                response.status(HttpServletResponse.SC_ACCEPTED);

                var userInfo = new UserInfo(new Subject(subject));
                userInfo.setClaim("https://vocab.account.gov.uk/v1/credentialStatus", "pending");

                return userInfo.toJSONString();
            };
}
