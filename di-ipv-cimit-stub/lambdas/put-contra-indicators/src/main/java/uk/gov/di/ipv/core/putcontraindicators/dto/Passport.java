package uk.gov.di.ipv.core.putcontraindicators.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Passport(String documentNumber, String expiryDate, String icaoIssuerCode)
        implements Document {

    private static final String PASSPORT_IDENTIFIER_TEMPLATE = "passport/%s/%s";

    @Override
    public String toIdentifier() {
        return String.format(PASSPORT_IDENTIFIER_TEMPLATE, icaoIssuerCode, documentNumber);
    }
}
