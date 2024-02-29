package uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential;

import java.util.List;

public record VcClaim(List<Evidence> evidence, List<String> type) {
    public VcClaim(List<Evidence> evidence) {
        this(evidence, List.of("VerifiableCredential", "SecurityCheckCredential"));
    }
}
