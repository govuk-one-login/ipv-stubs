package uk.gov.di.ipv.core.getcontraindicators.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class GetCiResponse {
    private List<ContraIndicatorItem> contraIndicators;
}
