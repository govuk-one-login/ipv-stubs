package uk.gov.di.ipv.stub.cred.service;

import java.util.HashMap;
import java.util.Map;

public class CredentialService {

    Map<String, String> credentials = new HashMap<>();

    public String getCredentialSignedJwt(String resourceId) {
        return credentials.get(resourceId);
    }

    public void persist(String signedJwt, String resourceId) {
        credentials.put(resourceId, signedJwt);
    }
}
