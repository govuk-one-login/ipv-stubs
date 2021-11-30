package uk.gov.di.ipv.stub.cred.service;

import uk.gov.di.ipv.stub.cred.domain.Credential;

import java.util.Map;

public class CredentialService {

    public Credential getCredential(String resourceId) {
        Map<String, Object> jsonAttributes = Map.of(
                "id", resourceId,
                "evidenceType", "passport",
                "evidenceID", "passport-abc-12345"
        );

        return new Credential(jsonAttributes);
    }
}
