package uk.gov.di.ipv.stub.core.config.uatuser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvidenceRequestClaims {
    @JsonProperty("scoringPolicy")
    private String scoringPolicy;

    @JsonProperty("strengthScore")
    private Integer strengthScore;

    @JsonProperty("verificationScore")
    private Integer verificationScore;

    public EvidenceRequestClaims() {}

    public EvidenceRequestClaims(
            String scoringPolicy, Integer strengthScore, Integer verificationScore) {
        this.scoringPolicy = scoringPolicy;
        this.strengthScore = strengthScore;
        this.verificationScore = verificationScore;
    }

    @JsonProperty("scoringPolicy")
    public String getScoringPolicy() {
        return scoringPolicy;
    }

    @JsonProperty("strengthScore")
    public Integer getStrengthScore() {
        return strengthScore;
    }

    @JsonProperty("verificationScore")
    public Integer getVerificationScore() {
        return verificationScore;
    }
}
