package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.IdCardDetails;

public class IdCards {

    public final static IdCardDetails EEA_VALID = createIdCard(
            "NLD", "SPEC12031", "2021-08-02", "2031-08-02");

    private IdCards() {
        // Replace default public constructor
    }

    private static IdCardDetails createIdCard(
            String icaoIssuerCode,
            String documentNumber,
            String issueDate,
            String expiryDate) {
        
        return IdCardDetails.builder()
                .withIcaoIssuerCode(icaoIssuerCode)
                .withDocumentNumber(documentNumber)
                .withIssueDate(issueDate)
                .withExpiryDate(expiryDate)
                .build();
    }
}