package uk.gov.di.ipv.stub.cred.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.auth.verifier.ClientCredentialsSelector;
import com.nimbusds.oauth2.sdk.auth.verifier.Context;
import com.nimbusds.oauth2.sdk.auth.verifier.InvalidClientException;
import com.nimbusds.oauth2.sdk.id.ClientID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.stub.cred.config.ClientConfig;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CriConfigPublicKeySelector implements ClientCredentialsSelector<Object> {

    public static final Logger LOGGER = LoggerFactory.getLogger(CriConfigPublicKeySelector.class);

    public Map<String, List<PublicKey>> clientPublicKeys = new HashMap<>();

    @Override
    public List<Secret> selectClientSecrets(
            ClientID claimedClientID, ClientAuthenticationMethod authMethod, Context context) {
        throw new UnsupportedOperationException("We don't do that round here...");
    }

    @Override
    public List<? extends PublicKey> selectPublicKeys(
            ClientID claimedClientID,
            ClientAuthenticationMethod authMethod,
            JWSHeader jwsHeader,
            boolean forceRefresh,
            Context context)
            throws InvalidClientException {
        List<PublicKey> publicKeys = clientPublicKeys.get(claimedClientID.getValue());
        if (publicKeys == null) {
            throw new InvalidClientException(
                    String.format(
                            "No public keys found for clientId '%s'", claimedClientID.getValue()));
        }
        return publicKeys;
    }

    public void registerClients(Map<String, ClientConfig> clientConfigs) {

        for (Map.Entry<String, ClientConfig> configEntry : clientConfigs.entrySet()) {
            Map<String, String> jwtAuthentication = configEntry.getValue().getJwtAuthentication();
            String authenticationMethod =
                    jwtAuthentication.getOrDefault("authenticationMethod", "none");
            LOGGER.info(
                    String.format(
                            "Using %s auth method for client id %s",
                            authenticationMethod, configEntry.getKey()));

            try {
                if (authenticationMethod.equals("jwt")) {
                    ECPublicKey publicKey =
                            ECKey.parse(
                                            configEntry
                                                    .getValue()
                                                    .getJwtAuthentication()
                                                    .get("signingPublicJwk"))
                                    .toECPublicKey();

                    clientPublicKeys.put(configEntry.getKey(), List.of(publicKey));
                }
            } catch (IllegalArgumentException | ParseException | JOSEException e) {
                LOGGER.error(
                        "Failed to parse signing public JWK for clientId '{}'. Continuing in degraded state. Error:'{}'",
                        configEntry.getKey(),
                        e.getMessage());
            }
        }
    }
}
