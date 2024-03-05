package uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential;

import java.util.List;
import java.util.SortedSet;

public record Evidence(List<ContraIndicator> contraIndicator, SortedSet<String> txn, String type) {
    private static final String SECURITY_CHECK = "SecurityCheck";

    public Evidence(List<ContraIndicator> contraIndicator, SortedSet<String> txn) {
        this(contraIndicator, txn, SECURITY_CHECK);
    }
}
