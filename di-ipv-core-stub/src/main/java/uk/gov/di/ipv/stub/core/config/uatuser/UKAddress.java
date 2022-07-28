package uk.gov.di.ipv.stub.core.config.uatuser;

import java.time.LocalDate;

public record UKAddress(
        String buildingNumber,
        String buildingName,
        String street,
        String county,
        String townCity,
        String postCode,
        LocalDate validFrom,
        LocalDate validUntil) {}
