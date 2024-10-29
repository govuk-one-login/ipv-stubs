package uk.gov.di.ipv.stub.cred.config;

import com.nimbusds.jose.jwk.RSAKey;
import uk.gov.di.ipv.stub.cred.vc.EncryptionAlgorithm;

import java.text.ParseException;

public class CredentialIssuerConfig {
    public static final String PORT = getConfigValue("CREDENTIAL_ISSUER_PORT", "8084");
    public static final String NAME =
            getConfigValue("CREDENTIAL_ISSUER_NAME", "Credential Issuer Stub");

    public static final String CLIENT_AUDIENCE = getConfigValue("CLIENT_AUDIENCE");
    public static final String DEV_DOMAIN =
            getConfigValue("DEV_DOMAIN", ".dev.identity.account.gov.uk");
    public static final String F2F_STUB_QUEUE_NAME_DEFAULT = getConfigValue("F2F_STUB_QUEUE_NAME");

    public static final String EVIDENCE_TYPE_PARAM = "type";
    public static final String EVIDENCE_TYPE_IDENTITY_CHECK = "IdentityCheck";
    public static final String EVIDENCE_TXN_PARAM = "txn";
    public static final String CHECK_DETAILS_PARAM = "checkDetails";
    public static final String FAILED_CHECK_DETAILS_PARAM = "failedCheckDetails";
    public static final String CRI_MITIGATION_ENABLED = "MITIGATION_ENABLED";
    private static final String CREDENTIAL_ISSUER_TYPE_VAR = "CREDENTIAL_ISSUER_TYPE";

    private CredentialIssuerConfig() {}

    public static CriType getCriType() {
        return CriType.fromValue(
                getConfigValue(CREDENTIAL_ISSUER_TYPE_VAR, CriType.EVIDENCE_CRI_TYPE.value));
    }

    public static String getVerifiableCredentialIssuer() {
        return getConfigValue("VC_ISSUER", null);
    }

    public static String getVerifiableCredentialSigningKey() {
        return getConfigValue("VC_SIGNING_KEY", null);
    }

    public static EncryptionAlgorithm getVerifiableCredentialSigningAlgorithm() {
        var signingAlgorithm = getConfigValue("VC_SIGNING_ALGORITHM", null);
        if (signingAlgorithm == null) {
            return EncryptionAlgorithm.EC;
        }

        return EncryptionAlgorithm.valueOf(signingAlgorithm);
    }

    public static RSAKey getPrivateEncryptionKey() throws ParseException {
        return RSAKey.parse(getConfigValue("PRIVATE_ENCRYPTION_KEY_JWK"));
    }

    public static String getConfigValue(String key) {
        return getConfigValue(key, null);
    }

    private static String getConfigValue(String key, String defaultValue) {
        var envValue = System.getenv(key);
        if (envValue == null) {
            return defaultValue;
        }

        return envValue;
    }

    public static boolean isEnabled(String key, String defaultValue) {
        return Boolean.parseBoolean(getConfigValue(key, defaultValue));
    }
}
