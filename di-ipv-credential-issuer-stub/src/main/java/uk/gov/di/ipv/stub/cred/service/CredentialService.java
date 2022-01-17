package uk.gov.di.ipv.stub.cred.service;

import java.util.HashMap;
import java.util.Map;

public class CredentialService {

    Map<String, Map<String, Object>> credentials = new HashMap<>();

    public Map<String, Object> getCredential(String resourceId) {
        return credentials.get(resourceId);
    }

    public void persist(Map<String, Object> jsonPayload, String resourceId) {
        credentials.put(resourceId, jsonPayload);
    }
}
