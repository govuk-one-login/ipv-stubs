package uk.gov.di.ipv.stub.cred.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.auth.verifier.ClientAuthenticationVerifier;
import com.nimbusds.oauth2.sdk.auth.verifier.InvalidClientException;
import com.nimbusds.oauth2.sdk.id.Audience;
import spark.QueryParamsMap;
import uk.gov.di.ipv.stub.cred.config.ClientConfig;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.error.ClientAuthenticationException;
import uk.gov.di.ipv.stub.cred.utils.ES256SignatureVerifier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClientJwtVerifier {

    public static final String AUTHENTICATION_METHOD = "authenticationMethod";
    public static final String NONE = "none";
    public static final String CLIENT_ASSERTION_PARAM = "client_assertion";

    private final ClientAuthenticationVerifier<Object> clientAuthVerifier;
    private final ES256SignatureVerifier es256SignatureVerifier;

    public ClientJwtVerifier() {
        this.clientAuthVerifier = getPopulatedClientAuthVerifier();
        this.es256SignatureVerifier = new ES256SignatureVerifier();
    }

    public void authenticateClient(QueryParamsMap queryParamsMap)
            throws ClientAuthenticationException {

        Map<String, List<String>> queryParams = listifyParamValues(queryParamsMap);
        PrivateKeyJWT authenticationJwt;
        try {
            authenticationJwt = PrivateKeyJWT.parse(queryParams);
        } catch (ParseException e) {
            throw new ClientAuthenticationException(e);
        }

        ClientConfig clientConfig =
                CredentialIssuerConfig.getClientConfig(authenticationJwt.getClientID().getValue());
        if (clientConfig == null) {
            throw new ClientAuthenticationException(
                    String.format(
                            "Config for client ID '%s' not found",
                            authenticationJwt.getClientID().getValue()));
        }

        if (clientConfig.getJwtAuthentication().get(AUTHENTICATION_METHOD).equals(NONE)) {
            return;
        }

        try {
            PrivateKeyJWT concatSignatureAuthJwt;
            if (es256SignatureVerifier.signatureIsDerFormat(
                    authenticationJwt.getClientAssertion())) {
                concatSignatureAuthJwt =
                        transcodeSignatureToConcatFormat(authenticationJwt, queryParams);
            } else {
                concatSignatureAuthJwt = authenticationJwt;
            }
            clientAuthVerifier.verify(concatSignatureAuthJwt, null, null);
        } catch (InvalidClientException
                | JOSEException
                | ParseException
                | java.text.ParseException e) {
            throw new ClientAuthenticationException(e);
        }
    }

    private Map<String, List<String>> listifyParamValues(QueryParamsMap requestParams) {
        Map<String, List<String>> listifiedParams = new HashMap<>();
        requestParams
                .toMap()
                .forEach((key, value) -> listifiedParams.put(key, Arrays.asList(value)));
        return listifiedParams;
    }

    private PrivateKeyJWT transcodeSignatureToConcatFormat(
            PrivateKeyJWT authJwt, Map<String, List<String>> queryParams)
            throws java.text.ParseException, JOSEException, ParseException {
        SignedJWT transcodedSignedJwt =
                es256SignatureVerifier.transcodeSignature(authJwt.getClientAssertion());
        queryParams.put(CLIENT_ASSERTION_PARAM, List.of(transcodedSignedJwt.serialize()));
        return PrivateKeyJWT.parse(queryParams);
    }

    private ClientAuthenticationVerifier<Object> getPopulatedClientAuthVerifier() {
        CriConfigPublicKeySelector criConfigPublicKeySelector = new CriConfigPublicKeySelector();
        criConfigPublicKeySelector.registerClients(CredentialIssuerConfig.getClientConfigs());
        return new ClientAuthenticationVerifier<>(
                criConfigPublicKeySelector,
                Set.of(new Audience(CredentialIssuerConfig.CLIENT_AUDIENCE)));
    }
}
