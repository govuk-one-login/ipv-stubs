package uk.gov.di.ipv.stub.core.config.credentialissuer;

import java.net.URI;
import java.util.Map;

public class CredentialIssuerMapper {

    public CredentialIssuer map(Map<String, Object> map) {
        String id = (String) map.get("id");
        String name = (String) map.get("name");
        URI authorizeUrl = URI.create((String) map.get("authorizeUrl"));
        URI tokenUrl = URI.create((String) map.get("tokenUrl"));
        URI credentialUrl = URI.create((String) map.get("credentialUrl"));
        URI audience = URI.create((String) map.get("audience"));
        boolean sendIdentityClaims = Boolean.TRUE.equals(map.get("sendIdentityClaims"));
        boolean sendOAuthJAR = Boolean.TRUE.equals(map.get("sendOAuthJAR"));
        String expectedAlgo = (String) map.get("expectedAlgo");
        String userInfoRequestMethod = (String) map.get("userInfoRequestMethod");
        return new CredentialIssuer(
                id,
                name,
                authorizeUrl,
                tokenUrl,
                credentialUrl,
                audience,
                sendIdentityClaims,
                sendOAuthJAR,
                expectedAlgo,
                userInfoRequestMethod);
    }
}
