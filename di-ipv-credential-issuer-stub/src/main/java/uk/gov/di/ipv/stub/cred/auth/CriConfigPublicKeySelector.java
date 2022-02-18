package uk.gov.di.ipv.stub.cred.auth;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.auth.verifier.ClientCredentialsSelector;
import com.nimbusds.oauth2.sdk.auth.verifier.Context;
import com.nimbusds.oauth2.sdk.auth.verifier.InvalidClientException;
import com.nimbusds.oauth2.sdk.id.ClientID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.stub.cred.config.ClientConfig;
import uk.gov.di.ipv.stub.cred.error.ClientRegistrationException;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CriConfigPublicKeySelector implements ClientCredentialsSelector<Object> {

    public static final Logger LOGGER = LoggerFactory.getLogger(CriConfigPublicKeySelector.class);
    public static final String PUBLIC_CERTIFICATE_CONFIG_KEY = "publicCertificateToVerify";

    public Map<String, List<PublicKey>> clientPublicKeys = new HashMap<>();

    @Override
    public List<Secret> selectClientSecrets(ClientID claimedClientID, ClientAuthenticationMethod authMethod, Context context) {
        throw new UnsupportedOperationException("We don't do that round here...");
    }

    @Override
    public List<? extends PublicKey> selectPublicKeys(
            ClientID claimedClientID,
            ClientAuthenticationMethod authMethod,
            JWSHeader jwsHeader,
            boolean forceRefresh,
            Context context) throws InvalidClientException {
        List<PublicKey> publicKeys = clientPublicKeys.get(claimedClientID.getValue());
        if (publicKeys == null) {
            throw new InvalidClientException(String.format("No public keys found for clientId '%s'", claimedClientID.getValue()));
        }
        return publicKeys;
    }

    public void registerClients(Map<String, ClientConfig> clientConfigs) {
        CertificateFactory certificateFactory;
        try {
            certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new ClientRegistrationException("Unable to create certificate factory", e);
        }

        Base64.Decoder decoder = Base64.getDecoder();
        for(Map.Entry<String, ClientConfig> configEntry: clientConfigs.entrySet()) {
            try {
                PublicKey publicKey = certificateFactory.generateCertificate(
                                new ByteArrayInputStream(
                                        decoder.decode(
                                                configEntry.getValue().getJwtAuthentication().get(PUBLIC_CERTIFICATE_CONFIG_KEY))))
                        .getPublicKey();

                clientPublicKeys.put(configEntry.getKey(), List.of(publicKey));
            } catch (CertificateException | IllegalArgumentException e) {
                LOGGER.error(
                        "Failed to parse JWT authentication certificate for clientId '{}'. Continuing in degraded state. Error:'{}'",
                        configEntry.getKey(),
                        e.getMessage());
            }
        }
    }
}
