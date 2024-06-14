package uk.gov.di.ipv.stub.core.config.uatuser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record EvidenceRequestClaims(
        @JsonProperty("scoringPolicy") String scoringPolicy,
        @JsonProperty("strengthScore") int strengthScore,
        @JsonProperty("verificationScore") int verificationScore) {

    @JsonCreator
    public EvidenceRequestClaims {}

    public String getScoringPolicy() {
        return scoringPolicy;
    }

    public int getStrengthScore() {
        return strengthScore;
    }

    public int getVerificationScore() {
        return verificationScore;
    }
}
