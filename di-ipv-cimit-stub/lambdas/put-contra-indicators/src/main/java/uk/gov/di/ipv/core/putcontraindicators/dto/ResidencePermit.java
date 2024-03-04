package uk.gov.di.ipv.core.putcontraindicators.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ResidencePermit(
        String documentType, String documentNumber, String icaoIssuerCode, String expiryDate)
        implements Document {

    private static final String RESIDENCE_PERMIT_IDENTIFIER_TEMPLATE = "residencePermit/%s/%s";

    @Override
    public String toIdentifier() {
        return String.format(RESIDENCE_PERMIT_IDENTIFIER_TEMPLATE, icaoIssuerCode, documentNumber);
    }
}
