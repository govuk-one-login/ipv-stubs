package uk.gov.di.ipv.core.putcontraindicators.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MitigationCredentialDto {
    private String issuer;
    private String validFrom;
    private String txn;
    private String id;
}
