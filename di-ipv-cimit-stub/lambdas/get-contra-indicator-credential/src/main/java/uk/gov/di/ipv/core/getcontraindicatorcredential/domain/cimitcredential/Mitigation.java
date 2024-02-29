package uk.gov.di.ipv.core.getcontraindicatorcredential.domain.cimitcredential;

import java.util.List;

public record Mitigation(String code, List<MitigatingCredential> mitigatingCredential) {}
