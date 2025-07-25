package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.DrivingPermitDetails;

public class DrivingLicences {

    // DVLA licences
    public static final DrivingPermitDetails ALICE_PARKER_DVLA =
            createDrivingPermit("PARKE710112PBFGA", "2032-02-02", "2005-02-02", "DVLA", "23", null);
    public static final DrivingPermitDetails JOE_SHMOE_DVLA =
            createDrivingPermit(
                    "DOE99802085J99FG",
                    "2023-01-18",
                    "2010-01-18",
                    "DVLA",
                    "5",
                    "122 BURNS CRESCENT EDINBURGH EH1 9GP");
    public static final DrivingPermitDetails KENNETH_DECERQUEIRA_DVLA =
            createDrivingPermit(
                    "DECER607085K99AE",
                    "2025-04-27",
                    "2023-08-22",
                    "DVLA",
                    "16",
                    "8 HADLEY ROAD BATH BA2 5AA");
    public static final DrivingPermitDetails KENNETH_DECERQUEIRA_DVLA_2 =
            createDrivingPermit(
                    "DECER607085K99AE",
                    "2035-05-01",
                    "2025-05-02",
                    "DVLA",
                    "17",
                    "8 HADLEY ROAD BATH BA2 5AA");
    public static final DrivingPermitDetails KENNETH_DECERQUEIRA_DVLA_INVALID =
            createDrivingPermit(
                    "", "2025-04-27", "2023-08-22", "DVLA", "11", "8 HADLEY ROAD BATH BR2 5LP");

    // DVA licences
    public static final DrivingPermitDetails BILLY_BATSON_DVA =
            createDrivingPermit(
                    "55667788",
                    "2042-10-01",
                    "2018-04-19",
                    "DVA",
                    null,
                    "8 HADLEY ROAD BATH NW3 5RG");
    public static final DrivingPermitDetails BOB_PARKER_DVA =
            createDrivingPermit("55667789", "2032-02-02", "2005-02-02", "DVA", null, null);
    public static final DrivingPermitDetails JOHN_ROBERTS_DVA_INVALID =
            createDrivingPermit("12345678", "2030-11-12", "2020-12-12", "DVA", null, "BT205NE");
    public static final DrivingPermitDetails KENNETH_DECERQUEIRA_DVA =
            createDrivingPermit(
                    "12345678",
                    "2042-10-01",
                    "2018-04-19",
                    "DVA",
                    null,
                    "8 HADLEY ROAD BATH BA2 5AA");

    // DVLA driving licences for DWP users
    public static final DrivingPermitDetails CLAIRE_AARTS_DVLA =
            createDrivingPermit("AARTS710112PBFGA", "2032-02-02", "2015-02-02", "DVLA", null, null);
    public static final DrivingPermitDetails KABIR_SINGH_DVLA =
            createDrivingPermit("SINGH710112PBFGA", "2032-02-02", "2015-02-02", "DVLA", null, null);
    public static final DrivingPermitDetails NORA_PORTER_DVLA =
            createDrivingPermit("PORTE710112PBFGA", "2032-02-02", "2015-02-02", "DVLA", null, null);
    public static final DrivingPermitDetails TOM_HARDY_DVLA =
            createDrivingPermit("HARDY710112PBFGA", "2032-02-02", "2015-02-02", "DVLA", null, null);

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
