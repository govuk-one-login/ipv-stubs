package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.BirthDate;

public class BirthDates {

    public final static BirthDate ALICE_DOE = createBirthDate("1970-01-01");
    public final static BirthDate ALICE_PARKER = createBirthDate("1970-01-01");
    public final static BirthDate BILLY_BATSON = createBirthDate("1981-07-26");
    public final static BirthDate BOB_PARKER = createBirthDate("1970-11-09");
    public final static BirthDate JAMES_MORIARTY = createBirthDate("1939-10-09");
    public final static BirthDate JOE_SHMOE = createBirthDate("1985-02-08");
    public final static BirthDate JOHN_ROBERTS = createBirthDate("1991-12-12");
    public final static BirthDate KENNETH_DECERQUEIRA = createBirthDate("1965-07-08");
    public final static BirthDate MARY_WATSON = createBirthDate("1932-02-25");
    public final static BirthDate SAUL_GOODMAN = createBirthDate("1995-08-16");

    // DWP users
    public final static BirthDate CLAIRE_AARTS_DWP = createBirthDate("1997-06-29");
    public final static BirthDate KABIR_SINGH_DWP = createBirthDate("1991-12-24");
    public final static BirthDate NORA_PORTER_DWP = createBirthDate("1978-01-01");
    public final static BirthDate TOM_HARDY_DWP = createBirthDate("1980-03-15");

    private BirthDates() {
        // Replace default public constructor
    }

    private static BirthDate createBirthDate(String dateValue) {
        return BirthDate.builder()
                .withValue(dateValue)
                .build();
    }
}