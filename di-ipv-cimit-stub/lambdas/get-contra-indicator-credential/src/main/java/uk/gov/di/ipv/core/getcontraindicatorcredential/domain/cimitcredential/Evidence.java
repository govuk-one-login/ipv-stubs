package uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential;

import java.util.List;
import java.util.UUID;

public record Evidence(List<ContraIndicator> contraIndicator, List<String> txn) {
    public Evidence(List<ContraIndicator> contraIndicator) {
        this(contraIndicator, List.of(UUID.randomUUID().toString()));
    }

    private static final String type = "SecurityCheck";
}
