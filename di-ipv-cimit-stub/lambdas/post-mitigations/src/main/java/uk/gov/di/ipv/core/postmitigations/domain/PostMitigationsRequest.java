package uk.gov.di.ipv.core.postmitigations.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PostMitigationsRequest {

    @JsonProperty("govuk_signin_journey_id")
    private String govukSigninJourneyId;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("signed_jwts")
    private List<String> signedJwtVCs;
}
