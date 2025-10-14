package uk.gov.di.ipv.stub.core.utils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;

public class JWTSigner extends ECDSASigner {

    private final String keyId;

    JWTSigner(ECKey ecKey) throws JOSEException {
        super(ecKey);
        this.keyId = ecKey.getKeyID();
    }

    String getKeyId() {
        return keyId;
    }
}
