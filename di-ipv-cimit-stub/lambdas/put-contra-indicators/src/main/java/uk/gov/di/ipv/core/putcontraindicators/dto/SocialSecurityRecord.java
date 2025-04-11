package uk.gov.di.ipv.core.putcontraindicators.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SocialSecurityRecord(String personalNumber) implements Document {

    private static final String SOCIAL_SECURITY_IDENTIFIER_TEMPLATE = "socialSecurity/GB/%s";

    @Override
    public String toIdentifier() {
        return String.format(SOCIAL_SECURITY_IDENTIFIER_TEMPLATE, personalNumber);
    }
}
