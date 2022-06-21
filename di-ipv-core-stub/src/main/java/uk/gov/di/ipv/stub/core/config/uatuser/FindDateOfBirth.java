package uk.gov.di.ipv.stub.core.config.uatuser;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

public record FindDateOfBirth(Instant dateOfBirth, Instant dateOfEntryOnCtdb) {

    public Date getDOB() {
        long diff = ChronoUnit.SECONDS.between(dateOfEntryOnCtdb, Instant.now());
        Instant aged = dateOfBirth.plus(diff, ChronoUnit.SECONDS);
        return Date.from(aged);
    }

    public int year() {
        return getCalendar().get(Calendar.YEAR);
    }

    public int month() {
        return getCalendar().get(Calendar.MONTH) + 1;
    }

    public int day() {
        return getCalendar().get(Calendar.DAY_OF_MONTH);
    }

    private Calendar getCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(getDOB());
        return calendar;
    }
}
