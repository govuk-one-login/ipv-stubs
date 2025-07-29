package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.PostalAddress;

public class Addresses {

    public static final PostalAddress ALICE_DOE_ADDRESS =
            createAddress(
                    "221C",
                    null,
                    "BAKER STREET",
                    "NW1 6XE",
                    "LONDON",
                    null,
                    null,
                    "GB",
                    "1980-01-02",
                    null);
    public static final PostalAddress ALICE_PARKER_ADDRESS =
            createAddress(
                    "80T",
                    null,
                    "YEOMAN WAY",
                    "BA14 0QP",
                    "TROWBRIDGE",
                    null,
                    null,
                    "GB",
                    "1952-01-01",
                    null);
    public static final PostalAddress ALICE_PARKER_NEW_ADDRESS =
            createAddress(
                    "27",
                    null,
                    "FOREST WAY",
                    "SM2 5TH",
                    "SUTTON",
                    null,
                    null,
                    "GB",
                    "2024-01-01",
                    null);
    public static final PostalAddress ALICE_PARKER_OLD_ADDRESS =
            createAddress(
                    "80T",
                    null,
                    "YEOMAN WAY",
                    "BA14 0QP",
                    "TROWBRIDGE",
                    null,
                    null,
                    "GB",
                    null,
                    "2024-01-01");
    public static final PostalAddress BOB_PARKER_ADDRESS =
            createAddress(
                    "80T",
                    null,
                    "YEOMAN WAY",
                    "BA14 0QP",
                    "TROWBRIDGE",
                    null,
                    null,
                    "GB",
                    "1951-01-01",
                    null);
    public static final PostalAddress JAMES_MORIARTY_INVALID_ADDRESS =
            createAddress(
                    "111B",
                    null,
                    "BAKER STREET",
                    "NW1 1XE",
                    "LONDON",
                    null,
                    null,
                    "GB",
                    "1887-01-01",
                    null);
    public static final PostalAddress JOE_SCHMOE_ADDRESS =
            createAddress(
                    "122",
                    null,
                    "BURNS CRESCENT",
                    "EH1 9GP",
                    "EDINBURGH",
                    null,
                    null,
                    "GB",
                    "1995-01-02",
                    null);
    public static final PostalAddress KENNETH_DECERQUERIA_ADDRESS =
            createAddress(
                    "",
                    "8",
                    "HADLEY ROAD",
                    "BA2 5AA",
                    "BATH",
                    null,
                    null,
                    "GB",
                    "2000-01-01",
                    null);
    public static final PostalAddress KENNETH_DECERQUERIA_JAPAN =
            createAddress("2000", "", "00", "0", "0", "0", "", "JP", "2017-01-01", null);
    public static final PostalAddress KENNETH_DECERQUERIA_NEW_ADDRESS =
            createAddress(
                    "",
                    "18",
                    "FOREST ROAD",
                    "KT4 8ND",
                    "LONDON",
                    null,
                    null,
                    "GB",
                    "2024-01-01",
                    null);
    public static final PostalAddress KENNETH_DECERQUERIA_OLD_ADDRESS =
            createAddress(
                    "",
                    "8",
                    "HADLEY ROAD",
                    "BA2 5AA",
                    "BATH",
                    null,
                    null,
                    "GB",
                    null,
                    "2024-01-01");
    public static final PostalAddress MARY_WATSON_ADDRESS =
            createAddress(
                    "221B",
                    null,
                    "BAKER STREET",
                    "NW1 6XE",
                    "LONDON",
                    null,
                    null,
                    "GB",
                    "1887-01-01",
                    null);
    public static final PostalAddress SAUL_GOODMAN_ADDRESS =
            createAddress(
                    "29",
                    null,
                    "CHURCH LANE",
                    "TN61 8PQ",
                    "TUNBRIDGE WELLS",
                    null,
                    null,
                    "GB",
                    "1996-01-01",
                    null);

    // DWP Addresses
    public static final PostalAddress CLAIRE_AARTS_ADDRESS =
            createAddress(
                    "1",
                    null,
                    "Hudswell Road",
                    "LS10 1AG",
                    "Leeds",
                    null,
                    null,
                    "GB",
                    "1996-01-01",
                    null);
    public static final PostalAddress KABIR_SINGH_ADDRESS =
            createAddress(
                    "1",
                    null,
                    "Hudswell Road",
                    "LS10 1AG",
                    "Leeds",
                    null,
                    null,
                    "GB",
                    "1996-01-01",
                    null);
    public static final PostalAddress NORA_PORTER_ADDRESS =
            createAddress(
                    "28",
                    null,
                    "Melmerby Close",
                    "NE3 5JA",
                    "Newcastle upon Tyne",
                    null,
                    null,
                    "GB",
                    "1996-01-01",
                    null);
    public static final PostalAddress TOM_HARDY_ADDRESS =
            createAddress(
                    "34",
                    null,
                    "Diamond Street",
                    "NE28 8RL",
                    "Wallsend",
                    null,
                    null,
                    "GB",
                    "1996-01-01",
                    null);

    private Addresses() {
        // Replace default public constructor
    }

    private static PostalAddress createAddress(
            String buildingName,
            String buildingNumber,
            String streetName,
            String postalCode,
            String addressLocality,
            String addressRegion,
            String subBuildingName,
            String addressCountry,
            String validFrom,
            String validUntil) {

        var builder =
                PostalAddress.builder()
                        .withBuildingName(buildingName)
                        .withStreetName(streetName)
                        .withPostalCode(postalCode)
                        .withAddressLocality(addressLocality)
                        .withAddressCountry(addressCountry);

        if (buildingNumber != null && !buildingNumber.isEmpty()) {
            builder.withBuildingNumber(buildingNumber);
        }

        if (addressRegion != null) {
            builder.withAddressRegion(addressRegion);
        }

        if (subBuildingName != null) {
            builder.withSubBuildingName(subBuildingName);
        }

        if (validFrom != null) {
            builder.withValidFrom(validFrom);
        }

        if (validUntil != null) {
            builder.withValidUntil(validUntil);
        }

        return builder.build();
    }
}
