package uk.gov.di.ipv.stub.core.config.credentialissuer;

import java.net.URI;

public record CredentialIssuer(
        String id,
        String name,
        URI jwksEndpoint,
        boolean useKeyRotation,
        URI authorizeUrl,
        URI tokenUrl,
        URI credentialUrl,
        URI audience,
        boolean sendIdentityClaims,
        String expectedAlgo,
        String publicEncryptionJwkBase64,
        String publicVCSigningVerificationJwkBase64,
        String apiKeyEnvVar) {

    public boolean isKbvCri() {
        return this.id.contains("kbv") && !this.id.contains("hmrc");
    }

    public boolean isAddressCri() {
        return this.id.contains("address");
    }

    public boolean isCheckHmrcCri() {
        return this.id.contains("check-hmrc");
    }
}
