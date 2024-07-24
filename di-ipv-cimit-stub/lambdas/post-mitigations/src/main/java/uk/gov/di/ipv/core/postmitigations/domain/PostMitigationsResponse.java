package uk.gov.di.ipv.core.postmitigations.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record PostMitigationsResponse(@JsonProperty("result") String result) {}
