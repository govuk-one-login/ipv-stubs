package uk.gov.di.ipv.stub.core.config.uatuser;

import java.util.List;

public record Identity(
        int rowNumber,
        String accountNumber,
        String ctdbDatabase,
        List<UKAddress> addresses,
        FindDateOfBirth findDateOfBirth,
        FullName name,
        Questions questions,
        String nino) {

    public Identity withNino(String nino) {
        return new Identity(
                rowNumber,
                accountNumber,
                ctdbDatabase,
                addresses,
                findDateOfBirth,
                name,
                questions,
                nino);
    }

    public Identity withAddressCountry(String addressCountry) {
        List<UKAddress> addresses = this.addresses();
        UKAddress address = addresses.get(0);

        return new Identity(
                rowNumber,
                accountNumber,
                ctdbDatabase,
                List.of(
                        new UKAddress(
                                address.buildingNumber(),
                                address.buildingName(),
                                address.street(),
                                address.county(),
                                address.townCity(),
                                address.postCode(),
                                address.validFrom(),
                                address.validUntil(),
                                addressCountry)),
                findDateOfBirth,
                name,
                questions,
                nino);
    }
}
