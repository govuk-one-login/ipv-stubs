package uk.gov.di.ipv.stub.core.config.uatuser;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SocialSecurityRecord(@JsonProperty("personalNumber") String personalNumber) {}
