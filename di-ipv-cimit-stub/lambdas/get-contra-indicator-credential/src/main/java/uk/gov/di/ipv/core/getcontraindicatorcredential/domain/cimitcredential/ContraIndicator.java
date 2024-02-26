package uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.SortedSet;

@Data
@NoArgsConstructor
@Builder
public final class ContraIndicator {
    private String code;
    private SortedSet<String> document;
    private String issuanceDate;
    private SortedSet<String> issuers;
    private List<Mitigation> mitigation;
    private List<Mitigation> incompleteMitigation;

    public ContraIndicator(
            String code,
            SortedSet<String> document,
            String issuanceDate,
            SortedSet<String> issuers,
            List<Mitigation> mitigation,
            List<Mitigation> incompleteMitigation) {
        this.code = code;
        this.document = document;
        this.issuanceDate = issuanceDate;
        this.issuers = issuers;
        this.mitigation = mitigation;
        this.incompleteMitigation = incompleteMitigation;
    }
}
