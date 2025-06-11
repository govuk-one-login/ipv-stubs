package uk.gov.di.ipv.core.putcontraindicators.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class PutContraIndicatorsRequestBody {
    @JsonProperty("signed_jwt")
    private String signedJwt;
}
