package uk.gov.di.ipv.core.putcontraindicators.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DrivingPermit(
        String expiryDate,
        String issuedBy,
        String issueDate,
        String issueNumber,
        String personalNumber)
        implements Document {

    private static final String DRIVING_PERMIT_IDENTIFIER_TEMPLATE = "drivingPermit/GB/%s/%s/%s";

    @Override
    public String toIdentifier() {
        return String.format(
                DRIVING_PERMIT_IDENTIFIER_TEMPLATE, issuedBy, personalNumber, issueDate);
    }
}
