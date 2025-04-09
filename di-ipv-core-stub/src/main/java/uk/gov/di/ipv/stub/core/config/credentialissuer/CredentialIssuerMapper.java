package uk.gov.di.ipv.stub.core.config.credentialissuer;

import java.net.URI;
import java.util.Map;

public class CredentialIssuerMapper {

    public CredentialIssuer map(Map<String, Object> map) {
        String id = (String) map.get("id");
        String name = (String) map.get("name");
        URI jwksEndpoint = URI.create((String) map.get("jwksEndpoint"));
        boolean useKeyRotation = Boolean.parseBoolean(map.get("useKeyRotation").toString());
        URI authorizeUrl = URI.create((String) map.get("authorizeUrl"));
        URI tokenUrl = URI.create((String) map.get("tokenUrl"));
        URI credentialUrl = URI.create((String) map.get("credentialUrl"));
        URI audience = URI.create((String) map.get("audience"));
        boolean sendIdentityClaims = Boolean.TRUE.equals(map.get("sendIdentityClaims"));
        String publicEncryptionJwkBase64 = (String) map.get("publicEncryptionJwkBase64");
        String publicVCSigningVerificationJwkBase64 =
                (String) map.get("publicVCSigningVerificationJwkBase64");

        String apiKeyEnvVar = (String) map.get("apiKeyEnvVar");
        return new CredentialIssuer(
                id,
                name,
                jwksEndpoint,
                useKeyRotation,
                authorizeUrl,
                tokenUrl,
                credentialUrl,
                audience,
                sendIdentityClaims,
                "ES256",
                publicEncryptionJwkBase64,
                publicVCSigningVerificationJwkBase64,
                apiKeyEnvVar);
    }
}
