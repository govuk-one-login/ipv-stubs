package uk.gov.di.ipv.stub.cred.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.auth.verifier.ClientAuthenticationVerifier;
import com.nimbusds.oauth2.sdk.auth.verifier.InvalidClientException;
import com.nimbusds.oauth2.sdk.id.Audience;
import uk.gov.di.ipv.stub.cred.config.ClientConfig;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.error.ClientAuthenticationException;

import java.util.Set;

public class ClientJwtVerifier {

    public static final String AUTHENTICATION_METHOD = "authenticationMethod";
    public static final String NONE = "none";

    private final ClientAuthenticationVerifier<Object> verifier;

    public ClientJwtVerifier() {
        this.verifier = getPopulatedClientAuthVerifier();
    }

    public void authenticateClient(String queryString) throws ClientAuthenticationException {

        PrivateKeyJWT authenticationJwt;
        try {
            authenticationJwt = PrivateKeyJWT.parse(queryString);
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
            verifier.verify(authenticationJwt, null, null);
        } catch (InvalidClientException | JOSEException e) {
            throw new ClientAuthenticationException(e);
        }
    }

    private ClientAuthenticationVerifier<Object> getPopulatedClientAuthVerifier() {
        CriConfigPublicKeySelector criConfigPublicKeySelector = new CriConfigPublicKeySelector();
        criConfigPublicKeySelector.registerClients(CredentialIssuerConfig.getClientConfigs());
        return new ClientAuthenticationVerifier<>(
                criConfigPublicKeySelector,
                Set.of(new Audience(CredentialIssuerConfig.CLIENT_AUDIENCE)));
    }
}
