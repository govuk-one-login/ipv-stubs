package uk.gov.di.ipv.stub.cred.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ResponseMode;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import uk.gov.di.ipv.stub.cred.config.ClientConfig;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.domain.AuthRequest;
import uk.gov.di.ipv.stub.cred.domain.RequestedError;
import uk.gov.di.ipv.stub.cred.handlers.RequestParamConstants;

import java.net.URI;
import java.security.PrivateKey;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RequestedErrorResponseService {
    private static final String AUTH = "auth";
    private static final String TOKEN = "token";
    private static final String CREDENTIAL = "credential";
    private static final String NONE = "none";

    private final Map<String, Map<String, String>> errorResponsesRequested;

    public RequestedErrorResponseService() {
        this.errorResponsesRequested = new ConcurrentHashMap<>();
    }

    public void persist(String authCode, RequestedError requestedError) {
        if (requestedError == null) {
            return;
        }
        Map<String, String> paramsValuesMap = new HashMap<>();
        paramsValuesMap.put(RequestParamConstants.REQUESTED_OAUTH_ERROR, requestedError.error());
        paramsValuesMap.put(
                RequestParamConstants.REQUESTED_OAUTH_ERROR_ENDPOINT, requestedError.endpoint());
        paramsValuesMap.put(
                RequestParamConstants.REQUESTED_OAUTH_ERROR_DESCRIPTION,
                requestedError.description());
        paramsValuesMap.put(RequestParamConstants.REQUESTED_API_ERROR, requestedError.apiError());

        errorResponsesRequested.put(authCode, paramsValuesMap);
    }

    public AuthorizationErrorResponse getRequestedAuthErrorResponse(AuthRequest authRequest)
            throws ParseException, JOSEException {
        var requestedError = authRequest.requestedError();
        if (requestedError == null) {
            return null;
        }
        if (AUTH.equals(requestedError.endpoint()) && !NONE.equals(requestedError.error())) {
            ClientConfig clientConfig = ConfigService.getClientConfig(authRequest.clientId());

            JWTClaimsSet jwtClaimsSet =
                    getSignedJWT(
                                    authRequest.request(),
                                    CredentialIssuerConfig.getPrivateEncryptionKey().toPrivateKey())
                            .getJWTClaimsSet();

            String redirectUri =
                    jwtClaimsSet.getClaim(RequestParamConstants.REDIRECT_URI).toString();
            String state = jwtClaimsSet.getClaim(RequestParamConstants.STATE).toString();

            return new AuthorizationErrorResponse(
                    URI.create(redirectUri),
                    new ErrorObject(requestedError.error(), requestedError.description()),
                    (state == null || state.isEmpty()) ? null : new State(state),
                    new Issuer(CredentialIssuerConfig.getName()),
                    ResponseMode.QUERY);
        }
        return null;
    }

    public TokenErrorResponse getRequestedAccessTokenErrorResponse(String authCode) {
        Map<String, String> requestedErrorResponse = errorResponsesRequested.get(authCode);
        if (requestedErrorResponse != null) {
            String error = requestedErrorResponse.get(RequestParamConstants.REQUESTED_API_ERROR);
            String endpoint =
                    requestedErrorResponse.get(
                            RequestParamConstants.REQUESTED_OAUTH_ERROR_ENDPOINT);

            if ((TOKEN.equals(endpoint)) && (!NONE.equals(error))) {
                return new TokenErrorResponse(
                        new ErrorObject(
                                error,
                                requestedErrorResponse.get(
                                        RequestParamConstants.REQUESTED_OAUTH_ERROR_DESCRIPTION),
                                Integer.parseInt(error)));
            }
        }
        return null;
    }

    public void persistUserInfoErrorAgainstAccessToken(String authCode, String accessToken) {
        Map<String, String> requestedErrorResponse = errorResponsesRequested.get(authCode);
        if (requestedErrorResponse != null) {
            String error = requestedErrorResponse.get(RequestParamConstants.REQUESTED_API_ERROR);
            String endpoint =
                    requestedErrorResponse.get(
                            RequestParamConstants.REQUESTED_OAUTH_ERROR_ENDPOINT);
            if (error != null && CREDENTIAL.equals(endpoint)) {
                Map<String, String> paramsValuesMap = new HashMap<>();
                paramsValuesMap.put(RequestParamConstants.REQUESTED_API_ERROR, error);
                errorResponsesRequested.put(accessToken, paramsValuesMap);
            }
        }
    }

    public UserInfoErrorResponse getUserInfoErrorByToken(String accessToken) {
        Map<String, String> requestedErrorResponse = errorResponsesRequested.get(accessToken);
        if (requestedErrorResponse != null) {
            String error = requestedErrorResponse.get(RequestParamConstants.REQUESTED_API_ERROR);
            String endpoint =
                    requestedErrorResponse.get(
                            RequestParamConstants.REQUESTED_OAUTH_ERROR_ENDPOINT);
            if (error != null && CREDENTIAL.equals(endpoint)) {
                return new UserInfoErrorResponse(
                        new ErrorObject(
                                error,
                                String.format("UserInfo endpoint %s triggered by stub", error),
                                Integer.parseInt(error)));
            }
        }
        return null;
    }

    private SignedJWT getSignedJWT(String request, PrivateKey encryptionPrivateKey)
            throws ParseException {
        try {
            JWEObject jweObject = getJweObject(request, encryptionPrivateKey);
            return jweObject.getPayload().toSignedJWT();
        } catch (ParseException | JOSEException e) {
            return SignedJWT.parse(request);
        }
    }

    private JWEObject getJweObject(String requestParam, PrivateKey encryptionPrivateKey)
            throws ParseException, JOSEException {
        JWEObject encryptedJweObject = JWEObject.parse(requestParam);
        RSADecrypter rsaDecrypter = new RSADecrypter(encryptionPrivateKey);
        encryptedJweObject.decrypt(rsaDecrypter);
        return encryptedJweObject;
    }
}
