package uk.gov.di.ipv.stub.core.config.uatuser;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SharedClaims(
        @JsonProperty("@context") List<String> context,
        @JsonProperty("name") List<Name> name,
        @JsonProperty("birthDate") List<DateOfBirth> birthDate,
        @JsonProperty("address") List<CanonicalAddress> addresses,
        @JsonProperty("socialSecurityRecord") List<SocialSecurityRecord> socialSecurityRecord) {}
