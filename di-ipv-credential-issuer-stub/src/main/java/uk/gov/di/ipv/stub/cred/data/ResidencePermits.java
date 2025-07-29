package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.ResidencePermitDetails;

public class ResidencePermits {

    public static final ResidencePermitDetails ALICE_PARKER_BRP =
            createResidencePermit("IR", "GBR", "ZR8016200", "2024-02-02");
    public static final ResidencePermitDetails ALICE_PARKER_BRC =
            createResidencePermit("CR", "GBR", "ZR8016200", "2024-02-02");
    public static final ResidencePermitDetails SAUL_GOODMAN_BRC =
            createResidencePermit("CR", "GBR", "AX66K69P2", "2030-07-13");

    private ResidencePermits() {
        // Replace default public constructor
    }

    private static ResidencePermitDetails createResidencePermit(
            String documentType, String icaoIssuerCode, String documentNumber, String expiryDate) {

        return ResidencePermitDetails.builder()
                .withDocumentType(documentType)
                .withIcaoIssuerCode(icaoIssuerCode)
                .withDocumentNumber(documentNumber)
                .withExpiryDate(expiryDate)
                .build();
    }
}
