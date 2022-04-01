package uk.gov.di.ipv.stub.core.config.uatuser;

public record Identity(
        int rowNumber,
        String accountNumber,
        String ctdbDatabase,
        UKAddress UKAddress,
        FindDateOfBirth findDateOfBirth,
        FullName name,
        Questions questions) {}
