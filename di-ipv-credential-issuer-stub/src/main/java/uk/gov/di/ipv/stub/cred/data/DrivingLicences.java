package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.DrivingPermitDetails;

import java.time.LocalDate;

public class DrivingLicences {

    // DVLA Licences
    public static DrivingPermitDetails getAliceParkerDvla() {
        return createDrivingPermit(
                "PARKE710112PBFGA",
                LocalDate.now().plusYears(5).toString(),
                "2005-02-02",
                "DVLA",
                "23",
                null);
    }

    public static DrivingPermitDetails getJoeShmoeDvla() {
        return createDrivingPermit(
                "DOE99802085J99FG",
                LocalDate.now().plusYears(5).toString(),
                "2010-01-18",
                "DVLA",
                "5",
                "122 BURNS CRESCENT EDINBURGH EH1 9GP");
    }

    public static DrivingPermitDetails getKennethDecerqueiraDvla() {
        return createDrivingPermit(
                "DECER607085K99AE",
                LocalDate.now().plusYears(5).toString(),
                "2023-08-22",
                "DVLA",
                "16",
                "8 HADLEY ROAD BATH BA2 5AA");
    }

    public static DrivingPermitDetails getKennethDecerqueiraDvlaInvalid() {
        return createDrivingPermit(
                "",
                LocalDate.now().plusYears(5).toString(),
                "2023-08-22",
                "DVLA",
                "11",
                "8 HADLEY ROAD BATH BR2 5LP");
    }

    // This doesn't have a dynamic expiry date as it's used in staging tests
    // therefore needs to have the below values to be valid
    public static DrivingPermitDetails getKennethDecerqueiraDvla2() {
        return createDrivingPermit(
                "DECER607085K99AE",
                "2035-05-01",
                "2025-05-02",
                "DVLA",
                "17",
                "8 HADLEY ROAD BATH BA2 5AA");
    }

    // This doesn't have a dynamic expiry date as it's used in staging tests
    // therefore needs to have the below values to be valid
    public static DrivingPermitDetails getKennethDecerqueiraDvla2Invalid() {
        return createDrivingPermit(
                "DECER607085K99AE",
                "2025-05-03",
                "2025-05-02",
                "DVLA",
                "17",
                "8 HADLEY ROAD BATH BA2 5AA");
    }

    public static DrivingPermitDetails getClaireAartsDvla() {
        return createDrivingPermit(
                "AARTS710112PBFGA",
                LocalDate.now().plusYears(5).toString(),
                "2015-02-02",
                "DVLA",
                null,
                null);
    }

    public static DrivingPermitDetails getKabirSinghDvla() {
        return createDrivingPermit(
                "SINGH710112PBFGA",
                LocalDate.now().plusYears(5).toString(),
                "2015-02-02",
                "DVLA",
                null,
                null);
    }

    // This doesn't have a dynamic expiry date as it's used in staging tests
    // therefore needs to have the below values to be valid
    public static DrivingPermitDetails getNoraPorterDvla() {
        return createDrivingPermit(
                "PORTE710112PBFGA", "2032-02-02", "2015-02-02", "DVLA", null, null);
    }

    // This doesn't have a dynamic expiry date as it's used in staging tests
    // therefore needs to have the below values to be valid
    public static DrivingPermitDetails getTomHardyDvla() {
        return createDrivingPermit(
                "HARDY710112PBFGA", "2032-02-02", "2015-02-02", "DVLA", null, null);
    }

    // DVA Licences
    public static DrivingPermitDetails getBillyBatsonDva() {
        return createDrivingPermit(
                "55667788",
                LocalDate.now().plusYears(5).toString(),
                "2018-04-19",
                "DVA",
                null,
                "8 HADLEY ROAD BATH NW3 5RG");
    }

    public static DrivingPermitDetails getBobParkerDva() {
        return createDrivingPermit(
                "55667789",
                LocalDate.now().plusYears(5).toString(),
                "2005-02-02",
                "DVA",
                null,
                null);
    }

    public static DrivingPermitDetails getJohnRobertsDvaInvalid() {
        return createDrivingPermit(
                "12345678",
                LocalDate.now().plusYears(5).toString(),
                "2020-12-12",
                "DVA",
                null,
                "BT205NE");
    }

    public static DrivingPermitDetails getKennethDecerqueiraDva() {
        return createDrivingPermit(
                "12345678",
                LocalDate.now().plusYears(5).toString(),
                "2018-04-19",
                "DVA",
                null,
                "8 HADLEY ROAD BATH BA2 5AA");
    }

    private DrivingLicences() {
        // Replace default public constructor
    }

    private static DrivingPermitDetails createDrivingPermit(
            String personalNumber,
            String expiryDate,
            String issueDate,
            String issuedBy,
            String issueNumber,
            String fullAddress) {

        var builder =
                DrivingPermitDetails.builder()
                        .withPersonalNumber(personalNumber)
                        .withExpiryDate(expiryDate)
                        .withIssueDate(issueDate)
                        .withIssuedBy(issuedBy);

        if (issueNumber != null) {
            builder.withIssueNumber(issueNumber);
        }

        if (fullAddress != null) {
            builder.withFullAddress(fullAddress);
        }

        return builder.build();
    }
}
