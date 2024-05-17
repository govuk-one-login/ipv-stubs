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
}
