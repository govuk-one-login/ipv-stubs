package uk.gov.di.ipv.stub.core.config.uatuser;

public record UKAddress(
        String buildingNumber,
        String buildingName,
        String street,
        String county,
        String townCity,
        String postCode,
        boolean currentAddress) {}
