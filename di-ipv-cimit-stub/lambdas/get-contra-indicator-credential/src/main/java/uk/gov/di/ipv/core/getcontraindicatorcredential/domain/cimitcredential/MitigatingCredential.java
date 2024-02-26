package uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential;

import lombok.Builder;

@Builder
public record MitigatingCredential(String issuer, String validFrom, String txn, String id) {
    public static final MitigatingCredential EMPTY = new MitigatingCredential("", "", "", "");
}
