package uk.gov.di.ipv.stub.cred.utils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ResponseMode;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import spark.QueryParamsMap;
import uk.gov.di.ipv.stub.cred.config.ClientConfig;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;

public class RequestedErrorResponseHelper {

    public static final String AUTH_PREFIX = "auth_";

    public AuthorizationErrorResponse getRequestedAuthErrorResponse(QueryParamsMap queryParamsMap)
            throws NoSuchAlgorithmException, InvalidKeySpecException, ParseException {
        String requestedOauthErrorResponse =
                queryParamsMap.value(RequestParamConstants.REQUESTED_OAUTH_ERROR_RESPONSE);
        String requestedOauthErrorDescription =
                queryParamsMap.value(RequestParamConstants.REQUESTED_OAUTH_ERROR_DESCRIPTION);

        if (requestedOauthErrorResponse != null
                && requestedOauthErrorResponse.startsWith(AUTH_PREFIX)) {
            String clientIdValue = queryParamsMap.value(RequestParamConstants.CLIENT_ID);
            ClientConfig clientConfig = CredentialIssuerConfig.getClientConfig(clientIdValue);

            String redirectUri =
                    getSignedJWT(
                                    queryParamsMap.value(RequestParamConstants.REQUEST),
                                    clientConfig.getEncryptionPrivateKey())
                            .getJWTClaimsSet()
                            .getClaim(RequestParamConstants.REDIRECT_URI)
                            .toString();

            String state = queryParamsMap.value(RequestParamConstants.STATE);

            return new AuthorizationErrorResponse(
                    URI.create(redirectUri),
                    new ErrorObject(
                            requestedOauthErrorResponse.substring(AUTH_PREFIX.length()),
                            requestedOauthErrorDescription),
                    (state == null || state.isEmpty()) ? null : new State(state),
                    new Issuer(CredentialIssuerConfig.NAME),
                    ResponseMode.QUERY);
        }
        return null;
    }

    private SignedJWT getSignedJWT(String request, PrivateKey encryptionPrivateKey)
            throws ParseException {
        try {
            JWEObject jweObject = getJweObject(request, encryptionPrivateKey);
            return jweObject.getPayload().toSignedJWT();
        } catch (ParseException
                | NoSuchAlgorithmException
                | InvalidKeySpecException
                | JOSEException e) {
            return SignedJWT.parse(request);
        }
    }

    private JWEObject getJweObject(String requestParam, PrivateKey encryptionPrivateKey)
            throws ParseException, NoSuchAlgorithmException, InvalidKeySpecException,
                    JOSEException {
        JWEObject encryptedJweObject = JWEObject.parse(requestParam);
        RSADecrypter rsaDecrypter = new RSADecrypter(encryptionPrivateKey);
        encryptedJweObject.decrypt(rsaDecrypter);
        return encryptedJweObject;
    }
}
