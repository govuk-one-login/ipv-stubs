package uk.gov.di.ipv.stub.core.config.uatuser;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NameParts(@JsonProperty("type") String type, @JsonProperty("value") String value) {}
