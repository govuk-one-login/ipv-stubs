package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import org.eclipse.jetty.http.HttpHeader;
import spark.Request;
import spark.Response;
import spark.Route;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.service.RequestedErrorResponseService;
import uk.gov.di.ipv.stub.cred.service.TokenService;
import uk.gov.di.ipv.stub.cred.validation.ValidationResult;

import javax.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.UUID;

public class DocAppCredentialHandler {

    private static final String JSON_RESPONSE_TYPE = "application/json;charset=UTF-8";
    private CredentialService credentialService;
    private TokenService tokenService;

    private RequestedErrorResponseService requestedErrorResponseService;

    public DocAppCredentialHandler(
            CredentialService credentialService,
            TokenService tokenService,
            RequestedErrorResponseService requestedErrorResponseService) {
        this.credentialService = credentialService;
        this.tokenService = tokenService;
        this.requestedErrorResponseService = requestedErrorResponseService;
    }

    public Route getResource =
            (Request request, Response response) -> {
                String accessTokenHeaderValue =
                        request.headers(HttpHeader.AUTHORIZATION.toString());

                ValidationResult validationResult =
                        tokenService.validateAccessToken(accessTokenHeaderValue);

                if (!validationResult.isValid()) {
                    response.status(validationResult.getError().getHTTPStatusCode());
                    return validationResult.getError().getDescription();
                }

                String accessTokenValue =
                        BearerAccessToken.parse(accessTokenHeaderValue).getValue();
                UserInfoErrorResponse requestedUserInfoErrorResponse =
                        requestedErrorResponseService.getUserInfoErrorByToken(accessTokenValue);
                if (requestedUserInfoErrorResponse != null) {
                    response.status(
                            requestedUserInfoErrorResponse.getErrorObject().getHTTPStatusCode());
                    return requestedUserInfoErrorResponse
                            .getErrorObject()
                            .toJSONObject()
                            .toJSONString();
                }

                String resourceId = tokenService.getPayload(accessTokenHeaderValue);
                String verifiableCredential = credentialService.getCredentialSignedJwt(resourceId);

                tokenService.revoke(accessTokenHeaderValue);

                response.type(JSON_RESPONSE_TYPE);
                response.status(HttpServletResponse.SC_CREATED);

                var userInfo =
                        new UserInfo(new Subject("urn:fdc:gov.uk:2022:" + UUID.randomUUID()));
                userInfo.setClaim(
                        "https://vocab.account.gov.uk/v1/credentialJWT",
                        List.of(verifiableCredential));

                return userInfo.toJSONString();
            };
}
