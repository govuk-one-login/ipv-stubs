package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import io.javalin.http.Context;
import io.javalin.http.Header;
import io.javalin.http.HttpStatus;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.service.RequestedErrorResponseService;
import uk.gov.di.ipv.stub.cred.service.TokenService;

import java.util.List;
import java.util.UUID;

public class DocAppCredentialHandler extends CredentialHandler {
    private final RequestedErrorResponseService requestedErrorResponseService;

    public DocAppCredentialHandler(
            CredentialService credentialService,
            TokenService tokenService,
            RequestedErrorResponseService requestedErrorResponseService) {
        super(credentialService, tokenService);
        this.requestedErrorResponseService = requestedErrorResponseService;
    }

    @Override
    protected void sendResponse(Context ctx, String verifiableCredential) throws Exception {
        var accessTokenHeaderValue = ctx.header(Header.AUTHORIZATION);

        String accessTokenValue = BearerAccessToken.parse(accessTokenHeaderValue).getValue();
        UserInfoErrorResponse requestedUserInfoErrorResponse =
                requestedErrorResponseService.getUserInfoErrorByToken(accessTokenValue);
        if (requestedUserInfoErrorResponse != null) {
            ctx.status(requestedUserInfoErrorResponse.getErrorObject().getHTTPStatusCode());
            ctx.json(requestedUserInfoErrorResponse.getErrorObject().toJSONObject());
            return;
        }

        var userInfo = new UserInfo(new Subject("urn:fdc:gov.uk:2022:" + UUID.randomUUID()));
        userInfo.setClaim(
                "https://vocab.account.gov.uk/v1/credentialJWT", List.of(verifiableCredential));

        ctx.status(HttpStatus.CREATED);
        ctx.json(userInfo.toJSONObject());
    }
}
