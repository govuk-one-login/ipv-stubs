package uk.gov.di.ipv.core.putcontraindicators.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialSubject(
        List<DrivingPermit> drivingPermit,
        List<Passport> passport,
        List<ResidencePermit> residencePermit,
        List<SocialSecurityRecord> socialSecurityRecord) {}
