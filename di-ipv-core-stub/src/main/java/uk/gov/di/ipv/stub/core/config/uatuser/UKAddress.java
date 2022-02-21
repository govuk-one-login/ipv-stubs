package uk.gov.di.ipv.stub.core.config.uatuser;

public record UKAddress(
        String street1,
        String street2,
        String county,
        String townCity,
        String postCode,
        boolean currentAddress) {}
