package uk.gov.di.ipv.core.postmitigations.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

public record PostMitigationsResponse(
        String result,
        @JsonInclude(JsonInclude.Include.NON_NULL) String reason,
        @JsonInclude(JsonInclude.Include.NON_NULL) String errorMessage) {}
