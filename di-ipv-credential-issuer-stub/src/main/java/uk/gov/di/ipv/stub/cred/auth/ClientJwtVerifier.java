package uk.gov.di.ipv.stub.cred.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.auth.verifier.ClientAuthenticationVerifier;
import com.nimbusds.oauth2.sdk.auth.verifier.InvalidClientException;
import com.nimbusds.oauth2.sdk.id.Audience;
import io.javalin.http.Context;
import uk.gov.di.ipv.stub.cred.config.ClientConfig;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.error.ClientAuthenticationException;
import uk.gov.di.ipv.stub.cred.service.ConfigService;
import uk.gov.di.ipv.stub.cred.utils.ES256SignatureVerifier;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClientJwtVerifier {

    public static final String NONE = "none";
    public static final String CLIENT_ASSERTION_PARAM = "client_assertion";

    private final ClientAuthenticationVerifier<Object> clientAuthVerifier;
    private final ES256SignatureVerifier es256SignatureVerifier;

    public ClientJwtVerifier() {
        this.clientAuthVerifier = getPopulatedClientAuthVerifier();
        this.es256SignatureVerifier = new ES256SignatureVerifier();
    }

    public void authenticateClient(Context ctx) throws ClientAuthenticationException {

        Map<String, List<String>> formParams = ctx.formParamMap();
        PrivateKeyJWT authenticationJwt;
        try {
            authenticationJwt = PrivateKeyJWT.parse(formParams);
        } catch (ParseException e) {
            throw new ClientAuthenticationException(e);
        }

        ClientConfig clientConfig =
                ConfigService.getClientConfig(authenticationJwt.getClientID().getValue());
        if (clientConfig == null) {
            throw new ClientAuthenticationException(
                    String.format(
                            "Config for client ID '%s' not found",
                            authenticationJwt.getClientID().getValue()));
        }

        if (clientConfig.getJwtAuthentication().getAuthenticationMethod().equals(NONE)) {
            return;
        }

        try {
            PrivateKeyJWT concatSignatureAuthJwt;
            if (es256SignatureVerifier.signatureIsDerFormat(
                    authenticationJwt.getClientAssertion())) {
                concatSignatureAuthJwt =
                        transcodeSignatureToConcatFormat(authenticationJwt, formParams);
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

    private PrivateKeyJWT transcodeSignatureToConcatFormat(
            PrivateKeyJWT authJwt, Map<String, List<String>> formParams)
            throws java.text.ParseException, JOSEException, ParseException {
        SignedJWT transcodedSignedJwt =
                es256SignatureVerifier.transcodeSignature(authJwt.getClientAssertion());
        formParams.put(CLIENT_ASSERTION_PARAM, List.of(transcodedSignedJwt.serialize()));
        return PrivateKeyJWT.parse(formParams);
    }

    private ClientAuthenticationVerifier<Object> getPopulatedClientAuthVerifier() {
        CriConfigPublicKeySelector criConfigPublicKeySelector = new CriConfigPublicKeySelector();
        criConfigPublicKeySelector.registerClients(ConfigService.getClientConfigs());
        return new ClientAuthenticationVerifier<>(
                criConfigPublicKeySelector,
                Set.of(new Audience(CredentialIssuerConfig.CLIENT_AUDIENCE)));
    }
}
