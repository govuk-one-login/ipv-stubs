package uk.gov.di.ipv.core.putcontraindicatorcredential.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PutContraIndicatorsRequest {

    @JsonProperty("govuk_signin_journey_id")
    private String govukSigninJourneyId;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty(value = "signed_jwt")
    private String signedJwt;
}
