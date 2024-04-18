package uk.gov.di.ipv.stub.core.config.uatuser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record EvidenceRequestClaims(
        @JsonProperty("scoringPolicy") String scoringPolicy,
        @JsonProperty("strengthScore") int strengthScore) {

    @JsonCreator
    public EvidenceRequestClaims {}

    public String getScoringPolicy() {
        return scoringPolicy;
    }

    public int getStrengthScore() {
        return strengthScore;
    }
}
