package uk.gov.di.ipv.core.getcontraindicatorcredential.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class GetCiCredentialRequest {

    @JsonProperty("govuk_signin_journey_id")
    private String govukSigninJourneyId;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("user_id")
    private String userId;
}
