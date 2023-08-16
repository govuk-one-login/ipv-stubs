package uk.gov.di.ipv.core.putcontraindicators.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContraIndicatorDto {
    private String code;
    private String issuanceDate;
    private String document;
    private List<String> txn;
    private List<MitigationDto> mitigation;
    private List<MitigationDto> incompleteMitigation;
}
