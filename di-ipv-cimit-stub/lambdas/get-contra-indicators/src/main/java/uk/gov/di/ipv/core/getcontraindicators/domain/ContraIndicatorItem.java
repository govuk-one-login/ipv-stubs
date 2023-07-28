package uk.gov.di.ipv.core.getcontraindicators.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ContraIndicatorItem {
    private String userId;
    private String sortKey;
    private String iss;
    private String issuedAt;
    private String ci;
    private String ttl;
    private String documentId;
}
