package uk.gov.di.ipv.stub.core.config.uatuser;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public record FindDateOfBirth(Instant dateOfBirth, Instant dateOfEntryOnCtdb) {

    public Date getDOB() {
        long diff = ChronoUnit.SECONDS.between(dateOfEntryOnCtdb, Instant.now());
        Instant aged = dateOfBirth.plus(diff, ChronoUnit.SECONDS);
        return Date.from(aged);
    }
}
