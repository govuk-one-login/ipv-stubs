package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.BirthDate;

public class BirthDates {

    public static final BirthDate ALICE_DOE = createBirthDate("1970-01-01");
    public static final BirthDate ALICE_PARKER = createBirthDate("1970-01-01");
    public static final BirthDate BILLY_BATSON = createBirthDate("1981-07-26");
    public static final BirthDate BOB_PARKER = createBirthDate("1970-11-09");
    public static final BirthDate JAMES_MORIARTY = createBirthDate("1939-10-09");
    public static final BirthDate JOE_SHMOE = createBirthDate("1985-02-08");
    public static final BirthDate JOHN_ROBERTS = createBirthDate("1991-12-12");
    public static final BirthDate KENNETH_DECERQUEIRA = createBirthDate("1965-07-08");
    public static final BirthDate MARY_WATSON = createBirthDate("1932-02-25");
    public static final BirthDate SAUL_GOODMAN = createBirthDate("1995-08-16");

    // DWP users
    public static final BirthDate CLAIRE_AARTS_DWP = createBirthDate("1997-06-29");
    public static final BirthDate KABIR_SINGH_DWP = createBirthDate("1991-12-24");
    public static final BirthDate NORA_PORTER_DWP = createBirthDate("1978-01-01");
    public static final BirthDate TOM_HARDY_DWP = createBirthDate("1980-03-15");

    private BirthDates() {
        // Replace default public constructor
    }

    private static BirthDate createBirthDate(String dateValue) {
        return BirthDate.builder().withValue(dateValue).build();
    }
}
