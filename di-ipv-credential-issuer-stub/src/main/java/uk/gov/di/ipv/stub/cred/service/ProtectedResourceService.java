package uk.gov.di.ipv.stub.cred.service;

import uk.gov.di.ipv.stub.cred.entity.ProtectedResource;

import java.util.Map;

public class ProtectedResourceService {

    public ProtectedResource getProtectedResource(String resourceId) {
        Map<String, Object> jsonAttributes = Map.of(
                "id", resourceId,
                "evidenceType", "passport",
                "evidenceID", "passport-abc-12345"
        );

        return new ProtectedResource(jsonAttributes);
    }
}
