package uk.gov.di.ipv.core.putcontraindicators.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialSubject(List<DrivingPermit> drivingPermit, List<Passport> passport) {
    public CredentialSubject(List<DrivingPermit> drivingPermit, List<Passport> passport) {
        this.drivingPermit = drivingPermit == null ? List.of() : drivingPermit;
        this.passport = passport == null ? List.of() : passport;
    }
}
