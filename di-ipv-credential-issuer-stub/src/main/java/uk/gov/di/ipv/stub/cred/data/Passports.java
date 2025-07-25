package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.PassportDetails;

public class Passports {

    // Alice Doe (Valid) NFC passport
    public static final PassportDetails ALICE_DOE_PASSPORT =
            createPassport("123456789", "2022-02-02", "GBR");

    // Alice Parker (Valid) passport
    public static final PassportDetails ALICE_PARKER_PASSPORT =
            createPassport("44442444", "2030-01-01", "GBR");

    // James Moriarty (Invalid) passport
    public static final PassportDetails JAMES_MORIARTY_PASSPORT =
            createPassport("532114382", "2030-01-01", "GBR");

    // DWP passports
    public static final PassportDetails CLAIRE_AARTS_PASSPORT =
            createPassport("824159125", "2030-01-01", "GBR");
    public static final PassportDetails KABIR_SINGH_PASSPORT =
            createPassport("824159123", "2030-01-01", "GBR");
    public static final PassportDetails NORA_PORTER_PASSPORT =
            createPassport("824159124", "2030-01-01", "GBR");
    public static final PassportDetails TOM_HARDY_PASSPORT =
            createPassport("824159123", "2030-01-01", "GBR");

    // Kenneth Decerqueira passports
    public static final PassportDetails KENNETH_DECERQUEIRA_PASSPORT =
            createPassport("321654987", "2030-01-01", "GBR");
    public static final PassportDetails KENNETH_DECERQUEIRA_INTERNATIONAL_PASSPORT =
            createPassport("321654987", "2030-01-01", "UTO");

    // Mary Watson passports
    public static final PassportDetails MARY_WATSON_PASSPORT =
            createPassport("824159122", "2030-01-01", "GBR");
    public static final PassportDetails MARY_WATSON_F2F_PASSPORT =
            createPassport("824159121", "2030-01-01", "GBR");

    private Passports() {
        // Replace default public constructor
    }

    private static PassportDetails createPassport(
            String documentNumber, String expiryDate, String icaoIssuerCode) {
        return PassportDetails.builder()
                .withDocumentNumber(documentNumber)
                .withExpiryDate(expiryDate)
                .withIcaoIssuerCode(icaoIssuerCode)
                .build();
    }
}
