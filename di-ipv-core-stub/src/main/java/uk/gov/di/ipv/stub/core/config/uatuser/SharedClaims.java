package uk.gov.di.ipv.stub.core.config.uatuser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SharedClaims(
        @JsonProperty("@context") List<String> context,
        @JsonProperty("name") List<Name> name,
        @JsonProperty("birthDate") List<DateOfBirth> birthDate,
        @JsonProperty("address") List<CanonicalAddress> addresses,
        @JsonProperty("socialSecurityRecord") List<SocialSecurityRecord> socialSecurityRecord) {}
