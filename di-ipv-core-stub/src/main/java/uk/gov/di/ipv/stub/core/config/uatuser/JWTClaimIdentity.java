package uk.gov.di.ipv.stub.core.config.uatuser;

import java.util.Date;
import java.util.List;

public record JWTClaimIdentity(
        List<Name> names, List<UKAddress> UKAddresses, List<Date> datesOfBirth) {}
