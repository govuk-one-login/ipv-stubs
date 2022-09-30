package uk.gov.di.ipv.stub.core.config.uatuser;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PostcodeSharedClaims(
        @JsonProperty("@context") List<String> context,
        @JsonProperty("address") List<CanonicalAddress> addresses) {}
