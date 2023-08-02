package uk.gov.di.ipv.core.putcontraindicators.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PutContraIndicatorsResponse {
    private String result;
}
