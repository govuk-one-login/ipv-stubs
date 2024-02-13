package uk.gov.di.ipv.stub.cred.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.Builder;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

@Builder
public class ClientConfig {
    private String signingPublicJwk;
    private JwtAuthenticationConfig jwtAuthentication;
    private String base64EncryptionPrivateKey;
    private String audienceForVcJwt;

    public String getSigningPublicJwk() {
        return signingPublicJwk;
    }

    public void setSigningPublicJwk(String signingPublicJwk) {
        this.signingPublicJwk = signingPublicJwk;
    }

    public JwtAuthenticationConfig getJwtAuthentication() {
        return jwtAuthentication;
    }

    public void setJwtAuthentication(JwtAuthenticationConfig jwtAuthentication) {
        this.jwtAuthentication = jwtAuthentication;
    }

    public PrivateKey getEncryptionPrivateKey()
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] binaryKey = Base64.getDecoder().decode(base64EncryptionPrivateKey);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(binaryKey);
        return factory.generatePrivate(privateKeySpec);
    }

    public void setEncryptionPrivateKey(String encryptionPrivateKey) {
        this.base64EncryptionPrivateKey = base64EncryptionPrivateKey;
    }

    public JWK getEncryptionPublicKeyJwk()
            throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        var privateKey = (RSAPrivateCrtKey) getEncryptionPrivateKey();
        var publicKeySpec =
                new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent());
        var keyFactory = KeyFactory.getInstance("RSA");
        var publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);
        return new RSAKey.Builder(publicKey).keyIDFromThumbprint().build();
    }

    public String getAudienceForVcJwt() {
        return audienceForVcJwt;
    }

    public void setAudienceForVcJwt(String audienceForVcJwt) {
        this.audienceForVcJwt = audienceForVcJwt;
    }
}
