package uk.gov.di.ipv.core.putcontraindicators.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContraIndicatorEvidenceDto {
    private String type;
    private List<ContraIndicatorDto> ci;
    private List<String> txn;
}
