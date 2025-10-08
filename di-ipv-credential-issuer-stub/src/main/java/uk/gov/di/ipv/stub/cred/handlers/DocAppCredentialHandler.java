package uk.gov.di.ipv.stub.cred.handlers;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import uk.gov.di.ipv.stub.cred.service.CredentialService;
import uk.gov.di.ipv.stub.cred.service.RequestedErrorResponseService;
import uk.gov.di.ipv.stub.cred.service.TokenService;

import java.util.List;

public class DocAppCredentialHandler extends CredentialHandler {

    public DocAppCredentialHandler(
            CredentialService credentialService,
            TokenService tokenService,
            RequestedErrorResponseService requestedErrorResponseService) {
        super(credentialService, tokenService, requestedErrorResponseService);
    }

    @Override
    protected void sendResponse(Context ctx, String verifiableCredential) throws Exception {
        String subject = SignedJWT.parse(verifiableCredential).getJWTClaimsSet().getSubject();

        var userInfo = new UserInfo(new Subject(subject));
        userInfo.setClaim(
                "https://vocab.account.gov.uk/v1/credentialJWT", List.of(verifiableCredential));

        ctx.status(HttpStatus.CREATED);
        ctx.json(userInfo.toJSONObject());
    }
}
