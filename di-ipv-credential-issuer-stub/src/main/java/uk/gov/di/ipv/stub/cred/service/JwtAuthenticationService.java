package uk.gov.di.ipv.stub.cred.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.auth.JWTAuthenticationClaimsSet;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.id.Audience;
import spark.QueryParamsMap;
import uk.gov.di.ipv.stub.cred.config.ClientConfig;
import uk.gov.di.ipv.stub.cred.config.CredentialIssuerConfig;
import uk.gov.di.ipv.stub.cred.error.ClientAuthenticationException;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JwtAuthenticationService {

    public static final String AUTHENTICATION_METHOD = "authenticationMethod";
    public static final String ISSUER = "issuer";
    public static final String NONE = "none";
    public static final String PUBLIC_CERT_TO_VERIFY = "publicCertificateToVerify";
    public static final String SUBJECT = "subject";

    public void authenticateClient(QueryParamsMap requestParams)
            throws ClientAuthenticationException {

        PrivateKeyJWT authenticationJwt;
        try {
            authenticationJwt = PrivateKeyJWT.parse(listifyParamValues(requestParams));
        } catch (ParseException e) {
            throw new ClientAuthenticationException(e);
        }

        ClientConfig clientConfig = CredentialIssuerConfig.getClientConfig(authenticationJwt.getClientID().getValue());
        if (clientConfig == null) {
            throw new ClientAuthenticationException(String.format("Config for client ID '%s' not found", authenticationJwt.getClientID().getValue()));
        }

        if (clientConfig.getJwtAuthentication().get(AUTHENTICATION_METHOD).equals(NONE)) {
            return;
        }

        validateSignature(authenticationJwt.getClientAssertion(), clientConfig.getJwtAuthentication().get(PUBLIC_CERT_TO_VERIFY));
        validateClaimsSet(authenticationJwt.getJWTAuthenticationClaimsSet(), clientConfig.getJwtAuthentication());
    }

    private Map<String, List<String>> listifyParamValues(QueryParamsMap requestParams) {
        Map<String, List<String>> listifiedParams = new HashMap<>();
        requestParams.toMap()
                .forEach((key, value) -> listifiedParams.put(key, Arrays.asList(value)));
        return listifiedParams;
    }

    private void validateSignature(SignedJWT clientAssertion, String certificate) throws ClientAuthenticationException {
        try {
            RSAPublicKey publicKey = (RSAPublicKey) CertificateFactory
                    .getInstance("X.509")
                    .generateCertificate(
                            new ByteArrayInputStream(
                                    Base64.getDecoder().decode(certificate)))
                    .getPublicKey();

            if (!clientAssertion.verify(new RSASSAVerifier(publicKey))) {
                throw new ClientAuthenticationException("Failed to verify client authentication JWT signature");
            }
        } catch (CertificateException | JOSEException e) {
            throw new ClientAuthenticationException(e);
        }
    }

    private void validateClaimsSet(JWTAuthenticationClaimsSet jwtAuthenticationClaimsSet, Map<String, String> clientAuthenticationConfig)
            throws ClientAuthenticationException {
        validateStringEquals(clientAuthenticationConfig.get(ISSUER), jwtAuthenticationClaimsSet.getIssuer().getValue(), ISSUER);
        validateStringEquals(clientAuthenticationConfig.get(SUBJECT), jwtAuthenticationClaimsSet.getSubject().getValue(), SUBJECT);
        validateAudience(jwtAuthenticationClaimsSet.getAudience());
        validateNotExpired(jwtAuthenticationClaimsSet.getExpirationTime());
    }

    private void validateStringEquals(String expected, String received, String claimType) throws ClientAuthenticationException {
        if (!expected.equals(received)) {
            throw new ClientAuthenticationException(
                    String.format(
                            "Client auth claims set failed validation for '%s'. Expected: '%s'. Received: '%s'",
                            claimType,
                            expected,
                            received));
        }
    }

    private void validateAudience(List<Audience> received) throws ClientAuthenticationException {
        if (received.stream().map(Audience::getValue).noneMatch((audience) -> audience.equals(CredentialIssuerConfig.CLIENT_AUDIENCE))) {
            throw new ClientAuthenticationException(
                    String.format(
                            "Invalid audience in claims set. Expected: '%s'. Received '%s'",
                            CredentialIssuerConfig.CLIENT_AUDIENCE,
                            received));
        };
    }

    private void validateNotExpired(Date expirationTime) throws ClientAuthenticationException {
        if (expirationTime.before(new Date())) {
            throw new ClientAuthenticationException("Expiration date in client auth claims set has passed");
        }
    }
}

