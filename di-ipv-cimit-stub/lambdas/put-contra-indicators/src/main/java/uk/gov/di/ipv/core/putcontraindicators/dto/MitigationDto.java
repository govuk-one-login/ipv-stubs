package uk.gov.di.ipv.core.putcontraindicators.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MitigationDto {
    private String code;
    private List<MitigationCredentialDto> mitigatingCredential;
}
