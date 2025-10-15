package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.service.RequestedErrorResponseService;
import uk.gov.di.ipv.stub.cred.service.TokenService;

public class F2FHandler extends CredentialHandler {

    public F2FHandler(
            CredentialService credentialService,
            TokenService tokenService,
            RequestedErrorResponseService requestedErrorResponseService) {
        super(credentialService, tokenService, requestedErrorResponseService);
    }

    @Override
    protected void sendResponse(Context ctx, String verifiableCredential) throws Exception {
        String subject = SignedJWT.parse(verifiableCredential).getJWTClaimsSet().getSubject();

        var userInfo = new UserInfo(new Subject(subject));
        userInfo.setClaim("https://vocab.account.gov.uk/v1/credentialStatus", "pending");

        ctx.status(HttpStatus.ACCEPTED);
        ctx.json(userInfo.toJSONObject());
    }
}
