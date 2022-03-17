package uk.gov.di.ipv.stub.cred.service;

import uk.gov.di.ipv.stub.cred.domain.Credential;

import java.util.HashMap;
import java.util.Map;

public class CredentialService {

    Map<String, Credential> credentials = new HashMap<>();

    public Credential getCredential(String resourceId) {
        return credentials.get(resourceId);
    }

    public void persist(Credential credential, String resourceId) {
        credentials.put(resourceId, credential);
    }
}
