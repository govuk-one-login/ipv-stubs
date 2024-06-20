package uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential;

import java.util.List;

public record Evidence(List<ContraIndicator> contraIndicator, String type) {
    private static final String SECURITY_CHECK = "SecurityCheck";

    public Evidence(List<ContraIndicator> contraIndicator) {
        this(contraIndicator, SECURITY_CHECK);
    }
}
